
package edu.wustl.mir.syngo.db;

import edu.wustl.mir.mars.db.Request;
import java.sql.ResultSet;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;
import edu.wustl.mir.mars.util.Util;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import org.apache.commons.codec.language.Soundex;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Encapsulates Patient identifying information taken from the pat_name and
 * Patient tables in the syngo DB. Property names are taken from the Syngo RIS
 * column name, camelcased. Not all columns in the tables are represented.
 * @author rmoult01
 */
public class Patient {

   private static Soundex sndx = new Soundex();

   //--------------- Patient internal key value (both tables)
   private String patItn = "";
   //--------------- Properties from the pat_name table
   private String lastSndx = "";
   private String firstSndx = "";
   private String ptSex = "N";
   private Date   ptBirthDtime = null;
   private String ptLast = "";
   private String ptFirst = "";
   private String ptMiddle = "";
   //--------------- Properties from the Patient table
   private String ptMedRecNo = "";
   private String ptSsNo = "";

   //-------------------------------------- Transient properties
   private boolean selected = false;

   public Patient() {}

   public String getFirstSndx() {
      return firstSndx;
   }

   public void setFirstSndx(String firstSndx) {
      this.firstSndx = firstSndx;
   }

   public String getLastSndx() {
      return lastSndx;
   }

   public void setLastSndx(String lastSndx) {
      this.lastSndx = lastSndx;
   }

   public String getPatItn() {
      return patItn;
   }

   public void setPatItn(String patItn) {
      this.patItn = patItn;
   }

   public String getPtMiddle() {
      return ptMiddle;
   }

   public void setPtMiddle(String prMiddle) {
      this.ptMiddle = prMiddle;
   }

   public Date getPtBirthDtime() {
      return ptBirthDtime;
   }

   public void setPtBirthDtime(Date ptBirthDtime) {
      this.ptBirthDtime = ptBirthDtime;
   }

   public String getPtFirst() {
      return ptFirst;
   }

   public void setPtFirst(String ptFirst) {
      this.ptFirst = ptFirst;
   }

   public String getPtLast() {
      return ptLast;
   }

   public void setPtLast(String ptLast) {
      this.ptLast = ptLast;
   }

   public String getPtMedRecNo() {
      return ptMedRecNo;
   }

   public void setPtMedRecNo(String ptMedRecNo) {
      this.ptMedRecNo = ptMedRecNo;
   }

   public String getPtSex() {
      return ptSex;
   }

   public void setPtSex(String ptSex) {
      this.ptSex = ptSex;
   }

   public String getPtSsNo() {
      return ptSsNo;
   }

   public void setPtSsNo(String ptSsNo) {
      this.ptSsNo = ptSsNo;
   }

   public boolean isSelected() {
      return selected;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   @Override
   public String toString() {
      SimpleDateFormat df = new SimpleDateFormat("MM-dd-yyyy");
      String
      rtVal = "[Patient: patItn="    + patItn +
                      " lastSndx="  + lastSndx +
                      " firstSndx=" + firstSndx +
                      " ptLast="    + ptLast +
                      " ptFirst="   + ptFirst +
                      " ptMiddle="  + ptMiddle +
                      " ptBirthDtime" + df.format(ptBirthDtime) +
                      " ptSex="     + ptSex +
                      " ptMedRecNo="+ ptMedRecNo;
      if (ptSsNo != null && ptSsNo.length() > 0) {
         rtVal = rtVal + " ptSsNo=" + Util.formatSSN(ptSsNo);
      }
      rtVal += "]";
      return rtVal;
   }

   //*********************** Static factory methods

   /**
    * Factory method which generates a Patient[] corresponding to the rows in
    * the passed JDBC ResultSet. The ResultSet must:<ul>
    * <li/>Represent a join of the Syngo RIS patient and pat_name tables on
    * their pat_itn columns, or an equivalent view.
    * <li/>Include the pt_itn, last_sndx, first_sndx, pt_sex, pt_birth_dtime,
    * pt_last, pt_first, pt_middle, pt_med_rec_no_ext, and pt_ss_no columns
    * using these column names. "select * from patient, pat_name" meets this
    * requirement.</ul>
    * One Patient object will be included in the outputlist for each distinct
    * patient (pat_itn) represented in the ResultSet.
    * @param rs {@link java.sql.ResultSet ResultSet}
    * @return List of unique Patients (that is, no duplicate pat_itn numbers)
    * @throws Exception on sql error. Exceptions are logged to syslog before
    * being thrown.
    */
   public static Patient[] getPatients(ResultSet rs) throws Exception {
      Map<String, Patient> patMap = new HashMap<String, Patient>();
      String itn = "", col = "", str = "";
      Patient p = null;

      try {
         rs.beforeFirst();
         while (rs.next()) {

            itn = "";
            col = "pat_itn";
            itn = new Long(rs.getLong(col)).toString();
            if (patMap.containsKey(itn)) continue;
            p = new Patient();
            p.setPatItn(itn);

            col = "last_sndx";
            p.setLastSndx(StringUtils.stripToEmpty(rs.getString(col)));
            col = "first_sndx";
            p.setFirstSndx(StringUtils.stripToEmpty(rs.getString(col)));
            col = "pt_sex";
            p.setPtSex(StringUtils.stripToEmpty(rs.getString(col)));
            col = "pt_birth_dtime";
            p.setPtBirthDtime(rs.getDate(col));
            col = "pt_last";
            p.setPtLast(StringUtils.stripToEmpty(rs.getString(col)));
            col = "pt_first";
            p.setPtFirst(StringUtils.stripToEmpty(rs.getString(col)));
            col = "pt_middle";
            p.setPtMiddle(StringUtils.stripToEmpty(rs.getString(col)));
            col = "pt_med_rec_no_ext";
            p.setPtMedRecNo(StringUtils.stripToEmpty(rs.getString(col)));
            col = "pt_ss_no";
            p.setPtSsNo(StringUtils.stripToEmpty(rs.getString(col)));

            patMap.put(itn, p);
         }
         return (Patient[]) patMap.values().toArray();

      } catch (Exception e) {
         String em = "getPatients itn=" + itn + " col=" + col + " :" + e.getMessage();
         Util.getSyslog().warn(em);
         throw new Exception(em);
      }
   } // getPatients

      String query = "select * from patient, pat_name where patient.pat_itn = pat_name.pat_itn";

      /**
       * Factory method which generates a Patient[] corresponding to patients in
       * the syngo RIS DB which might be the patient represented by the passed
       * {@link ecu.wustl.mir.mars.db.Request Request}
       * object. In order to avoid queries which place too great a load on the
       * Syngo RIS DB, minimum data is required in the Request:<ul>
       * <li/>A non-blank mpi to query on mpi.
       * <li/>Both last and first name to query on name. Query will be based on
       * the Soundex of the name entries.
       * <li/>Last name and DOB to query on last name (Soundex) and DOB.
       * <li/>If Sex is present and not U (Unknown), sex will be used to restrict
       * the name and name-DOB queries. It will not restrict the mpi query.</ul>
       * If none of the first three scenarios is satisfied, the method will
       * throw an exception.
       * @param r Request object for patient to be identified.
       * @return List\<Patient\> of possible matches. Empty if no matchs found.
       * @throws Exception on insufficient data to query or sql error. Errors
       * are logged to syslog before being thrown.
       */
      public static Patient[] getPatients(Request r) throws Exception {
      String mpi = StringUtils.stripToEmpty(r.getMpi());
      String last = StringUtils.stripToEmpty(r.getLastName());
      String first = StringUtils.stripToEmpty(r.getFirstName());
      Date dob = r.getDob();
      String sex = StringUtils.stripToEmpty(r.getSex());
      StringBuilder query = new StringBuilder("");
      String qh = "SELECT * FROM patient, pat_name WHERE patient.pat_itn = pat_name.pat_itn ";
      
      try {
         //---------------------------------- Validate enough data to query
         if (mpi.isEmpty() &&
             (last.isEmpty() || first.isEmpty()) &&
             (last.isEmpty() || dob == null))
            throw new Exception ("Insufficient data to query RIS DB");
         
         //----------------------------------------------------- mpi query
         if (!mpi.isEmpty()) {
            query.append(qh).append("AND patient.pt_med_rec_no_ext == ")
                    .append(StringEscapeUtils.escapeSql(mpi));
         }
         
         //---------------------------------------------------- name query
         if (!last.isEmpty() && !first.isEmpty()) {
            if (query.length() > 0) query.append(" UNION ");
            query.append(qh)
               .append("AND pat_name.last_sndx = '")
               .append(sndx.soundex(last))
               .append("' ")
               .append("AND pat_name.first_sndx = '")
               .append(sndx.soundex(first))
               .append("'");
         }

         //---------------------------------------- last name - dob query
         if (!last.isEmpty() && dob != null) {
            if (query.length() > 0) query.append(" UNION ");
            query.append(qh)
               .append("AND pat_name.last_sndx = '")
               .append(sndx.soundex(last))
               .append("' ")
               .append("AND DATE(pat_name.pt_birth_dtime) = '")
               .append(dob.toString())
               .append("'");
         }
         ResultSet res = Util.dbQuery("SyngoRIS", query);
         return getPatients(res);
         
      } catch (Exception e) {
         String em = "getPatients(Request " + e.getMessage();
         Util.getSyslog().warn(em);
         throw new Exception(em);
      } 
   }// EO getPatients

   public static class PatientComparator implements Comparator<Patient> {
      
      private boolean asc;
      private String col = null;
      private Comparator<Patient> patientComparator = null;

      public PatientComparator (String columnName, boolean ascending) {
         col = columnName;
         asc = ascending;
      }

      @Override
      public int compare(Patient one, Patient two) {
         if (col == null) col = "null";
         try {

         if (col.equalsIgnoreCase("patItn")) {
            int o = Integer.parseInt(one.getPatItn());
            int t = Integer.parseInt(two.getPatItn());
            return asc ? t - o : o - t;
         } else if(col.equalsIgnoreCase("name")) {
            if (one.getPtLast().equalsIgnoreCase(two.getPtLast())) {
               return asc
                       ? one.getPtFirst().compareToIgnoreCase(two.getPtFirst())
                       : two.getPtFirst().compareToIgnoreCase(one.getPtFirst());
            } else
               return asc
                       ? one.getPtLast().compareToIgnoreCase(two.getPtLast())
                       : two.getPtLast().compareToIgnoreCase(one.getPtLast());
         } else if (col.equalsIgnoreCase("ptSex")) {
            return asc
                    ? one.getPtSex().compareTo(two.getPtSex())
                    : two.getPtSex().compareTo(one.getPtSex());
         } else if (col.equalsIgnoreCase("ptBirthDtime")) {
            return asc
                    ? one.getPtBirthDtime().compareTo(two.getPtBirthDtime())
                    : two.getPtBirthDtime().compareTo(one.getPtBirthDtime());
         } else if (col.equalsIgnoreCase("ptMedRecNo")) {
            int o = Integer.parseInt(one.getPtMedRecNo());
            int t = Integer.parseInt(two.getPtMedRecNo());
            return asc ? t - o : o - t;
         } else if (col.equalsIgnoreCase("ptSsNo")) {
            int o = Integer.parseInt(one.getPtSsNo());
            int t = Integer.parseInt(two.getPtSsNo());
            return asc ? t - o : o - t;
         }
         throw new Exception("invalid column name");

         } catch (Exception e) {
            StringBuilder em = new StringBuilder("patient sort comparison error ");
            em.append(col).append(" asc=").append(asc).append(e.getMessage());
            Util.getSyslog().warn(em);
         }
         return 0;
      }
   }
} // EO Patient class
