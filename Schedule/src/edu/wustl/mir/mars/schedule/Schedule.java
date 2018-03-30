
package edu.wustl.mir.mars.schedule;

import edu.wustl.mir.mars.db.Request;
import edu.wustl.mir.mars.db.User;
import edu.wustl.mir.mars.hibernate.HibernateUtil;
import edu.wustl.mir.mars.util.Email;
import edu.wustl.mir.mars.util.Util;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
/**
 * Generates scheduled exams list for studies in mars system
 * @author rmoult01
 */
public class Schedule {
   
   private static final String REQUEST_HQL =
      "from request r fetch all properties " +
      "where " +
      "r.study.active = true and " +
      "r.scheduleAlerts = true " +
      "order by r.study.id, r.lastName, r.firstName";

   private static final String RIS_SQL =
      "select " +
         "patient.pat_itn as 'itn'," +
         "patient.pt_med_rec_no as 'mri', " +
      // "patient.pt_ss_no as 'ss', " +
         "pat_name.pt_last as 'last', " +
         "pat_name.pt_first as 'first', " +
         "pat_name.pt_middle as 'middle', " +
         "pat_name.pt_birth_dtime as 'dob', " +
         "pat_name.pt_sex as 'sex', " +
         "visit_activity.hosp as 'hospital', " +
      // "schdtl.sch_dtime as 'scheduled', " +
         "schdtlhdr.start_dtime as 'examTime', " +
         "schdtl.multi_proc_itn as 'group' " +
         "from schdtlhdr, schdtl, visit_activity, patient, pat_name " +
         "where " +
         "schdtlhdr.start_dtime > GETDATE() and " +
         "schdtlhdr.acc_itn = schdtl.acc_itn and " +
         "schdtlhdr.acc_itn = visit_activity.acc_itn and " +
         "visit_activity.pat_itn = patient.pat_itn and " +
         "visit_activity.pat_itn = pat_name.pat_itn " +
         "order by patient.pat_itn, schdtl.multi_proc_itn";

   private static Logger syslog = null;

   public static void main(String[] args) throws Exception {

      HierarchicalINIConfiguration ini = null;

      CommandLineParser parser = new PosixParser();
      Options opts = new Options();
      String runDir, iniName;
      //------------------------------- parse command line arguments
      try {
         opts.addOption("h", "help", false, "help message and exit");
         opts.addOption("d", "directory", true,
                 "run directory (def: current dir");
         opts.addOption("c", "config", true, "config file name");
         CommandLine line = parser.parse(opts, args);
         if (line.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("command syntax: ", opts);
            return;
         }
         runDir = line.getOptionValue("directory",
                 java.lang.System.getProperty("user.dir"));
         iniName = line.getOptionValue("config", "Schedule");

      } catch (Exception e) {
         System.err.println("parameter error: " + e.getMessage());
         HelpFormatter formatter = new HelpFormatter();
         formatter.printHelp("command syntax: ", opts);
         throw new Exception("Invalid command line parameters");
      }

      try {
         //---------------------- Initialize utilities and Hibernate
         Util.initialize(runDir, iniName);
         HibernateUtil.Initialize(Util.getIni());
         syslog = Util.getSyslog();
         ini = Util.getIni();
      } catch (Exception e) {
         String em = "Util.Initialize: " + e.getMessage();
         syslog.error(em);
         throw new Exception(em);
      }

      //----------------------------------- Load request array
      Request[] requests = null;
      try {
         getNewSession();
         List<Request> rl = (List<Request>) session.createQuery(REQUEST_HQL).list();
         requests = rl.toArray(new Request[0]);
         discardSession();
      } catch (Exception e) {
         String em = "load requests: " + e.getMessage();
         syslog.error(em);
         throw new Exception(em);
      }
      //-- Create array of Exam lists to store exams we find for each request
      List<Exam>[] exams = (List<Exam>[]) Array.newInstance(new ArrayList<Exam>().getClass(), requests.length);
      for (int x = 0; x < exams.length; x++) exams[x] = new ArrayList<Exam>();

      //************************************************************************
      //************************************************************************
      //********** process RIS Query, build scheduled exam lists
      //************************************************************************
      //************************************************************************

      //------------------- Query Syngo RIS for schedule data
      ResultSet syngo = Util.dbQuery("Syngo", RIS_SQL,
              ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

      State state = State.LOOKING_FOR_PATIENT;
      int currentItn = -1;
      List<Integer> matchingRequests = new ArrayList<Integer>();
      Exam currentExam = null;
      boolean repeat = false;
      int count = 0;


      while (syngo.next()) {
         count++;
         if (count % 1000 == 0) Util.getSyslog().info("count = " + count);
         Exam s = new Exam(syngo);
         do {
            repeat = false;
            switch (state) {
               case LOOKING_FOR_PATIENT:
                  // Skip until we get a new patient itn
                  if (s.getItn() == currentItn) break;
                  currentItn = s.getItn();
                  // Build list of matching requests (if any)
                  matchingRequests = new ArrayList<Integer>();
                  for (int i = 0; i < requests.length; i++) {
                     int mc = 0;
                     if(isSameString(requests[i].getLastName(),s.getLast())) mc++;
                     if(isSameString(requests[i].getFirstName(),s.getFirst())) mc++;
                     if (isSameDay(requests[i].getDob(), s.getDob())) mc++;
                     if (isSameString(requests[i].getSex(),s.getSex()) &&
                         !s.getSex().equalsIgnoreCase("U")) mc++;
                     if (isSameString(requests[i].getMpi(),s.getMri()) || mc >= 3)
                        matchingRequests.add(i);
                  }
                  // if no matching requests, we look for next patient
                  if (matchingRequests.isEmpty()) break;
                  Util.getSyslog().info("found match " + s.getLast() + ", " + s.getFirst());
                  // This exam becomes current exam, and we look at exam
                  currentExam = s;
                  state = State.LOOKING_AT_EXAM;
                  break;
               case LOOKING_AT_EXAM:
                  // Same exam, store exam time if earlier
                  if (currentExam.isSameExam(s)) {
                     currentExam.earliestExamTime(s.getExamTime());
                     break;
                  }
                  // not same exam, store current exam in matching requests
                  for (Integer i : matchingRequests) exams[i].add(currentExam);
                  Util.getSyslog().info("added exam ");
                  // same person, this exam becomes current exam & we look at exams
                  if (currentExam.isSamePatient(s)) {
                     currentExam = s;
                     break;
                  }
                  // not same person we keep same exam, but look for patient
                  repeat = true;
                  state = State.LOOKING_FOR_PATIENT;
                  break;
               default:
                  String em = "invalid state " + state.toString();
                  syslog.error(em);
                  throw new Exception(em);
            }
         } while (repeat);  // EO look at exam loop (may do more than once for an exam)
      } // EO process ResultSet row (exam)
      syngo.close();
      Util.dbClose("Syngo");

      // Store last exam in relevant requests, if any
      for (Integer i : matchingRequests) exams[i].add(currentExam);


      //************************************************************************
      //************************************************************************
      //********** Generate report file (Excel)
      //************************************************************************
      //************************************************************************

      Set<String> coordinators = new HashSet<String>(); // Coordinators receiving alerts

      //---------------------------------------------- Create workbook
      Workbook wb = new XSSFWorkbook();
      CreationHelper createHelper = wb.getCreationHelper();

      String study = "";
      Sheet sheet = null;
      Row row = null;
      int currentRowNumber = 0;
      Cell cell = null;

      //-------------------------------------------------------- Styles

      Font f = wb.createFont();
      f.setFontName("Courier New");
      f.setFontHeightInPoints((short)10);
      f.setBoldweight(Font.BOLDWEIGHT_BOLD);
      CellStyle titleStyle = wb.createCellStyle();
      titleStyle.setFont(f);
      titleStyle.setAlignment(CellStyle.ALIGN_CENTER);
      titleStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

      f = wb.createFont();
      f.setFontName("Courier New");
      f.setFontHeightInPoints((short)10);
      f.setBoldweight(Font.BOLDWEIGHT_BOLD);
      CellStyle subTitleStyle = wb.createCellStyle();
      subTitleStyle.setFont(f);
      subTitleStyle.setAlignment(CellStyle.ALIGN_CENTER);
      subTitleStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

      f = wb.createFont();
      f.setFontName("Courier New");
      f.setFontHeightInPoints((short)10);
      f.setBoldweight(Font.BOLDWEIGHT_BOLD);
      CellStyle colHdrStyle = wb.createCellStyle();
      colHdrStyle.setFont(f);
      colHdrStyle.setAlignment(CellStyle.ALIGN_CENTER);
      colHdrStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

      String[] headers = {
         "MPI",
         "Name",
         "DOB",
         "Sex",
         "MPI",
         "Name",
         "DOB",
         "Sex",
         "Hosp",
         "Exam Date/Time"
      };
      //------------------------------ regular (text) data
      f = wb.createFont();
      f.setFontName("Courier New");
      f.setFontHeightInPoints((short)10);
      CellStyle dataStyle = wb.createCellStyle();
      dataStyle.setFont(f);
      dataStyle.setAlignment(CellStyle.ALIGN_LEFT);
      dataStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

      //------------------------------ centered text data
      f = wb.createFont();
      f.setFontName("Courier New");
      f.setFontHeightInPoints((short)10);
      CellStyle cntrStyle = wb.createCellStyle();
      dataStyle.setFont(f);
      dataStyle.setAlignment(CellStyle.ALIGN_LEFT);
      dataStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);

      //--------------------------------------------------- DOB
      f = wb.createFont();
      f.setFontName("Courier New");
      f.setFontHeightInPoints((short)10);
      CellStyle dobStyle = wb.createCellStyle();
      dobStyle.setFont(f);
      dobStyle.setAlignment(CellStyle.ALIGN_CENTER);
      dobStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
      dobStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yy"));

      //--------------------------------------------------- Exam Date time
      f = wb.createFont();
      f.setFontName("Courier New");
      f.setFontHeightInPoints((short)12);
      CellStyle examStyle = wb.createCellStyle();
      examStyle.setFont(f);
      examStyle.setAlignment(CellStyle.ALIGN_CENTER);
      examStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
      examStyle.setDataFormat(createHelper.createDataFormat().getFormat("m/d/yy h:mm"));


      for (int r = 0; r < requests.length; r++) {
         Request request = requests[r];
         String str = request.getStudy().getId();
         //----------------------------- new study break processing
         if (!str.equalsIgnoreCase(study)) {
            study = str;
            //--- first, adjust column widths on previous sheet
            if (sheet != null)
               for (int c = 0; c < 10; c++) sheet.autoSizeColumn(c);

            //--- Add users set to get alerts for this study to coordinators 
            String q = "select u from user u join u.studyUsers su "
                    + "where su.study.dba = :dba and "
                    + "su.receiveAlerts = true";
            getNewSession();
            List<User> uga = (List<User>) session.createQuery(q)
                 .setInteger("dba", request.getStudy().getDba())
                 .list();
            for (User u : uga) coordinators.add(u.getEmail());
            discardSession();

            //------------------------- Create new sheet for each study
            str = WorkbookUtil.createSafeSheetName(str);
            sheet = wb.createSheet(str);
            //-------------------------------------------- Title, row 0
            row = sheet.createRow(0);
            cell = row.createCell(0);
            cell.setCellValue("Schedule Requests / Events for: " + study);
            cell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0,0,0,9));
            //------------------------------ Subtitles, row 1
            row = sheet.createRow(1);
            cell = row.createCell(0);
            cell.setCellValue("Request Information");
            cell.setCellStyle(subTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1,1,0,3));

            cell = row.createCell(4);
            cell.setCellValue("Scheduled Exam Information");
            cell.setCellStyle(subTitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1,1,4,9));
            //-------------------------- column headers, row 3
            row = sheet.createRow(2);
            for (int i = 0; i < headers.length; i++) {
               cell = row.createCell(i);
               cell.setCellValue(headers[i]);
               cell.setCellStyle(colHdrStyle);
            }
            currentRowNumber = 2;
         } // EO create new sheet for each study

         //---------------------------- Create new row for this request
         currentRowNumber++;
         row = sheet.createRow(currentRowNumber);
         //--------------------------------------- Load Request data
         cell = row.createCell(0);
         cell.setCellValue(request.getMpi());
         cell.setCellStyle(cntrStyle);

         cell = row.createCell(1);
         cell.setCellValue(request.getLastName() + ", " + request.getFirstName());
         cell.setCellStyle(dataStyle);

         cell = row.createCell(2);
         if (request.getDob() != null) cell.setCellValue(request.getDob());
         cell.setCellStyle(dobStyle);

         cell = row.createCell(3);
         cell.setCellValue(request.getSex());
         cell.setCellStyle(cntrStyle);

         //------------------- Process Scheduled events for this Request
         boolean firstExam = true;
         for (Exam exam : exams[r]) {
            // For additional exam rows, add in empty leading cells
            if (!firstExam) {
               currentRowNumber++;
               row = sheet.createRow(currentRowNumber);
               for (int c = 0; c < 4; c++) {
                  cell = row.createCell(c);
                  cell.setCellValue("");
                  cell.setCellStyle(dataStyle);
               }
            }
            firstExam = false;

            //------------------------------ Add Scheduled event data

            cell = row.createCell(4);
            cell.setCellValue(exam.getMri());
            cell.setCellStyle(cntrStyle);

            cell = row.createCell(5);
            cell.setCellValue(exam.getLast() + ", " + exam.getFirst() + " " + exam.getMiddle());
            cell.setCellStyle(dataStyle);

            cell = row.createCell(6);
            cell.setCellValue(exam.getDob());
            cell.setCellStyle(dobStyle);

            cell = row.createCell(7);
            cell.setCellValue(exam.getSex());
            cell.setCellStyle(cntrStyle);

            cell = row.createCell(8);
            cell.setCellValue(exam.getHospital());
            cell.setCellStyle(cntrStyle);

            cell = row.createCell(9);
            cell.setCellValue(exam.getExamTime());
            cell.setCellStyle(examStyle);

         } // EO process exams for this request

      } // EO process requests

      // save workbook to disc
      SimpleDateFormat df = new SimpleDateFormat("yyyy_MM_dd");

      String pfn = Util.getRunDirectoryPath() + "/reports/marsSched_" +
              df.format(new java.util.Date()) + ".xlsx";
      FileOutputStream fileOut = new FileOutputStream(pfn);
      wb.write(fileOut);
      fileOut.close();

      //----------------------- Email to appropriate people
      Iterator<String> emails = (Iterator<String>) Util.getIni().getKeys("Emails");
      while(emails.hasNext()) {
         String emAddress = emails.next();
         emAddress = StringUtils.replace(emAddress, "..", ".");
         emAddress = StringUtils.substringAfter(emAddress, "Emails.");
         coordinators.add(emAddress);
      }
      List<String> files = new ArrayList<String>();
      files.add(pfn);

      Email email = new Email();
      email.sendToEmails(coordinators, "Mars Scheduled Exams report", "See attached file", files);

   } // EO main method

   private enum State {
      LOOKING_FOR_PATIENT,
      LOOKING_AT_EXAM
   }
   /**
    * Are the two strings a significant match? Returns true if strings a & b are
    * not null, empty, or just whitespace and are equal. Ignores case.
    * @param a string to compare
    * @param b string to compare
    * @return boolean true if significant match, false otherwise.
    */
   private static boolean isSameString(String a, String b) {
      String c = StringUtils.trimToEmpty(a);
      String d = StringUtils.trimToEmpty(b);
      return c.length() > 0 && c.equalsIgnoreCase(d);
   }
   /**
    * Are the two dates the same calendar day? Returns true if a & b are not
    * null and represent the same calendar day.
    * @param a
    * @param b
    * @return
    */
   private static boolean isSameDay(Date a, Date b) {
      if (a == null || b == null) return false;
      return DateUtils.isSameDay(a, b);
   }

   /****************************************************************************
    *********************************** Hibernate
    ***************************************************************************/

   protected static SessionFactory sf = null;
   protected static Session session = null;
   protected static Transaction transaction = null;

   protected static void getNewSession() {
      if (sf == null) sf = HibernateUtil.getSessionFactory();
      discardSession();
      session = sf.openSession();
      transaction = session.beginTransaction();
   }

   protected static void discardSession() {
      if (transaction != null) {
         if (transaction.isActive()) transaction.rollback();
         transaction = null;
      }
      if (session != null) {
         if(session.isOpen()) session.close();
         session = null;
      }
   }

   /**
    * Commit current transaction. It is expected that a valid session and a
    * valid transaction exist.
    * @param newTransaction boolean. If true, starts a new transaction after
    * committing this one.
    * @throws Exception on error
    */
   protected static void commit(boolean newTransaction) throws Exception {
      if (session == null || !session.isOpen())
         throw new Exception("Commit: No active hibernate session");
      if (transaction == null || !transaction.isActive())
         throw new Exception("Commit: No active hibernate transaction");
      try {
         transaction.commit();
         transaction = null;
         if (newTransaction) transaction = session.beginTransaction();
      } catch (HibernateException e) {
         throw new Exception("Commit: Hibernate Exception: " + e.getMessage());
      }
   }
   protected static void commit() throws Exception { commit(false); }

   /**
    * Roll back current transaction. It is expected that a valid session and a
    * valid transaction exist.
    * @param newTransaction boolean. If true, starts a new transaction after
    * rolling back this one.
    * @throws Exception on error
    */
   protected static void rollback(boolean newTransaction) throws Exception{
      if (session == null || !session.isOpen())
         throw new Exception("Commit: No active hibernate session");
      if (transaction == null || !transaction.isActive())
         throw new Exception("Commit: No active hibernate transaction");
      try {
         transaction.rollback();
         transaction = null;
         if (newTransaction) transaction = session.beginTransaction();
      } catch (HibernateException e) {
         throw new Exception("Commit: Hibernate Exception: " + e.getMessage());
      }
   }
   protected static void rollback() throws Exception { rollback(false); }
   /**
    * Gets a new transaction.  If no session exists, it is created.
    * @param commitOldTransaction If an active transaction exists, and this is
    * true, it will be committed; If false it will be rolled back.
    * @return
    */
   protected static void newTransaction(boolean commitOldTransaction) throws Exception{
      if (session == null || !session.isOpen()) session = sf.openSession();
      if (transaction != null && transaction.isActive())
         if (commitOldTransaction) commit(false);
         else rollback(false);
      try {
         transaction = session.beginTransaction();
      } catch (HibernateException e) {
         throw new Exception("newTransaction: Hibernate Exception: " +
                 e.getMessage());
      }
   }

   protected static void newTransaction() throws Exception {
      newTransaction(false);
   }
} // EO Schedule class