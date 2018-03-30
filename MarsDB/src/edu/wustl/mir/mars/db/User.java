package edu.wustl.mir.mars.db;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

/**
 * user table entity bean
 */
@Entity(name="user")
public class User implements Serializable {

   static final long serialVersionUID = 1L;

   //-------------------------------------- Persisted properties
   private int dba = 0;
   private String id = "";
   private String firstName = "";
   private String lastName = "";
   private String pw = "";
   private boolean admin = false;
   private String email = "";
   private List<StudyUser> studyUsers = null;

   //-------------------------------------- Transient properties
   private boolean selected = false;

   public User() { }

   @Id
   @GeneratedValue(strategy=GenerationType.IDENTITY)
   public int getDba() {
      return dba;
   }

   public void setDba(int dba) {
      this.dba = dba;
   }
   @Column(unique=true)
   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
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

   public String getPw() {
      return pw;
   }

   public void setPw(String pw) {
      this.pw = pw;
   }

   public boolean isAdmin() {
      return admin;
   }

   public void setAdmin(boolean admin) {
      this.admin = admin;
   }

   public String getEmail() {
      return email;
   }

   public void setEmail(String email) {
      this.email = email;
   }
   @Transient
   public boolean isSelected() {
      return selected;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   @OneToMany(mappedBy = "user",
      targetEntity = StudyUser.class,
      fetch = FetchType.EAGER, orphanRemoval = true)
   public List<StudyUser> getStudyUsers() {
      return studyUsers;
   }

   public void setStudyUsers(List<StudyUser> studyUsers) {
      this.studyUsers = studyUsers;
   }
   
   public String toString() {
      return "[User: dba=" + dba +  " id=" + id + " firstName=" + firstName +
              " lastname=" + lastName + " pw=" + pw + " admin=" + admin + 
              " email=" + email + "]";
   }

   public static class UserComparator implements Comparator<User> {

      private String col;
      private boolean asc;
      public UserComparator(String columnName, boolean ascending) {
         col = columnName;
         asc = ascending;
      }

      @Override
      public int compare(User one, User two) {
         if (col == null) return 0;
         if (col.equalsIgnoreCase("id")) {
            return asc ?
               one.getId().compareToIgnoreCase(two.getId()) :
               two.getId().compareToIgnoreCase(one.getId()) ;
         } else if (col.equalsIgnoreCase("lastName")) {
            return asc ?
               one.getLastName().compareToIgnoreCase(two.getLastName()) :
               two.getLastName().compareToIgnoreCase(one.getLastName()) ;
         } else if (col.equalsIgnoreCase("firstName")) {
            return asc ?
               one.getFirstName().compareToIgnoreCase(two.getFirstName()) :
               two.getFirstName().compareToIgnoreCase(one.getFirstName()) ;
            // Sort last name, within last name first name
         } else if (col.equalsIgnoreCase("name")) {
            int t = asc ?
               one.getLastName().compareToIgnoreCase(two.getLastName()) :
               two.getLastName().compareToIgnoreCase(one.getLastName()) ;
            if (t != 0) return t;
            return asc ?
               one.getFirstName().compareToIgnoreCase(two.getFirstName()) :
               two.getFirstName().compareToIgnoreCase(one.getFirstName()) ;
         } else if (col.equalsIgnoreCase("email")) {
            return asc ?
               one.getEmail().compareToIgnoreCase(two.getEmail()) :
               two.getEmail().compareToIgnoreCase(one.getEmail()) ;
         } else return 0;
      }

   } // EO User Comparator inner class

} // EO User class
