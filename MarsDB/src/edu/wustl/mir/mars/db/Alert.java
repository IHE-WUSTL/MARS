
package edu.wustl.mir.mars.db;

import java.io.Serializable;
import java.util.Comparator;
import java.sql.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 * Alert table entity bean
 */
@Entity(name="alert")
public class Alert  implements Serializable{

   static final long serialVersionUID = 1L;
   static final String[] eventName = {"scheduled", "cancelled", "reported" };
   public static final int SCHEDULED = 0;
   public static final int CANCELLED = 1;
   public static final int REPORTED  = 2;
   static final String MATCH_CSS = "marsCell";
   static final String NON_MATCH_CSS = "marsDifferentCell";

   //-------------------------------------------- persisted properties
   private int dba = 0;
   
   private String firstName = "";
   private String lastName = "";
   private String mpi = "";
   private Date dob = null;
   private String sex = "U";
   private int event;
   private Date arrivalTime = new Date(new java.util.Date().getTime());
   private String description;
   private Request request;

   //-------------------------------------- Transient properties
   private boolean selected = false;
   private boolean renderDeleteButton = false;

   public Alert() {}

   public Date getArrivalTime() {
      return arrivalTime;
   }

   public void setArrivalTime(Date arrivalTime) {
      this.arrivalTime = arrivalTime;
   }
   //-------- These are for the JSF, using java.util.Date
   @Transient
   public java.util.Date getDispArrivalTime() {
      return new java.util.Date(arrivalTime.getTime());
   }
   public void setDispArrivalTime(java.util.Date dd) {
      arrivalTime = new Date(dd.getTime());
   }

   @Id
   @GeneratedValue(strategy=GenerationType.IDENTITY)
   public int getDba() {
      return dba;
   }

   public void setDba(int dba) {
      this.dba = dba;
   }

   //---------------------- these are java.sql.Date, for the DB
   public Date getDob() {
      return dob;
   }

   public void setDob(Date dob) {
      this.dob = dob;
   }
   //-------- These are for the JSF, using java.util.Date
   @Transient
   public java.util.Date getDispDob() {
      if (dob == null) return null;
      return new java.util.Date(dob.getTime());
   }
   public void setDispDob(java.util.Date dd) {
      if (dd == null) dob = null;
      else dob = new Date(dd.getTime());
   }

   public String dobClass () {
      if (dob != null && request.getDob() != null &&
              dob.equals(request.getDob())) return MATCH_CSS;
      return NON_MATCH_CSS;
   }

   @Column (length=1)
   public String getSex() {
      return sex;
   }

   public void setSex(String sex) {
      this.sex = sex;
   }

   public String sexClass () {
      if (sex.equals(request.getSex())) return MATCH_CSS;
      return NON_MATCH_CSS;
   }

   public int getEvent() {
      return event;
   }

   public void setEvent(int event) {
      this.event = event;
   }
   public String eventName() {
      return eventName[event];
   }

   public String getFirstName() {
      return firstName;
   }

   public void setFirstName(String firstName) {
      this.firstName = firstName;
   }

   public String firstNameClass () {
      if (firstName.equals(request.getFirstName())) return MATCH_CSS;
      return NON_MATCH_CSS;
   }

   public String lastNameClass () {
      if (lastName.equals(request.getLastName())) return MATCH_CSS;
      return NON_MATCH_CSS;
   }

   public String getLastName() {
      return lastName;
   }

   public void setLastName(String lastName) {
      this.lastName = lastName;
   }

   public String getMpi() {
      return mpi;
   }

   public void setMpi(String mpi) {
      this.mpi = mpi;
   }

   public String mpiClass () {
      if (mpi.equals(request.getMpi())) return MATCH_CSS;
      return NON_MATCH_CSS;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   @ManyToOne
   @JoinColumn(name="requestDba")
   public Request getRequest() {
      return request;
   }

   public void setRequest(Request request) {
      this.request = request;
   }

   @Transient
   public boolean isSelected() {
      return selected;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   @Transient
   public boolean isRenderDeleteButton() {
      return renderDeleteButton;
   }

   public void setRenderDeleteButton(boolean renderDeleteButton) {
      this.renderDeleteButton = renderDeleteButton;
   }




   public static class AlertComparator implements Comparator<Alert> {

      private enum Comparators { STUDY, REQUEST, ALERT }
      private enum Comparisons {
         SID             ("studyId",               "id",            Comparators.STUDY),
         RLASTNAME       ("requestLastName",       "lastName",      Comparators.REQUEST),
         RFIRSTNAME      ("requestFirstName",      "firstName",     Comparators.REQUEST),
         RNAME           ("requestName",           "name",          Comparators.REQUEST),
         RMPI            ("requestMpi",            "mpi",           Comparators.REQUEST),
         RDOB            ("requestDob",            "dob",           Comparators.REQUEST),
         RSEX            ("requestSex",            "sex",           Comparators.REQUEST),
         RSCHEDULEALERTS ("requestScheduleAlerts", "scheduleAlerts",Comparators.REQUEST),
         RREPORTALERTS   ("requestReportAlerts",   "reportAlerts",  Comparators.REQUEST),
         LASTNAME        ("lastName",              "lastName",      Comparators.ALERT),
         FIRSTNAME       ("firstName",             "firstName",     Comparators.ALERT),
         NAME            ("name",                  "name",          Comparators.ALERT),
         MPI             ("mpi",                   "mpi",           Comparators.ALERT),
         DOB             ("dob",                   "dob",           Comparators.ALERT),
         SEX             ("sex",                   "sex",           Comparators.ALERT),
         ARRIVALTIME     ("arrivalTime",           "arrivalTime",   Comparators.ALERT);

         public final String externalColumnName;
         public final String internalColumnName;
         public final Comparators comparatorToUse;

         private Comparisons (String external, String internal, Comparators comparator) {
            externalColumnName = external;
            internalColumnName = internal;
            comparatorToUse = comparator;
         }
         public static Comparisons getComparisonForExternalColumnName(String external) {
            for (Comparisons c : Comparisons.values()) {
               if (c.externalColumnName.equalsIgnoreCase(external)) return c;
            }
            return null;
         }
      } // EO Comparisons enum

      private String col = null;
      private boolean asc;
      Comparisons comparison;
      private Comparator<Study> studyComparator = null;
      private Comparator<Request> requestComparator = null;

      public AlertComparator(String columnName, boolean ascending) {
         asc = ascending;
         comparison = Comparisons.getComparisonForExternalColumnName(columnName);
         if (comparison != null) {
            col = comparison.internalColumnName;
            switch (comparison.comparatorToUse) {
               case STUDY:
                  studyComparator = new Study.StudyComparator(col, asc);
                  break;
               case REQUEST:
                  requestComparator = new Request.RequestComparator(col, asc);
                  break;
               default:
            }
         }
      } // EO AlertrComparator(String columnName, boolean ascending)

   @Override
   public int compare (Alert one, Alert two) {
      if (col == null) return 0;

         switch (comparison.comparatorToUse) {
            case STUDY:
               return studyComparator.compare(one.getRequest().getStudy(), two.getRequest().getStudy());
            case REQUEST:
               return requestComparator.compare(one.getRequest(), two.getRequest());
            case ALERT:
               if (col.equalsIgnoreCase(Comparisons.LASTNAME.internalColumnName)) {
                  return asc
                          ? one.getLastName().compareToIgnoreCase(two.getLastName())
                          : two.getLastName().compareToIgnoreCase(one.getLastName());
               } else if (col.equalsIgnoreCase(Comparisons.FIRSTNAME.internalColumnName)) {
                  return asc
                          ? one.getFirstName().compareToIgnoreCase(two.getFirstName())
                          : two.getFirstName().compareToIgnoreCase(one.getFirstName());
               } else if (col.equalsIgnoreCase(Comparisons.NAME.internalColumnName)) {
                  int t = asc
                          ? one.getLastName().compareToIgnoreCase(two.getLastName())
                          : two.getLastName().compareToIgnoreCase(one.getLastName());
                  if (t != 0) {
                     return t;
                  }
                  return asc
                          ? one.getFirstName().compareToIgnoreCase(two.getFirstName())
                          : two.getFirstName().compareToIgnoreCase(one.getFirstName());
               } else if (col.equalsIgnoreCase(Comparisons.MPI.internalColumnName)) {
                  return asc
                          ? one.getMpi().compareToIgnoreCase(two.getMpi())
                          : two.getMpi().compareToIgnoreCase(one.getMpi());
               } else if (col.equalsIgnoreCase(Comparisons.DOB.internalColumnName)) {
                  return asc
                          ? one.getDob().compareTo(two.getDob())
                          : two.getDob().compareTo(one.getDob());
               } else if (col.equalsIgnoreCase(Comparisons.SEX.internalColumnName)) {
                  return asc
                          ? one.getSex().compareTo(two.getSex())
                          : two.getSex().compareTo(one.getSex());
               }
               return 0;
            default:
               return 0;
         }  // EO switch
      } // EO compare(Alert one, Alert two)
   } // EO AlertComparator class
} // EO Alert class

