
package edu.wustl.mir.mars.web;

import com.icesoft.faces.component.paneltabset.TabChangeEvent;
import edu.wustl.mir.mars.db.Request;
import edu.wustl.mir.mars.db.Study;
import edu.wustl.mir.mars.db.StudyUser;
import edu.wustl.mir.mars.db.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.sql.Date;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import org.apache.commons.lang.StringUtils;

/**
 * for Request.xhtml
 */
@ManagedBean
@SessionScoped
public class RequestBean extends BeanBase implements Serializable {

   @SuppressWarnings("FieldNameHidesFieldInSuperclass")
   static final long serialVersionUID = 1L;

   //********************** State processing and render flags

   public enum State {

      CHOOSE_STUDY (true,  false, false, false, false, false, false),
      ADD_REQUEST  (false, true,  false, false, false, false, false),
      CHG_REQUEST  (false, true,  false, true,  false, false, false),
      CHG_TYPE     (false, true,  false, true,  true,  false, false),
      MOD_REQUEST  (false, true,  false, false, false, true,  false),
      DEL_REQUEST  (false, true,  false, false, false, false, true),
      VIEW_REQUEST (false, false, true,  false, false, false, false);

      private State( boolean st, boolean t, boolean v,
              boolean ct, boolean ctb, boolean m, boolean d) {
         renderStudyTable = st;
         renderTabs = t;
         renderView = v;
         renderChgTable = ct;
         renderChgTypeButtons = ctb;
         renderModify = m;
         renderDelete = d;
      }
      private boolean renderStudyTable;
      private boolean renderTabs;
      private boolean renderView;
      private boolean renderChgTypeButtons;
      private boolean renderChgTable;
      private boolean renderModify;
      private boolean renderDelete;

      public boolean renderStudyTable()     { return renderStudyTable; }
      public boolean renderTabs()           { return renderTabs; }
      public boolean renderView()           { return renderView; }
      public boolean renderChgTypeButtons() { return renderChgTypeButtons; }
      public boolean renderChgTable()       { return renderChgTable; }
      public boolean renderModify()         { return renderModify; }
      public boolean renderDelete()         { return renderDelete; }

   } // EO State enum

   private State state;

   public State getState() {
      return state;
   }

   public void setState(State state) {
      this.state = state;
   }
   
   private void changeState(State newState) {
      state = newState;
      switch (newState) {
         case CHOOSE_STUDY:
            initializeChooseStudyState();
            break;
         case ADD_REQUEST:
            initializeAddRequestState();
            break;
         case CHG_REQUEST:
         case VIEW_REQUEST:
            initializeChgRequestState();
            break;
         default:
      }
   }

   // Instantiate bean, load general items. Set initial state and initialize it.
   public RequestBean() {
      initialize();
   }

   //--------------------------------------------------------------------------
   //  initialization processing
   //--------------------------------------------------------------------------

   User user;    // current user
   
   private void initialize() {
      sessionBean.newTaskBean("requestBean");
      changeState(State.CHOOSE_STUDY);
   }

   //--------------------------------------------------------------------------
   //  CHOOSE_STUDY state processing
   //--------------------------------------------------------------------------

   private StudyUser[] studyUsers;

   private String sortColumnName, oldSortColumnName;
   private boolean ascending, oldAscending;

   private StudyUser studyUser;  // StudyUser selected to work with
   private Study study;          // Study from this studyUser

   private void initializeChooseStudyState() {
      getNewSession();
      user = (User) session
              .createQuery("from user u fetch all properties where u.dba = :dba")
              .setInteger("dba", sessionBean.getUserDba())
              .uniqueResult();
      List<StudyUser> sus = new ArrayList<StudyUser> ();
      for (StudyUser su : user.getStudyUsers())
         if (su.getStudy().isActive()) sus.add(su);
      studyUsers = sus.toArray(new StudyUser[0]);
      sortColumnName = oldSortColumnName = "id";
      ascending = oldAscending = true;
      discardSession();
   }

    //************************** User selected a studyUser to work on
    public void studyUserSelected() {
      studyUser = null;
      for (StudyUser su : studyUsers) {
         if (su.isSelected()) {
            su.setSelected(false);
            studyUser = su;
            study = studyUser.getStudy();
            switch (studyUser.getUserRole()) {
               case 0:
               default:
                  changeState(State.VIEW_REQUEST);
                  break;
               case 1:
               case 2:
                  changeState(State.ADD_REQUEST);
                  break;
            }
            break;
         }
      }
   }

   public StudyUser[] getStudyUsers() {
      if (!sortColumnName.equals(oldSortColumnName) ||
              ascending != oldAscending) {
         oldSortColumnName = sortColumnName;
         oldAscending = ascending;
         Comparator<StudyUser> comp = new StudyUser.StudyUserComparator(sortColumnName, ascending);
         Arrays.sort(studyUsers, comp);
      }
      return studyUsers;
   }

   public void setStudies(StudyUser[] studyUsers) {
      this.studyUsers = studyUsers;
   }

   public boolean studyUsersEmpty() {
      return studyUsers.length == 0;
   }

   public boolean isAscending() {
      return ascending;
   }

   public void setAscending(boolean ascending) {
      this.ascending = ascending;
   }


   public String getSortColumnName() {
      return sortColumnName;
   }

   public void setSortColumnName(String sortColumnName) {
      this.sortColumnName = sortColumnName;
   }

   //--------------------------------------------------------------------------
   //  Tab panel for both add and chg states
   //--------------------------------------------------------------------------
    
   private static final int ADD_TAB_SELECTED = 0;
   private static final int CHG_TAB_SELECTED = 1;
   private int selectedTab;
   public int getSelectedTab() {return selectedTab; }
   public void setSelectedTab(int s) {selectedTab = s; }

   public void tabChangeListener(TabChangeEvent tabChangeEvent) {
      switch (selectedTab) {
         case ADD_TAB_SELECTED:
            changeState(State.ADD_REQUEST);
            break;
         case CHG_TAB_SELECTED:
            changeState(State.CHG_REQUEST);
            break;
         default:
            syslog.warn("Request.xhtml returned invalid tab selected value: " +
                    selectedTab);
      }
   }

   //--------------------------------------------------------------------------
   //  ADD_REQUEST state processing
   //--------------------------------------------------------------------------

   Request newRequest;
   Request[] similarRequests;


   SelectItem[] sexes = {
      new SelectItem("M", "Male"),
      new SelectItem("F", "Female"),
      new SelectItem("U", "Unknown")
   };
   public SelectItem[] getSexes() { return sexes; }

   private void initializeAddRequestState() {
      newRequest = new Request(study);
      similarRequests = new Request[0];
      selectedTab = ADD_TAB_SELECTED;
   }

   public boolean disableAddMatch() {
      if (StringUtils.trimToEmpty(newRequest.getMpi()).length() > 0) return true;
      if (StringUtils.trimToEmpty(newRequest.getLastName()).length() > 0 &&
          StringUtils.trimToEmpty(newRequest.getLastName()).length() > 0) return true;
      return false;
   }

   @SuppressWarnings("unchecked")
   public void addNewRequest() {
      similarRequests = new Request[0];
      List<Request> similar = new ArrayList<Request>();
      Valid v = new Valid();
      try {
         v.startValidations();
         // Basic validation, must have last name and at least two other demographic elements
         v.NB("lastName", newRequest.getLastName());
         int i = 0;
         if (StringUtils.isNotBlank(newRequest.getMpi())) i++;
         if (StringUtils.isNotBlank(newRequest.getFirstName())) i++;
         if (newRequest.getDob() != null) i++;
         if (!newRequest.getSex().equals("U")) i++;
         if (i < 2) v.error("Must have at least three demographics", "");
         if (v.isErrors()) return;

         // Look for existing requests which are similar in this studyUser
         newTransaction();
         List<Request> sim = (List<Request>) session.createQuery("from request r " +
                 "where r.study.dba = :studyDba and (" +
                 "(r.mpi       is not null and r.mpi       is not empty and r.mpi       = :mpi)       or " +
                 "(r.firstName is not null and r.firstName is not empty and r.firstName = :firstName) or " +
                 "(r.lastName  is not null and r.lastName  is not empty and r.lastName  = :lastName)  or " +
                 "(r.dob       is not null and r.dob = :dob) or " +
                 "(r.sex is not null and r.sex is not empty and r.sex != 'U' and r.sex = :sex))")
                 .setInteger("studyDba", studyUser.getStudy().getDba())
                 .setString("mpi", newRequest.getMpi())
                 .setString("lastName", newRequest.getLastName())
                 .setString("firstName", newRequest.getFirstName())
                 .setDate("dob", newRequest.getDob())
                 .setString("sex", newRequest.getSex())
                 .list();
         // Scan results looking for 'similar' requests
         for (Request r : sim) {
            // If mpis match, it is similar
            if (areMatchedStrings(r.getMpi(), newRequest.getMpi())) {
               similar.add(r);
               continue;
            }
            // If any two other demographics match, it is similar.
            i = 0;
            if (areMatchedStrings(r.getLastName(), newRequest.getLastName())) i++;
            if (areMatchedStrings(r.getFirstName(), newRequest.getFirstName())) i++;
            if (areMatchedDates(r.getDob(), newRequest.getDob())) i++;
            if (areMatchedSexes(r.getSex(), newRequest.getSex())) i++;
            if (i >= 2) similar.add(r);
         }
         // If there are any similar requests, return to show them
            if (similar.size() > 0) {
               similarRequests = similar.toArray(new Request[0]);
               return;
         }
         newTransaction();
         session.save(newRequest);
         transaction.commit();
         changeState(State.ADD_REQUEST);
         //------------------------------------------------ Add the request
      } catch (Exception e) {
         String em = "Error during addRequest()" + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }  // EO Add new Request method

   public void addAnyway() {
      try {
         newTransaction();
         session.save(newRequest);
         transaction.commit();
         changeState(State.ADD_REQUEST);
         //------------------------------------------------ Add the request
      } catch (Exception e) {
         String em = "Error during addAnyway()" + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }

   public void cancelNewRequest() {
         changeState(State.ADD_REQUEST);
   }

   public void doneWithStudy() {
      changeState(State.CHOOSE_STUDY);
   }

    //**************************************** Getters & Setters

   public StudyUser getStudy() {
      return studyUser;
   }

   public void setStudy(StudyUser study) {
      this.studyUser = study;
   }

   public Request getNewRequest() {
      return newRequest;
   }

   public void setNewRequest(Request newRequest) {
      this.newRequest = newRequest;
   }

   public Request[] getSimilarRequests() {
      return similarRequests;
   }

   public void setSimilarRequests(Request[] similarRequests) {
      this.similarRequests = similarRequests;
   }

   public int similarRequestsCount() {
      return similarRequests.length;
   }

   //--------------------------------------------------------------------------
   //  CHG_REQUEST state processing
   //  VIEW_REQUEST state processing
   //  Note: VIEW_REQUEST is the same as CHG_REQUEST except that the table of
   //  Requests has no select option, so the user cannot choose a request to
   //  Change, and as a result cannot change anything.
   //--------------------------------------------------------------------------

   private Request[] requests;   // request for table
   private Request chgRequest = null;   // request selected from table to modify
   //----------------------------------------------------- Column sort
   private String sortCol, oldSortCol;
   private boolean ascend, oldAscend;

   public boolean disableChgMatch() {
      if (StringUtils.trimToEmpty(chgRequest.getMpi()).length() > 0) return true;
      if (StringUtils.trimToEmpty(chgRequest.getLastName()).length() > 0 &&
          StringUtils.trimToEmpty(chgRequest.getLastName()).length() > 0) return true;
      return false;
   }

   @SuppressWarnings("unchecked")
   private void initializeChgRequestState() {
      if (chgRequest != null) chgRequest.setSelected(false);
      try {
         newTransaction();
         List<Request> reqs = (List<Request>) session
                 .createQuery("from request r where r.study.dba = :studyDba " +
                 "order by r.lastName, r.firstName asc")
                 .setInteger("studyDba", study.getDba())
                 .list();
         sortCol = oldSortCol = "name";
         ascend = oldAscend = true;
         discardSession();
         requests = reqs.toArray(new Request[0]);
      } catch (Exception e) {
         String em = "initializeChgRequestState() " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }

   // This can be called from CHG_TYPE state also
   public void requestSelected() {
      chgRequest = null;
      for (Request r : requests) {
         if (r.isSelected()) {
            chgRequest = r;
            syslog.warn("in dob = " + chgRequest.getDob().toString());
            changeState(State.CHG_TYPE);
            return;
         }
      }
      changeState(State.CHG_REQUEST);
   }


    //**************************************** Getters & Setters


   public Request[] getRequests() {
      if (!sortCol.equals(oldSortCol) ||
              ascend != oldAscend) {
         oldSortCol = sortCol;
         oldAscend = ascend;
         Comparator<Request> comp = new Request.RequestComparator(sortCol, ascend);
         Arrays.sort(requests, comp);
      }
      return requests;
   }

   public void setRequests(Request[] requests) {
      this.requests = requests;
   }

   public boolean isRequestsEmpty() {
      return requests == null || requests.length == 0;
   }
   public void setRequestsEmpty(boolean e) {

   }


   public boolean isAscend() {
      return ascend;
   }

   public void setAscend(boolean ascend) {
      this.ascend = ascend;
   }

   public String getSortCol() {
      return sortCol;
   }

   public void setSortCol(String sortCol) {
      this.sortCol = sortCol;
   }

   public Request getChgRequest() {
      return chgRequest;
   }

   public void setChgRequest(Request chgRequest) {
      this.chgRequest = chgRequest;
   }


   //--------------------------------------------------------------------------
   //  CHG_TYPE state processing   NO initialization required
   //--------------------------------------------------------------------------


   public void chgTypeModifyRequest() {
      changeState(State.MOD_REQUEST);
   }

   public void chgTypeDeleteRequest() {
      changeState(State.DEL_REQUEST);
   }


   //--------------------------------------------------------------------------
   //  MOD_REQUEST state processing   NO initialization required
   //--------------------------------------------------------------------------

   public void modRequestUpdate() {
      try {
         getNewSession();
         syslog.warn("dob = " + chgRequest.getDob().toString());
         session.saveOrUpdate(chgRequest);
         commit();
      } catch (Exception e) {
         String em = "Update Request failed: " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
      changeState(State.CHG_REQUEST);
   }

   public void modRequestCancel() {
      discardSession();
      changeState(State.CHG_REQUEST);
   }


   //--------------------------------------------------------------------------
   //  DEL_REQUEST state processing   NO initialization required
   //--------------------------------------------------------------------------

   public void delRequest() {
      try {
         getNewSession();
         session.delete(chgRequest);
         commit();
      } catch (Exception e) {
         String em = "Delete Request failed: " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
      changeState(State.CHG_REQUEST);
   }

   public void delRequestCancel() {
      discardSession();
      changeState(State.CHG_REQUEST);
   }

   //-----------------------------------------------------------------------
   // Match patient in RIS to this Request
   //-----------------------------------------------------------------------

   private RISMatch match = null;

   /* Ends the RIS Match function */
   public void clearRISMatch() { match = null; }

   /* starts RIS Match function if possible */
   public void startRISMatch(ActionEvent ae) {
      Request req = (Request) ae.getComponent().getAttributes().get("request");
      match = new RISMatch();
      try {
         match.setup(this, req);
      } catch (Exception e) {
         syslog.info(e.getMessage());
         FacesUtils.addErrorMessage(e.getMessage());
         clearRISMatch();
      }
   }


   //************************************************************************
   //************************************************************************
   //****************************** Miscl ***********************************
   //************************************************************************
   //************************************************************************
       /**
        * Returns true for a meaningful match of the two strings.<br/>
        * If either is null, empty, or only whitespaces, returns false<br/>
        * Returns true if the two strings match, case insensitive.
        * @param a
        * @param b
        * @return boolean are the strings meaninfully matched?
        */
       public static boolean areMatchedStrings(String a, String b) {
          if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) return false;
          return a.equalsIgnoreCase(b);
       }
       public static boolean areMatchedDates(Date a, Date b) {
          if (a == null || b == null) return false;
          return a.equals(b);
       }
       public static boolean areMatchedSexes(String a, String b) {
          if (StringUtils.isBlank(a) || StringUtils.isBlank(b)) return false;
          if (a.equals("U")) return false;
          return a.equalsIgnoreCase(b);
       }

} // EO RequestBean class
