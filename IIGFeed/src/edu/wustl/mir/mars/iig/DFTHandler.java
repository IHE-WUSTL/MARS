package edu.wustl.mir.mars.iig;

import ca.uhn.hl7v2.app.Application;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.ExtraComponents;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v24.datatype.CX;
import ca.uhn.hl7v2.model.v24.datatype.XPN;
import ca.uhn.hl7v2.model.v24.message.ACK;
import ca.uhn.hl7v2.model.v24.message.DFT_P03;
import ca.uhn.hl7v2.model.v24.segment.FT1;
import ca.uhn.hl7v2.model.v24.segment.MSA;
import ca.uhn.hl7v2.model.v24.segment.MSH;
import ca.uhn.hl7v2.model.v24.segment.PID;
import ca.uhn.hl7v2.util.DeepCopy;
import ca.uhn.hl7v2.util.Terser;
import edu.wustl.mir.mars.db.Alert;
import edu.wustl.mir.mars.db.Request;
import edu.wustl.mir.mars.db.User;
import edu.wustl.mir.mars.hibernate.HibernateUtil;
import edu.wustl.mir.mars.util.Email;
import edu.wustl.mir.mars.util.Util;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Simple Server handler class for DFT^P03 message from IIG.
 * @author rmoult01
 */
public class DFTHandler implements Application {

   Logger syslog;

   private static final String SUBJECT = "MARS IIG Alert";
   private static final String BODY =
           "<html><body>" +
           "One or more MARS Alerts have been generated<br/>" +
           "for which you are assigned to receive alerts.<br/>" +
           "Log in to the MARS system to review your alerts.<p/>" +
           "Click <a href=\"http://mars-irat.wucon.wustl.edu:8082/Mars/index.jsf\">here</a> " +
           "to access the MARS system." +
           "</body></html>";

   public DFTHandler() {
      syslog = Util.getSyslog();
   }

   public boolean canProcess(Message msg) {
      return true;
   }

   public Message processMessage(Message msg)  {
      syslog.trace("DFTHandler.processMessage(msg)");

       Util.getMsglog().info("Received from IIG:" + HL7V2.format(msg));

      List<Alert> alerts = new ArrayList<Alert>();

      //----------------------------------- fields we are looking for
      String mshSite = null;
      String pidMpi = null;
      String pidLname = null;
      String pidFname = null;
      java.sql.Date   pidDob = null;
      String pidSex = null;
      String ft1Acc = null;
      java.sql.Date ft1Comp = null;
      String ft1Descr = null;
      String ft1Dsc = null;

      //---------------------------------------- messages, segments & components
      DFT_P03 dft = null;
      MSH dftmsh = null;
      PID dftpid = null;
      FT1 dftft1 = null;
      XPN xpn = null;
      

      //-------------------------- Cast message and pull segments we need
      dft = (DFT_P03) msg;
      dftmsh = dft.getMSH();

      ACK ack = (ACK) DFTHandler.makeACK(dftmsh);

      try {
      
      dftpid = dft.getPID();
      dftft1 = dft.getFINANCIAL().getFT1();
      
      //----------------------------------------------------- Fields
      mshSite = dftmsh.getMsh4_SendingFacility().getHd1_NamespaceID().getValue();
      
      /*
       * SP puts the pid in PID.2
       * BJH, SLCH, and WS use the Enterprise ID, which should be in one of the 
       * repetitions of PID.3.
       */
      if (StringUtils.equalsIgnoreCase(mshSite, "SP")) {
         pidMpi = dftpid.getPid2_PatientID().getCx1_ID().getValue();
      } else {
         int reps = dftpid.getPid3_PatientIdentifierListReps();
         for (int i = 0; i < reps; i++) {
            CX cx = dftpid.getPid3_PatientIdentifierList(i);
            if (cx.getCx5_IdentifierTypeCode().getValue().equalsIgnoreCase("EE"))
               pidMpi = cx.getCx1_ID().getValue();
         }
      }
      
      //---------------- string leading zeroes
       while (pidMpi.length() > 1 && pidMpi.startsWith("0")) 
          pidMpi = pidMpi.substring(1);
      
      xpn = dftpid.getPid5_PatientName(0);
      pidLname = StringUtils.trimToEmpty(xpn.getXpn1_FamilyName().getFn1_Surname().getValue());
      pidFname = StringUtils.trimToEmpty(xpn.getXpn2_GivenName().getValue());

      try {
         Date dob = dftpid.getPid7_DateTimeOfBirth().getTs1_TimeOfAnEvent().getValueAsDate();
         pidDob = new java.sql.Date(dob.getTime());
      } catch (DataTypeException ex) {
         syslog.warn("Invalid DOB" + ex.getMessage());
      }

      pidSex = StringUtils.trimToEmpty(dftpid.getPid8_AdministrativeSex().getValue());

      ft1Acc = dftft1.getFt12_TransactionID().getValue();

      try {
         Date trn = dftft1.getFt14_TransactionDate().getTs1_TimeOfAnEvent().getValueAsDate();
         ft1Comp = new java.sql.Date(trn.getTime());
      } catch (DataTypeException ex) {
         syslog.warn("Invalid Comp date" + ex.getMessage());
      }

      if (StringUtils.equalsIgnoreCase(mshSite, "SP")) {
         ft1Dsc = StringUtils.trimToEmpty(dftft1.getFt18_TransactionDescription().getValue());
         ExtraComponents ec = dftft1.getFt18_TransactionDescription().getExtraComponents();
         ft1Descr = "";
         if (ec.numComponents() > 0) ft1Descr = ec.getComponent(0).getData().toString();
      } else {
         ft1Descr = StringUtils.trimToEmpty(dftft1.getFt18_TransactionDescription().getValue());
         ft1Dsc = StringUtils.trimToEmpty(dftft1.getFt125_ProcedureCode().getCe1_Identifier().getValue());
      }

       //------------------------------- query for requests matching event
      String query = "from request r where r.finalReportAlerts = true and " +
              "r.study.active = true and (r.mpi = :mpi " +
              "or r.lastName = :lname or r.firstName = :fname " +
              "or r.dob = :dob)";

      SessionFactory sf = HibernateUtil.getSessionFactory();
      Session session = sf.openSession();
      Transaction trans = session.beginTransaction();
      @SuppressWarnings("unchecked")
      List<Request> requests = session.createQuery(query)
              .setString("mpi", pidMpi)
              .setString("lname", pidLname)
              .setString("fname", pidFname)
              .setDate("dob", pidDob)
              .list();
      trans.rollback();
      if (requests.isEmpty()) {
         Util.getMsglog().info("No requests found");
         Util.getMsglog().debug("Returned: " + HL7V2.format(ack));
         return ack;
      }
      //-------------- examine requests looking for 'match'
      for (Request request : requests) {
         int count = 0;
         if (Util.significantlyEqual(request.getLastName(),pidLname)) count++;
         if (Util.significantlyEqual(request.getFirstName(),pidFname)) count++;
         if (request.getSex().equalsIgnoreCase(pidSex) && !pidSex.equalsIgnoreCase("U")) count++;
         if (Util.significantlyEqual(request.getDob(),pidDob)) count++;
         //-------------------- match on mpi or any three others
         if (!Util.significantlyEqual(request.getMpi(),pidMpi) && count < 3) continue;

         //--------------------------------- Generate Alert
         trans = session.beginTransaction();
         Alert alert = new Alert();
         alert.setFirstName(pidFname);
         alert.setLastName(pidLname);
         alert.setMpi(pidMpi);
         alert.setDob(pidDob);
         alert.setSex(pidSex);
         alert.setEvent(Alert.REPORTED);
         //------------------- description of study
         String desc = "Accn: " + ft1Acc +
                 "\nCode: " + ft1Dsc +
                 "\nDesc: " + ft1Descr +
                 "\nComp: " + ft1Comp;
         alert.setDescription(desc);
         alert.setRequest(request);
         session.save(alert);
         trans.commit();

         alerts.add(alert);  // Collect generated alerts
      } // EO examine requests

      if (alerts.isEmpty()) {
         Util.getMsglog().info("No alerts generated");
         Util.getMsglog().debug("Returned: " + HL7V2.format(ack));
         return ack;
      }

      //***************** generate e-mails and log entries for alerts
      Set<User> emails = new HashSet<User>();
      trans = session.beginTransaction();
      String q = "select u from user u join u.studyUsers su " +
                 "where su.study.dba = :dba and " +
                 "su.receiveAlerts = true";
      for (Alert alert : alerts) {
         @SuppressWarnings("unchecked")
         List<User> usrs = session.createQuery(q)
                 .setInteger("dba", alert.getRequest().getStudy().getDba())
                 .list();
         logAlert(alert, usrs);
         emails.addAll(usrs);
      } // EO process generated alerts

      if (emails.isEmpty()) {
         Util.getTranslog().warn("Alerts with no assigned recipients");
         Util.getMsglog().info("Alerts with no assigned recipients");
         Util.getMsglog().debug("Returned: " + HL7V2.format(ack));
         return ack;
      }
      //------------------------------------------- Send the e-mails
      Email email = new Email();
      if (email.sendToUsersSilent(emails, SUBJECT, BODY)) {
         Util.getTranslog().warn("Email send error(s). See syslog for details");
      }
      } catch (Exception e) {
         syslog.warn("Error processing msg : " + e.getMessage());
      }
      Util.getMsglog().info("Alerts generated");
      Util.getMsglog().debug("Returned: " + HL7V2.format(ack));
      return ack;
   }
    /**
     * Creates an ACK AA message for the passed MSH
     * @param inMSH the MSH segment if the inbound message
     * @return ACK AA message
     */
    @SuppressWarnings("unchecked")
    public static Message makeACK(MSH inMSH) {

        ACK out = new ACK();
        MSH outmsh = out.getMSH();
        MSA outmsa = out.getMSA();

        try {
        
        outmsh.getFieldSeparator().setValue("|");
        outmsh.getEncodingCharacters().setValue("^~\\&");
        DeepCopy.copy(inMSH.getReceivingApplication(), outmsh.getSendingApplication());
        DeepCopy.copy(inMSH.getReceivingFacility(), outmsh.getReceivingFacility());
        DeepCopy.copy(inMSH.getSendingApplication(), outmsh.getSendingApplication());
        DeepCopy.copy(inMSH.getSendingFacility(), outmsh.getSendingFacility());
        DeepCopy.copy(inMSH.getDateTimeOfMessage(), outmsh.getDateTimeOfMessage());
        DeepCopy.copy(inMSH.getProcessingID(), outmsh.getProcessingID());
        outmsh.getMsh9_MessageType().getMsg1_MessageType().setValue("ACK");
        outmsh.getMsh12_VersionID().getVid1_VersionID().setValue("2.4");
        
        outmsa.getMsa1_AcknowledgementCode().setValue("AA");
        DeepCopy.copy(inMSH.getMsh10_MessageControlID(), outmsa.getMsa2_MessageControlID());

       } catch (Exception e) {
          Util.getSyslog().warn("Error constructing ACK " + e.getMessage());
          return out;
       }

        return out;

    } // EO makeACK

     private static SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy");

     private void logAlert(Alert alert, List<User> users) {
         Request request = alert.getRequest();
         StringBuilder logMsg = new StringBuilder("Report Alert Generated:\n");
         logMsg.append(makeLine("study", request.getStudy().getId()));
         for (User u : users) {
            logMsg.append(makeLine("sendto", u.getFirstName() + " " +
                 u.getLastName() + " at " + u.getEmail()));
         }
         logMsg.append(makeLine("", "Event", "Request"));
         logMsg.append(makeLine("----------","---------------","---------------"));
         logMsg.append(makeLine("mpi", alert.getMpi(),request.getMpi()));
         logMsg.append(makeLine("last name", alert.getLastName(), request.getLastName()));
         logMsg.append(makeLine("first name", alert.getFirstName(), request.getFirstName()));
         logMsg.append(makeLine("dob", safeFormat(df, alert.getDob()), safeFormat(df, request.getDob())));
         logMsg.append(makeLine("sex", alert.getSex(), request.getSex()));
         logMsg.append(makeLine("desc", alert.getDescription()));
         Util.getTranslog().info(logMsg.toString());
         Util.getMsglog().info("Alert generated");
   }
   private String makeLine(String c1, String c2) {
      return makeLine(c1, c2, null);
   }
   private String makeLine(String c1, String c2, String c3) {
      StringBuilder line = new StringBuilder("   ").append(c1);
      while (line.length() < 16) line.append(" ");
      line.append(c2);
      if (c3 != null) {
         while (line.length() < 36) line.append(" ");
         line.append(c3);
      }
      line.append("\n");
      return line.toString();
   }

   private String safeFormat(SimpleDateFormat df, Date dt) {
      if (dt == null) return "";
      try {
         return df.format(dt);
      } catch (Exception e) {
         return "Invalid";
      }
   }

} // EO DFTHandler class
