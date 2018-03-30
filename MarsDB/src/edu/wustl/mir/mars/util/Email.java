package edu.wustl.mir.mars.util;

import edu.wustl.mir.mars.db.User;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.Multipart;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.lang.StringUtils;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Handler for email
 */
public class Email implements Serializable {

   private static final long serialVersionUID = 1L;

   Properties emailConfig;
   Session session;

   public Email() {
      initialize();
   }
   
   public void send(String to, String subject, String body)
           throws Exception {
      MimeMessage email = new MimeMessage(session);
      email.addRecipient(RecipientType.TO, new InternetAddress(to));
      email.setSubject(subject);
      email.setContent(body, "text/html");
      Transport.send(email);
   }

   public boolean sendSilent(String to, String subject, String body) {
      try {
         send(to, subject, body);
         return false;
      } catch (Exception e) {
         StringBuilder em = new StringBuilder("Error sending email:\n to ");
         em.append(to).append(" - ").append(e.getMessage());
         Util.getSyslog().warn(em.toString());
         return true;
      }
   }

   public void sendToUsers(Collection<User> users, String subject, String body)
           throws Exception {
      MimeMessage email = new MimeMessage(session);
      for (User u : users) {
         email.addRecipient(RecipientType.TO, new InternetAddress(u.getEmail()));
      }
      email.setSubject(subject);
      email.setContent(body, "text/html");
      Transport.send(email);
   }

   public boolean sendToUsersSilent(Collection<User> users, String subject, String body) {
      try {
         sendToUsers(users, subject, body);
         return false;
      } catch (Exception e) {
         StringBuilder em = new StringBuilder("Error sending email - ");
         em.append(e.getMessage());
         for (User u : users) {
            em.append("\n to: ").append(u.getFirstName()).append(" ")
              .append(u.getLastName()).append(" at ").append(u.getEmail());
         }
         Util.getSyslog().warn(em.toString());
         return true;
      }
   }

   public void sendToEmails(Collection<String> emails, String subject, String
           body, Collection<String> files) throws Exception {
      Message msg = new MimeMessage(session);
      for (String em : emails)
         msg.addRecipient(RecipientType.TO, new InternetAddress(em));
      msg.setSubject(subject);

      Multipart multipart = new MimeMultipart();

      BodyPart mbp = new MimeBodyPart();
      mbp.setText(body);
      multipart.addBodyPart(mbp);

      for (String f : files) {
         mbp = new MimeBodyPart();
         DataSource source = new FileDataSource(f);
         mbp.setDataHandler(new DataHandler(source));
         mbp.setFileName(StringUtils.substringAfterLast(f, "/"));
         multipart.addBodyPart(mbp);
      }
      msg.setContent(multipart);
      Transport.send(msg);
   }

   public boolean sendToEmailsSilent(Collection<String> emails, String subject, String body, Collection<String> files) {
      try {
         sendToEmails(emails, subject, body, files);
         return false;
      } catch (Exception e) {
         Util.getSyslog().warn("sendToEmailSilent: " + e.getMessage());
         return true;
      }
   }


   private void initialize() {
      HierarchicalINIConfiguration ini = Util.getIni();
      emailConfig = new Properties();
      @SuppressWarnings("unchecked")
      Iterator<String> properties = (Iterator<String>) ini.getKeys("Email");
      while (properties.hasNext()) {
         String key = properties.next();
         String value = ini.getString(key, "NOF");
         if (!value.equalsIgnoreCase("NOF")) {
            key = StringUtils.replace(key, "..", ".");
            key = StringUtils.replace(key, "Email.", "", 1);
            emailConfig.setProperty(key, value);
            Util.getSyslog().trace("Email setProperty(" + key + "," + value + ")");
         }
      }
      session = Session.getDefaultInstance(emailConfig);
   }
} // EO Email class
