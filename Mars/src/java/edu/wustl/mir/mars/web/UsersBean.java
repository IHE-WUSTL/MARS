package edu.wustl.mir.mars.web;

import com.icesoft.faces.component.paneltabset.TabChangeEvent;
import edu.wustl.mir.mars.db.User;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.apache.commons.validator.EmailValidator;

/**
 * Backing bean for the User maintenance functions: User.xhtml
 */
@ManagedBean
@SessionScoped
public class UsersBean extends BeanBase implements Serializable {

   @SuppressWarnings("FieldNameHidesFieldInSuperclass")
   static final long serialVersionUID = 1L;

    /** Creates a new instance of UsersBean */
    public UsersBean() {
       sessionBean.newTaskBean("usersBean");
       selectedTab = ADD_TAB_SELECTED;
       initializeAdd();
       initializeChg();
    }



   /****************************************************************************
    *********************************** panelTabSet Controls
    ***************************************************************************/

   private static final int ADD_TAB_SELECTED = 0;
   private static final int CHG_TAB_SELECTED = 1;
   private int selectedTab;
   public int getSelectedTab() {return selectedTab; }
   public void setSelectedTab(int s) {selectedTab = s; }

   public void tabChangeListener(TabChangeEvent tabChangeEvent) {
      switch (selectedTab) {
         case ADD_TAB_SELECTED:
            initializeAdd();
            break;
         case CHG_TAB_SELECTED:
            initializeChg();
            break;
         default:
            syslog.warn("Users.xhtml returned invalid tab selected value: " +
                    selectedTab);
      }
   }

   /****************************************************************************
    *********************************** Add tab methods
    ***************************************************************************/

   private User newUser;
   public User getNewUser() {return newUser;}
   public void setNewUser(User newUser) {this.newUser = newUser;}

   private String pw;
   public String getPw() {return pw;}
   public void setPw(String pw) {this.pw = pw;}

   public final void initializeAdd() {
      newUser = new User();
      pw = "";
   }

   public void addNewUser() {
      Valid v = new Valid();
      try {
         v.startValidations();
         v.NB("User Id", newUser.getId());
         newTransaction();
         User u = (User) session.createQuery("from user u where u.id = :id").
                 setString("id", newUser.getId()).uniqueResult();
         if (u != null) {
            v.error("User Id", "is already in use");
         }
         v.NB("First Name", newUser.getFirstName());
         v.NB("Last Name", newUser.getLastName());
         v.NB("Password ", newUser.getPw());
         if (!newUser.getPw().equals(pw)) {
            v.error("Passwords", "Don't match");
         }
         if (!EmailValidator.getInstance().isValid(newUser.getEmail())) {
            v.error("Email address", "missing/invalid");
         }
         if (v.isErrors()) {
            rollback();
            return;
         }
         session.save(newUser);
         commit();
         initializeAdd();
      } catch (Exception e) {
         syslog.warn("Error during add User: " + e.getMessage());
         v.error("Error during add User: ", e.getMessage());
      }
   }

  public void canAdd() {
     initializeAdd();
  }

   /****************************************************************************
    *********************************** Change tab methods
    ***************************************************************************/

   private User[] allUsers;  // List of users for table
   public User[] getAllUsers() {
      if (!sortColumnName.equals(oldSortColumnName) ||
              ascending != oldAscending) {
         oldSortColumnName = sortColumnName;
         oldAscending = ascending;
         Comparator<User> comp = new User.UserComparator(sortColumnName, ascending);
         Arrays.sort(allUsers, comp);
      }
      return allUsers;
   }
   public void setAllUsers(User[] allUsers) {this.allUsers = allUsers;}

   //----------------------------------------------------- Column sort
   private String sortColumnName, oldSortColumnName;
   private boolean ascending, oldAscending;
   public String getSortColumnName() {
      return sortColumnName;
   }

   public void setSortColumnName(String sortColumnName) {
      syslog.debug("sortColumnName set to " + sortColumnName);
      this.sortColumnName = sortColumnName;
   }

   public boolean isAscending() {
      return ascending;
   }

   public void setAscending(boolean ascending) {
      syslog.debug("ascending set to " + ascending);
      this.ascending = ascending;
   }


   private User chgUser = null;   // Selected user for changes
   public User getChgUser() {return chgUser;}
   public void setChgUser(User chgUser) {this.chgUser = chgUser;}

  /*
   * 1 = begin; No user selected
   * 2 = User selected; No action to take selected
   * 3 = Modify selected User
   * 4 = Delete selected User
   * 5 = Reset pw for selected user
   */
  private int currentChangeStep = 1;
  public boolean renderSelectUser()     { return currentChangeStep <= 2; }
  public boolean renderCommandButtons() { return currentChangeStep == 2; }
  public boolean renderMessage1()       { return currentChangeStep == 1; }
  public boolean renderMessage2()       { return currentChangeStep == 2; }
  public boolean renderModifyUser()     { return currentChangeStep == 3; }
  public boolean renderDeleteUser()     { return currentChangeStep == 4; }
  public boolean renderResetUserPw()    { return currentChangeStep == 5; }


   @SuppressWarnings("unchecked")
   public final void initializeChg() {
      if (chgUser != null) chgUser.setSelected(false);
      try {
         newTransaction();
         List<User> users = (List<User>) session.
                 createQuery("from user u order by u.id asc").list();
         allUsers = users.toArray(new User[0]);
         sortColumnName = oldSortColumnName = "id";
         ascending = oldAscending = true;
         rollback();
         currentChangeStep = 1;
      } catch (Exception e) {
         String em = "initializeChg() " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }

   public void userSelected() {
      for (User u : allUsers) {
         if (u.isSelected()) {
            chgUser = u;
            currentChangeStep = 2;
            return;
         }
      }
      currentChangeStep = 1;
   }

   public void chgUser() { currentChangeStep = 3; }
   public void delUser() { currentChangeStep = 4; }


   public void chgOk() {
      Valid v = new Valid();
      try {
         v.startValidations();
         v.NB("User Id", chgUser.getId());
         newTransaction();
         User u = (User) session.createQuery("from user u where u.id = :id and u.dba <> :dba").
              setString("id", chgUser.getId()).
              setInteger("dba", chgUser.getDba()).
              uniqueResult();
         if (u != null) v.error("User Id", "is already in use");
         rollback();
         v.NB("First Name", chgUser.getFirstName());
         v.NB("Last Name", chgUser.getLastName());
         if (v.isErrors()) return;
         newTransaction();
         session.saveOrUpdate(chgUser);
         commit();
         initializeChg();
      } catch (Exception e) {
         String em = "initializeChg() " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
     }
   }

   public void delOk() {
      try {
         newTransaction();
         session.delete(chgUser);
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

   /****************************************************************************
    *********************************** Reset password methods
    ***************************************************************************/

   public void chgPw() {
      currentChangeStep = 5;
      newPw = "";
      confirmPw = "";
   }
      
   public void resetPw() {
      Valid v = new Valid();
      v.startValidations();
      v.NB("Password ", newPw);
      if(!newPw.equals(confirmPw)) v.error("Passwords", "Don't match");
      if (v.isErrors()) {
         return;
      }
      try {
         newTransaction();
         chgUser.setPw(newPw);
         session.update(chgUser);
         commit();
      } catch (Exception e) {
         String em = "initializeChg() " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }

   private String newPw;
   public String getNewPw() { return newPw; }
   public void setNewPw(String newPw) {this.newPw = newPw; }

   private String confirmPw;
   public String getConfirmPw() {return confirmPw;}
   public void setConfirmPw(String confirmPw) {this.confirmPw = confirmPw;}

} // EO UsersBean
