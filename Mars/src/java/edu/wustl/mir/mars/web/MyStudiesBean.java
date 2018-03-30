
package edu.wustl.mir.mars.web;

import edu.wustl.mir.mars.db.Study;
import edu.wustl.mir.mars.db.StudyUser;
import edu.wustl.mir.mars.db.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.model.SelectItem;
import org.hibernate.Session;

/**
 * Session bean for add/chg/del studies
 */
@ManagedBean
@SessionScoped
public class MyStudiesBean extends BeanBase implements Serializable {

   @SuppressWarnings("FieldNameHidesFieldInSuperclass")
   static final long serialVersionUID = 1L;

    /** Creates a new instance of StudiesBean */
    public MyStudiesBean() {
       sessionBean.newTaskBean("myStudiesBean");
       initializeChg();
    }
  

  


   /****************************************************************************
    *********************************** Change tab methods
    ***************************************************************************/

    User user;

   //----------------------------------------------------- Column sort
   private String sortColumnName, oldSortColumnName;
   private boolean ascending, oldAscending;
   public String getSortColumnName() {return sortColumnName;}
   public void setSortColumnName(String sortColumnName) {this.sortColumnName = sortColumnName;}
   public boolean isAscending() {return ascending;}
   public void setAscending(boolean ascending) {this.ascending = ascending;}

   private Study chgStudy = null;   // Selected user for changes
   public Study getChgStudy() {return chgStudy;}
   public void setChgUser(Study chgStudy) {this.chgStudy = chgStudy;}

   private Study[] allStudies;
   public Study[] getAllStudies() {
      if (!sortColumnName.equals(oldSortColumnName) ||
              ascending != oldAscending) {
         oldSortColumnName = sortColumnName;
         oldAscending = ascending;
         Comparator<Study> comp = new Study.StudyComparator(sortColumnName, ascending);
         Arrays.sort(allStudies, comp);
      }
      return allStudies;
   }
   public void setAllStudies(Study[] allStudies) {this.allStudies = allStudies;}

    /*
   * 1 = begin; No study selected
   * 2 = Study selected; No action to take selected
   * 3 = Modify selected User
   * 4 = Delete selected User
   * 5 = Reset pw for selected user
   */
  private int currentChangeStep = 1;
  private static final int NO_STUDIES = 99;
  public boolean renderSelectStudy()    { return currentChangeStep <= 2; }
  public boolean renderCommandButtons() { return currentChangeStep == 2; }
  public boolean renderMessage1()       { return currentChangeStep == 1; }
  public boolean renderMessage2()       { return currentChangeStep == 2; }
  public boolean renderModifyStudy()    { return currentChangeStep == 3; }
  public boolean renderDeleteStudy()    { return currentChangeStep == 4; }
  public boolean renderChgUsers()       { return currentChangeStep == 5; }
  public boolean noStudies() { return currentChangeStep == NO_STUDIES; }

   public final void initializeChg() {
      if (chgStudy != null) chgStudy.setSelected(false);
      try {
         getNewSession();
         user = (User) session.createQuery("from user u fetch all properties where u.dba = :dba")
              .setInteger("dba", sessionBean.getUserDba())
              .uniqueResult();
         List<Study> studies = new ArrayList<Study>();
         for (StudyUser su : user.getStudyUsers()) {
            if (su.getUserRole() == StudyUser.ADMIN_ROLE) studies.add(su.getStudy());
         }
         allStudies = studies.toArray(new Study[0]);
         if (allStudies.length > 1) {
            Comparator<Study> comp = new Study.StudyComparator("id", true);
            Arrays.sort(allStudies, comp);
         }
         sortColumnName = oldSortColumnName = "id";
         ascending = oldAscending = true;
         rollback();
         currentChangeStep = 1;
         if (allStudies.length == 0) currentChangeStep = NO_STUDIES;
      } catch (Exception e) {
         String em = "initializeChg() " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }

   public void studySelected() {
      for (Study s : allStudies) {
         if (s.isSelected()) {
            chgStudy = s;
            currentChangeStep = 2;
            return;
         }
      }
      currentChangeStep = 1;
   }

   public void clearStudySelections() {
      for (Study s : allStudies) s.setSelected(false);
   }
   
   //---------- Action to take selections on main chg screen

   public void chgStudy() {
      currentChangeStep = 3;
   }

   public void delStudy() {
      currentChangeStep = 4;
   }

   public void chgUsers() {
      currentChangeStep = 5;
      initializeMaintainUsers();
   }

   //----------------- Actions from modify study screen

   public void chgOk() {
      Valid v = new Valid();
      try {
         v.startValidations();
         v.NB("Study Id", chgStudy.getId());
         newTransaction();
         Study u = (Study) session.createQuery("from study s where s.id = :id and s.dba <> :dba").
              setString("id", chgStudy.getId()).
              setInteger("dba", chgStudy.getDba()).
              uniqueResult();
         if (u != null) v.error("Study Id", "is already in use");
         rollback();
         if (v.isErrors()) return;
         newTransaction();
         session.saveOrUpdate(chgStudy);
         commit();
         initializeChg();
      } catch (Exception e) {
         String em = "initializeChg() " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
     }
   }

   public void canChg() {
       initializeChg();
   }

   public void delOk() {
      try {
         newTransaction();
         session.delete(chgStudy);
         commit();
         initializeChg();
      } catch (Exception e) {
         String em = "initializeChg() " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }

   /****************************************************************************
    *********************************** Maintain Users
    ***************************************************************************/

   private void initializeMaintainUsers() {
      loadAvailableUsers();
      loadAssignedUsers();
   }

   private User[] availableUsers;
   private String scn, oldscn;
   public String getScn() { return scn;}
   public void setScn(String s) {scn = s; }
   private boolean asc, oldasc;
   public boolean isAsc() { return asc; }
   public void setAsc(boolean a) { asc = a; }

   SelectItem[] userRoles = {
      new SelectItem(0, "View"),
      new SelectItem(1, "Maintain"),
      new SelectItem(2, "Admin")
   };
   public SelectItem[] getUserRoles() { return userRoles; }

   public User[] getAvailableUsers() {
      if (!scn.equals(oldscn) || asc != oldasc) {
         oldscn = scn;
         oldasc = asc;
         Comparator<User> comp = new User.UserComparator(scn, asc);
         Arrays.sort(availableUsers, comp);
      }
      return availableUsers;
   }

   public void setAvailableUsers(User[] availableUsers) {
      this.availableUsers = availableUsers;
   }

   private void loadAvailableUsers() {
      List<User> available = new ArrayList<User>();
      try {
         Session s = sf.openSession();
         s.beginTransaction();
         @SuppressWarnings("unchecked")
         List<User> all = (List<User>) s.createQuery("from user u fetch all properties order by u.id").list();
         s.close();
         for (User u : all) {
            boolean include = true;
            for (StudyUser su : u.getStudyUsers()) {
               if (su.getStudy().getDba() == chgStudy.getDba()) {
                  include = false;
                  break;
               }
            }
            if (include) {
               u.setSelected(false);
               available.add(u);
            }
         }
         scn = oldscn = "id";
         asc = oldasc = true;
      } catch (Exception e) {
         String em = "Error during loadAvailableUsers()" + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
      availableUsers = available.toArray(new User[0]);
   }

   public void availableUserSelected() {
      // NOP
   }
   
   public boolean disableAssignToStudy() {
      if (availableUsers == null) return true;
      for (User u : availableUsers) {
         if (u.isSelected()) return false;
      }
      return true;
   }
   
   public boolean disableRemoveFromStudy() {
      if (assignedUsers != null) {
         for (StudyUser su : assignedUsers) {
            if (su.isSelected()) return false;
         }
      }
      return true;
   }

   /**
    * Makes a StudyUser object for each selected user, persists it and adds it
    * to the assigned users list.  Removes user object from available list.
    */
   public void assignToStudy() {
     List<User> users = new ArrayList<User>();
     List<StudyUser> studyUsers = new ArrayList<StudyUser>(Arrays.asList(assignedUsers));

     for (User u : availableUsers) {
        if (u.isSelected()) {
            StudyUser su = new StudyUser(0, chgStudy, u, 1, false);
            session.save(su);
            studyUsers.add(su);
        } else {
           users.add(u);
        }
     }
     availableUsers = users.toArray(new User[0]);
     assignedUsers = studyUsers.toArray(new StudyUser[0]);
   }

   /**
    * Deletes the StudyUser object for each selected user, and removes it from
    * the assigned users list. Adds user back to the available list.
    */
   public void removeFromStudy() {
      List<StudyUser> studyUsers = new ArrayList<StudyUser>();
      List<User> users = new ArrayList<User>(Arrays.asList(availableUsers));

      for (StudyUser su : assignedUsers) {
         if (su.isSelected()) {
            session.delete(su);
            users.add(su.getUser());
         } else {
            studyUsers.add(su);
         }
      }
     availableUsers = users.toArray(new User[0]);
     assignedUsers = studyUsers.toArray(new StudyUser[0]);
   }

   private StudyUser[] assignedUsers;
   private String sname, oldsname;
   public String getSname() { return sname; }
   public void setSname(String s) { sname = s; }
   private boolean ascend, oldascend;
   public boolean isAscend() { return ascend; }
   public void setAscend(boolean a) { ascend = a; }

   private void loadAssignedUsers() {
      assignedUsers = new StudyUser[0];
      sname = oldsname = "userId";
      ascend = oldascend = true;
      try {
         getNewSession();
         List<StudyUser> assigned = (List<StudyUser>) session
            .createQuery("from studyUser su fetch all properties where su.study.dba = :dba order by su.user.id")
            .setInteger("dba", chgStudy.getDba())
            .list();
         if (assigned != null) assignedUsers = assigned.toArray(new StudyUser[0]);
      } catch (Exception e) {
         String em = "Error during loadAvailableUsers()" + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }

   public StudyUser[] getAssignedUsers() {
      if (!sname.equals(oldsname) || ascend != oldascend) {
         oldsname = sname;
         oldascend = ascend;
         Comparator<StudyUser> comp = new StudyUser.StudyUserComparator(sname, ascend);
         Arrays.sort(assignedUsers, comp);
      }
      return assignedUsers;
   }

   public void setAssignedUsers(StudyUser[] assignedUsers) {
      this.assignedUsers = assignedUsers;
   }

   public void assignedUserSelected() {
      // NOP
   }

   public void saveStudyUserChanges() {
      transaction.commit();
      clearStudySelections();
      currentChangeStep = 1;
   }

   public void cancelStudyUserChanges() {
      transaction.rollback();
      clearStudySelections();
      currentChangeStep = 1;
   }

} // EO StudiesBean class
