
package edu.wustl.mir.mars.db;

import java.io.Serializable;
import java.util.Comparator;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 *
 * @author rmoult01
 */
@Entity(name="studyUser")
public class StudyUser implements Serializable {

   static final long serialVersionUID = 1L;
   static final String[] roles = {"View", "Maintain", "Admin" };
   public static final int ADMIN_ROLE = 2;

   private int dba = 0;
   private Study study;
   private User user;
   private int userRole = 1;
   private boolean receiveAlerts = false;

   private boolean selected = false;

   public StudyUser() { }
   public StudyUser(int dba, Study study, User user, int userRole, boolean receiveAlerts) {
      this.dba = dba;
      this.study = study;
      this.user = user;
      this.userRole = userRole;
      this.receiveAlerts = receiveAlerts;
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

   @ManyToOne
   @JoinColumn(name="userDba")
   public User getUser() {
      return user;
   }

   public void setUser(User user) {
      this.user = user;
   }


   public int getUserRole() {
      return userRole;
   }

   @Transient
   public String getUserRoleName() {
      return roles[userRole];
   }

   public void setUserRole(int userRole) {
      this.userRole = userRole;
   }

   public boolean isReceiveAlerts() {
      return receiveAlerts;
   }

   public void setReceiveAlerts(boolean receiveAlerts) {
      this.receiveAlerts = receiveAlerts;
   }

   @Transient
   public boolean isSelected() {
      return selected;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   public String toString() {
      return "[StudyUser: dba=" + dba + " Study=" + study.getId() + 
              " User=" + user.getId() + " userRole=" + userRole +
              " receiveAlerts=" + receiveAlerts + "]";
   }

   public static class StudyUserComparator implements Comparator<StudyUser> {

      private enum Comparators { USER, STUDY, STUDYUSER }
      private enum Comparisons {
         USERID         ("userId",        "id",            Comparators.USER),
         USERLASTNAME   ("userLastName",  "lastName",      Comparators.USER),
         USERFIRSTNAME  ("userFirstName", "firstName",     Comparators.USER),
         USERNAME       ("userName",      "name",          Comparators.USER),
         STUDYID        ("studyId",       "id",            Comparators.STUDY),
         USERROLE       ("userRole",      "userRole",      Comparators.STUDYUSER),
         ALERTS         ("receiveAlerts", "receiveAlerts", Comparators.STUDYUSER);

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
      private Comparator<User> userComparator = null;
      private Comparator<Study> studyComparator = null;

      public StudyUserComparator(String columnName, boolean ascending) {
         asc = ascending;
         comparison = Comparisons.getComparisonForExternalColumnName(columnName);
         if (comparison != null) {
            col = comparison.internalColumnName;
            switch (comparison.comparatorToUse) {
               case USER:
                  userComparator = new User.UserComparator(col, asc);
                  break;
               case STUDY:
                  studyComparator = new Study.StudyComparator(col, asc);
                  break;
               default:
            }
         }
      } // EO StudyUserComparator(String columnName, boolean ascending)

      @Override
      public int compare(StudyUser one, StudyUser two) {
         if (col == null) return 0;
         switch(comparison.comparatorToUse) {
            case USER:
               return userComparator.compare(one.getUser(), two.getUser());
            case STUDY:
               return studyComparator.compare(one.getStudy(), two.getStudy());
            case STUDYUSER:
               if (col.equalsIgnoreCase(Comparisons.USERROLE.internalColumnName)) {
                  return asc ?
                     two.getUserRole() - one.getUserRole() :
                     one.getUserRole() - two.getUserRole() ;
               } else if (col.equalsIgnoreCase(Comparisons.ALERTS.internalColumnName)) {
                  if (one.receiveAlerts == two.receiveAlerts) return 0;
                  return asc ?
                     one.receiveAlerts ?  1 : -1 :
                     one.receiveAlerts ? -1 :  1 ;
               } else return 0;
            default:
               return 0;
         }
      } // EO compare(StudyUser one StudyUser two)

   } // EO StudyUser Comparator inner class
  

} // EO StudyUser class