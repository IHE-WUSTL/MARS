
package edu.wustl.mir.mars.db;

import java.io.Serializable;
import java.text.SimpleDateFormat;
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
 * Request table entity bean
 */
@Entity(name="request")
public class Request implements Serializable{

   static final long serialVersionUID = 1L;

   //-------------------------------------------- persisted properties
   private int dba = 0;
   private Study study;
   private String firstName = "";
   private String lastName = "";
   private String mpi = "";
   private Date dob = null;
   private String sex = "U";
   private boolean scheduleAlerts = true;
   private boolean finalReportAlerts = true;
   private String ptItn = "";

   //-------------------------------------- Transient properties
   private boolean selected = false;

   public Request() {}
   public Request(Study study) {
      this.study = study;
   }

   @Id
   @GeneratedValue(strategy=GenerationType.IDENTITY)
   public int getDba() {
      return dba;
   }

   public void setDba(int dba) {
      this.dba = dba;
   }

   @ManyToOne
   @JoinColumn(name="studyDba")
   public Study getStudy() {
      return study;
   }

   public void setStudy(Study study) {
      this.study = study;
   }

   //------------------ These set the DB, using java.sql.Date
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
      if (dd == null) {
         dob = null;
         return;
      }
      dob = new Date(dd.getTime());
   }

   public boolean isFinalReportAlerts() {
      return finalReportAlerts;
   }

   public void setFinalReportAlerts(boolean finalReportAlerts) {
      this.finalReportAlerts = finalReportAlerts;
   }

   public String getFirstName() {
      return firstName;
   }

   public void setFirstName(String firstName) {
      this.firstName = firstName;
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
      //-------------------------------------- trim off excess leading zeroes
      while (mpi.length() > 1 && mpi.startsWith("0")) mpi = mpi.substring(1);
      this.mpi = mpi;
   }

   public boolean isScheduleAlerts() {
      return scheduleAlerts;
   }

   public void setScheduleAlerts(boolean scheduleAlerts) {
      this.scheduleAlerts = scheduleAlerts;
   }

   public String scheduledAlerts() {
      String a = "";
      if (isScheduleAlerts()) a += "S";
      if (isFinalReportAlerts()) a += "R";
      return a;
   }

   @Column (length=1)
   public String getSex() {
      return sex;
   }

   public void setSex(String sex) {
      this.sex = sex;
   }
   
   @Transient
   public String getPtItn() {
      return ptItn;
   }
   public void setPtItn(String ptItn) {
      this.ptItn = ptItn;
   }

   @Transient
   public boolean isSelected() {
      return selected;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   public String toString() {
      SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy");
      return  "[Request: dba="   + dba +
                       " Study=" + study.getId() +
                       " firstName=" + firstName +
                       " lastName="  + lastName +
                       " mpi="       + mpi +
                       " dob="       + df.format(dob) +
                       " sched="     + scheduleAlerts +
                       " report="    + finalReportAlerts
              + "]";
   }

   public static class RequestComparator implements Comparator<Request> {

      private enum Comparators { STUDY, REQUEST }
      private enum Comparisons {
         STUDYID        ("studyId",       "id",            Comparators.STUDY),
         LASTNAME       ("lastName",      "lastName",      Comparators.REQUEST),
         FIRSTNAME      ("firstName",     "firstName",     Comparators.REQUEST),
         NAME           ("name",          "name",          Comparators.REQUEST),
         MPI            ("mpi",           "mpi",           Comparators.REQUEST),
         DOB            ("dob",           "dob",           Comparators.REQUEST),
         SEX            ("sex",           "sex",           Comparators.REQUEST),
         SCHEDULEALERTS ("scheduleAlerts","scheduleAlerts",Comparators.REQUEST),
         REPORTALERTS   ("reportAlerts",  "reportAlerts",  Comparators.REQUEST);

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

      public RequestComparator(String columnName, boolean ascending) {
         asc = ascending;
         comparison = Comparisons.getComparisonForExternalColumnName(columnName);
         if (comparison != null) {
            col = comparison.internalColumnName;
            switch (comparison.comparatorToUse) {
               case STUDY:
                  studyComparator = new Study.StudyComparator(col, asc);
                  break;
               default:
            }
         }
      } // EO RequestComparator(String columnName, boolean ascending)

      @Override
      public int compare(Request one, Request two) {
         if (col == null) {
            return 0;
         }
         switch (comparison.comparatorToUse) {
            case STUDY:
               return studyComparator.compare(one.getStudy(), two.getStudy());
            case REQUEST:
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
               } else if (col.equalsIgnoreCase(Comparisons.SCHEDULEALERTS.internalColumnName)) {
                  if (one.isScheduleAlerts() == two.isScheduleAlerts()) {
                     return 0;
                  }
                  return asc
                          ? one.isScheduleAlerts() ? 1 : -1
                          : one.isScheduleAlerts() ? -1 : 1;
               } else if (col.equalsIgnoreCase(Comparisons.REPORTALERTS.internalColumnName)) {
                  if (one.isScheduleAlerts() == two.isScheduleAlerts()) {
                     return 0;
                  }
                  return asc
                          ? one.isScheduleAlerts() ? 1 : -1
                          : one.isScheduleAlerts() ? -1 : 1;
               } else {
                  return 0;
               }
            default:
               return 0;
         }
      } // EO compare(StudyUser one StudyUser two)

   } // EO StudyUser Comparator inner class
}
