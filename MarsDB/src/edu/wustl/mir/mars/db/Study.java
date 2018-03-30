
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
 * study table entity bean
 */
@Entity(name="study")
public class Study implements Serializable{

   static final long serialVersionUID = 1L;

   //-------------------------------------------- persisted properties
   private int dba = 0;
   private String id = "";
   private String description = "";
   private boolean active = true;
   private List<StudyUser> studyUsers = null;

   //-------------------------------------------- Transient properties
   private boolean selected = false;

   public Study() {   }

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

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   public boolean isActive() {
      return active;
   }

   public void setActive(boolean active) {
      this.active = active;
   }


   @Transient
   public boolean isSelected() {
      return selected;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   @OneToMany(mappedBy = "study", targetEntity = StudyUser.class,
      fetch = FetchType.EAGER, orphanRemoval = true)
   public List<StudyUser> getStudyUsers() {
      return studyUsers;
   }

   public void setStudyUsers(List<StudyUser> studyUsers) {
      this.studyUsers = studyUsers;
   }

   public String toString() {
      return "[Study: dba=" + dba + " id=" + id + " desc=" + description + 
              " active=" + active + "]";
   }

   public static class StudyComparator implements Comparator<Study> {

      private String col;
      private boolean asc;
      public StudyComparator(String columnName, boolean ascending) {
         col = columnName;
         asc = ascending;
      }

      @Override
      public int compare(Study one, Study two) {
         if (col == null) return 0;
         if (col.equalsIgnoreCase("id")) {
            return asc ?
               one.getId().compareToIgnoreCase(two.getId()) :
               two.getId().compareToIgnoreCase(one.getId()) ;
         } else return 0;
      }

   } // EO User Comparator inner class

  
}
