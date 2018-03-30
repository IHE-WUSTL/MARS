package edu.wustl.mir.mars.web;

import edu.wustl.mir.mars.db.Alert;
import edu.wustl.mir.mars.db.Request;
import edu.wustl.mir.mars.db.StudyUser;
import edu.wustl.mir.mars.db.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import org.hibernate.Query;

/**
 *  for Alerts.xhtml
 */
@ManagedBean
@SessionScoped
public class AlertsBean extends BeanBase implements Serializable {

   @SuppressWarnings("FieldNameHidesFieldInSuperclass")
   static final long serialVersionUID = 1L;

   private User user;

   private StudyUser[] studyUsers;       // Active studies for this user
   private SelectItem[] studySelections;
   private String selectedStudyId;  // Id of selected study, or "All Studies"
   private final String ALL_STUDIES = "All Studies";

   private Alert[] alerts;
   private int totalNumberOfAlerts;
   private String sortColumnName, oldSortColumnName;
   private boolean ascending, oldAscending;

    /** Creates a new instance of AlertsBean */
    public AlertsBean() {
       initializeAlerts();
    }

    private void initializeAlerts() {
       sessionBean.newTaskBean("alertsBean");
       getNewSession();

       //---------------------------------- Pull current user data
       user = (User) session
              .createQuery("from user u fetch all properties " +
              "where u.dba = :dba order by u.id asc")
              .setInteger("dba", sessionBean.getUserDba())
              .uniqueResult();

       // Get list of active studies user is assigned to get alerts for
       List<StudyUser> sus = new ArrayList<StudyUser> ();
       for (StudyUser su : user.getStudyUsers())
          if (su.getStudy().isActive() && su.isReceiveAlerts()) sus.add(su);
       studyUsers = sus.toArray(new StudyUser[0]);
       //-----------------------------------------------------------
       // If user is assigned to more than one study, sort studies
       // by id and generate a SelectItem[] with "All Studies" and
       // them. If not, we just use that study. If no studies,
       // render a message on the screen
       //-----------------------------------------------------------
          Column.STUDY_ID.setRender(false);
       if (studyUsers.length > 1) {
          Comparator<StudyUser> cmp = new
                  StudyUser.StudyUserComparator("studyId", true);
          Arrays.sort(studyUsers, cmp);
          List<SelectItem> sis = new ArrayList<SelectItem>();
          sis.add(new SelectItem(ALL_STUDIES, "All Studies"));
          for (StudyUser su : studyUsers) {
             sis.add(new SelectItem(su.getStudy().getId(),
                     su.getStudy().getId()));
          }
          studySelections = sis.toArray(new SelectItem[0]);
          selectedStudyId = ALL_STUDIES;
          Column.STUDY_ID.setRender(true);
       } else if (studyUsers.length == 1) {
          selectedStudyId = studyUsers[0].getStudy().getId();
       } else {
          //---------------------------------------------------------
          // User has no studies, therefore no alerts.
          // set to -1 so that the "no alerts" message will not
          // appear along with the "no studies" messages.
          //---------------------------------------------------------
          totalNumberOfAlerts = -1;
          return;
       }
          loadAlertTable();
          totalNumberOfAlerts = alerts.length;
    } // EO InitializeAlerts()


    public void selectedStudyChange(ValueChangeEvent event) {
       loadAlertTable();
    }

    public void loadAlertTable() {
      String q;
      Query query;
      getNewSession();
      //--------------------------------------------- Load all alerts
      if (selectedStudyId.equals(ALL_STUDIES)) {
         q = "from alert a where a.request.study.dba in (?";
         for (int i = 1; i < studyUsers.length; i++) {
            q += ", ?";
         }
         q += ") order by a.lastName, a.firstName asc";
         query = session.createQuery(q);
         for (int i = 0; i < studyUsers.length; i++) {
            query.setInteger(i, studyUsers[i].getStudy().getDba());
         }
         //---------------------------- Load alerts for a single study
      } else {
         query = session.createQuery("from alert a " +
                 "where a.request.study.id = :id " +
                 "order by a.lastName, a.firstName asc")
                 .setString("id", selectedStudyId);
      }
      @SuppressWarnings("unchecked")
      List<Alert> allAlerts = (List<Alert>) query.list();
      alerts = allAlerts.toArray(new Alert[0]);
      setRoleForAlerts();
      sortColumnName = oldSortColumnName = "name";
      ascending = oldAscending = true;
      discardSession();
   }

    private void setRoleForAlerts() {
      boolean r = false;
      for (int i = 0; i < alerts.length; i++) {
         alerts[i].setRenderDeleteButton(false);
         for (StudyUser su : studyUsers) {
            if (su.getStudy().getDba() == alerts[i].getRequest().getStudy().getDba()
                    && su.getUserRole() > 0) {
               alerts[i].setRenderDeleteButton(true);
               r = true;
               break;
            }
         }
      }
      Column.REQUEST_DEL.setRender(r);
      Column.ALERT_DEL.setRender(r);
   }

    //------------------------------------------------- Alert table columns

    public enum Column {
       STUDY_ID (75, 0, ""),
       REQUEST_MPI (75, 0, "#b1ede9"),
       REQUEST_NAME (150, 0, "#b1ede9"),
       REQUEST_DOB (75, 0, "#b1ede9"),
       REQUEST_SEX (40, 0, "#b1ede9"),
       REQUEST_TYPES (40, 0, ""),
       REQUEST_DEL (75, 0, ""),
       ALERT_MPI (75, 1, "#faeea3"),
       ALERT_NAME (150, 1, "#faeea3"),
       ALERT_DOB (75, 1, "#faeea3"),
       ALERT_SEX (40, 1, "#faeea3"),
       ALERT_EVENT(60, 1, ""),
       ALERT_DESC (200, 1, ""),
       ALERT_DATE (75, 1, ""),
       ALERT_DEL (75, 1, "");

       private Column(int w, int g, String c) {
          width = w;
          columnGroup = g;
          backgroundColor = c;
       }

       private boolean render = true;
       private int width;                // in px
       private String backgroundColor;
       private int columnGroup;
       private static final int REQUEST_COL = 0;
       private static final int ALERT_COL = 1;

       public boolean isRender() { return render; }
       public void setRender(boolean r) { render = r; }
    } // EO Column enum

       // width style for column name, empty string if not found.
       public String style(String name) {
          try {
            Column c = Column.valueOf(name);
            String style = "width: " + c.width + "px;";
            if (!c.backgroundColor.isEmpty())
               style += "background-color: " + c.backgroundColor + ";";
            return style;
          } catch (IllegalArgumentException e) {
             FacesUtils.addErrorMessage(name + " " + e.getMessage());
             return "";
          }
       }
       // Is column with name to be rendered?
       public boolean render(String name) {
          try {
            Column c = Column.valueOf(name);
            return c.render;
          } catch (IllegalArgumentException e) {
             FacesUtils.addErrorMessage(name + " " + e.getMessage());
             return true;
          }
       }

       //---------- columnWidths entry for dataTable tag
       public String columnWidths() {
          StringBuilder b = new StringBuilder();
          for (Column c : Column.values()) {
             if (c.render) b.append(c.width).append("px, ");
          }
          int i = b.lastIndexOf(", ");
          b.delete(i, i + 1);
          return b.toString();
       }

       public int requestColumns() {
          int i = 0;
          for (Column c: Column.values()) {
             if (c.render && c.columnGroup == Column.REQUEST_COL) i++;
          }
          return i;
       }

       public int alertColumns() {
          int i = 0;
          for (Column c: Column.values()) {
             if (c.render && c.columnGroup == Column.ALERT_COL) i++;
          }
          return i;
       }

       public int totalColumns() {
          return requestColumns() + alertColumns();
       }

    public void deleteRequest(Alert a) {
       getNewSession();
       Long count = (Long) session.createQuery("select count(*) from alert a " +
               "where a.request.dba = :dba")
               .setInteger("dba", a.getRequest().getDba())
               .uniqueResult();
       Request toDel = (Request) session.createQuery("from request r where r.dba = :dba")
               .setInteger("dba", a.getRequest().getDba())
               .uniqueResult();
       session.delete(toDel);
       transaction.commit();
       loadAlertTable();
       totalNumberOfAlerts -= count;
    }

    public void deleteAlert(Alert a) {
       getNewSession();
       Alert toDel = (Alert) session.createQuery("from alert a where a.dba = :dba")
               .setInteger("dba", a.getDba())
               .uniqueResult();
       session.delete(toDel);
       transaction.commit();
       loadAlertTable();
       totalNumberOfAlerts--;
    }

    public int numberOfStudies() { return studyUsers.length; }
    public int numberOfAlerts() { return alerts.length; }
    public int totalNumberOfAlerts() { return totalNumberOfAlerts; }
    public StudyUser getStudyUser() { return studyUsers[0];}

   public Alert[] getAlerts() {
      if (!sortColumnName.equals(oldSortColumnName) ||
              ascending != oldAscending) {
         oldSortColumnName = sortColumnName;
         oldAscending = ascending;
         Comparator<Alert> comp = new Alert.AlertComparator(sortColumnName, ascending);
         Arrays.sort(alerts, comp);
      }
      return alerts;
   }

   public void setAlerts(Alert[] alerts) {
      this.alerts = alerts;
   }

   public boolean isAscending() {
      return ascending;
   }

   public void setAscending(boolean ascending) {
      this.ascending = ascending;
   }

   public String getSelectedStudyId() {
      return selectedStudyId;
   }

   public void setSelectedStudyId(String selectedStudyId) {
      this.selectedStudyId = selectedStudyId;
   }

   public String getSortColumnName() {
      return sortColumnName;
   }

   public void setSortColumnName(String sortColumnName) {
      this.sortColumnName = sortColumnName;
   }

   public SelectItem[] getStudySelections() {
      return studySelections;
   }

   public void setStudySelections(SelectItem[] studySelections) {
      this.studySelections = studySelections;
   }

}
