package edu.wustl.mir.mars.schedule;

import edu.wustl.mir.mars.util.Util;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author rmoult01
 */
public class Exam {

   private int itn;
   private String mri;
   private String last;
   private String first;
   private String middle;
   private Date dob;
   private String sex;
   private String hospital;
   private Timestamp examTime;
   private int group;

   public Exam(ResultSet r) throws Exception {
      try {
         itn = r.getInt("itn");
         mri = StringUtils.stripStart(StringUtils.trimToEmpty(r.getString("mri")), "0");
         last = StringUtils.trimToEmpty(r.getString("last"));
         first = StringUtils.trimToEmpty(r.getString("first"));
         middle = StringUtils.trimToEmpty(r.getString("middle"));
         dob = r.getDate("dob");
         sex = r.getString("sex");
         if (sex == null) {
            sex = "U";
         }
         hospital = StringUtils.trimToEmpty(r.getString("hospital"));
         examTime = r.getTimestamp("examTime");
         group = r.getInt("group");
      } catch (Exception e) {
         String em = "Exam constructor exception " + e.getMessage();
         Util.getSyslog().warn(em);
         throw new Exception(em);
      }
   }

   public boolean isSameExam(Exam other) {
      return this.itn == other.itn && this.group == other.group;
   }
   public boolean isSamePatient(Exam other) {
      return this.itn == other.itn;
   }

   public Date getDob() {
      return dob;
   }

   public void setDob(Date dob) {
      this.dob = dob;
   }

   public Timestamp getExamTime() {
      return examTime;
   }

   public void setExamTime(Timestamp examTime) {
      this.examTime = examTime;
   }

   public void earliestExamTime(Timestamp examTime) {
      if (examTime.before(this.examTime)) {
         this.examTime = examTime;
      }
   }

   public String getFirst() {
      return first;
   }

   public void setFirst(String first) {
      this.first = first;
   }

   public int getGroup() {
      return group;
   }

   public void setGroup(int group) {
      this.group = group;
   }

   public String getHospital() {
      return hospital;
   }

   public void setHospital(String hospital) {
      this.hospital = hospital;
   }

   public int getItn() {
      return itn;
   }

   public void setItn(int itn) {
      this.itn = itn;
   }

   public String getLast() {
      return last;
   }

   public void setLast(String last) {
      this.last = last;
   }

   public String getMiddle() {
      return middle;
   }

   public void setMiddle(String middle) {
      this.middle = middle;
   }

   public String getMri() {
      return mri;
   }

   public void setMri(String mri) {
      this.mri = mri;
   }

   public String getSex() {
      return sex;
   }

   public void setSex(String sex) {
      this.sex = sex;
   }
}
