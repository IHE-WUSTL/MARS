package edu.wustl.mir.mars.web;

import edu.wustl.mir.mars.util.Util;
import edu.wustl.mir.mars.hibernate.HibernateUtil;
import java.io.Serializable;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 * Common bean functionality.
 * @author rmoult01
 */
public class BeanBase implements Serializable {

   static final long serialVersionUID = 1L;

   protected SessionBean sessionBean = (SessionBean) FacesUtils.getManagedBean("sessionBean");
   protected Logger syslog = Util.getSyslog();


   /****************************************************************************
    *********************************** Hibernate
    ***************************************************************************/

   protected SessionFactory sf = HibernateUtil.getSessionFactory();
   protected Session session = null;
   protected Transaction transaction = null;

   protected void getNewSession() {
      discardSession();
      session = sf.openSession();
      transaction = session.beginTransaction();
   }

   protected void discardSession() {
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
   protected void commit(boolean newTransaction) throws Exception {
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
   protected void commit() throws Exception { commit(false); }

   /**
    * Roll back current transaction. It is expected that a valid session and a
    * valid transaction exist.
    * @param newTransaction boolean. If true, starts a new transaction after
    * rolling back this one.
    * @throws Exception on error
    */
   protected void rollback(boolean newTransaction) throws Exception{
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
   protected void rollback() throws Exception { rollback(false); }
   /**
    * Gets a new transaction.  If no session exists, it is created.
    * @param commitOldTransaction If an active transaction exists, and this is
    * true, it will be committed; If false it will be rolled back.
    * @return
    */
   protected void newTransaction(boolean commitOldTransaction) throws Exception{
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

   protected void newTransaction() throws Exception {
      newTransaction(false);
   }
}
