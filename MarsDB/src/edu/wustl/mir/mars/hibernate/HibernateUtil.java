package edu.wustl.mir.mars.hibernate;


import edu.wustl.mir.mars.util.Util;
import java.io.File;
import java.util.Iterator;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.lang.StringUtils;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.jdbc.Work;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/*
 * Hibernate Utility class. Handles configuration and maintenance of the 
 * standard hibernate configuration and sessionfactory. 
 */
public class HibernateUtil implements HibernateAppenderSessionService {

	private static Configuration cnf = null;
   private static boolean configured = false;
	private static SessionFactory sf = null;

   private static final String HIBERNATE_MANAGED_CLASSES = "HibernateManagedClasses";

   /**
    * Initializes Hibernate
    * @param ini previously initialized
    * {@link org.apache.commons.configuration.HierarchicalINIConfiguration ini} object
    * @throws Exception on error
    */
   public static void Initialize(HierarchicalINIConfiguration ini) throws Exception {
      if (configured) return;
      configured = true;
      
      cnf = new Configuration();

      //-------------------------- Load hibernate properties from ini file
      @SuppressWarnings("unchecked")
      Iterator<String> properties = (Iterator<String>) ini.getKeys("HibernateProperties");
      while (properties.hasNext()) {
         String key = properties.next();
         String value = ini.getString(key, "NOF");
         if (!value.equalsIgnoreCase("NOF")) {
            key = StringUtils.replace(key, "HibernateProperties", "hibernate", 1);
            key = StringUtils.replace(key, "..", ".");
            cnf.setProperty(key, value);
            Util.getSyslog().trace("setProperty(" + key + "," + value + ")");
         }
      }

      //---------------------- load hibernate managed classes from ini file
      @SuppressWarnings("unchecked")
      Iterator<String> classes = (Iterator<String>) ini.getKeys(HIBERNATE_MANAGED_CLASSES);
      while (classes.hasNext()) {
         String className = classes.next();
         className = StringUtils.replace(className, "..", ".");
         className = StringUtils.replace(className, HIBERNATE_MANAGED_CLASSES + ".", "");
         cnf.addAnnotatedClass(Class.forName(className));
         Util.getSyslog().trace("addAnnotatedClass(" + className + ")");
      }

      sf = cnf.buildSessionFactory();

      //------------------- Recreate Data Base Schema
      String recreateDB = ini.getString("Hibernate.DropCreateInstanceSchema", "NO");
      if (recreateDB.equalsIgnoreCase("YES")) {
         new SchemaExport(cnf).create(false, true);

         //---------------- Load import db
         String load = ini.getString("Hibernate.ImportFile", "NO");
         if (load.equalsIgnoreCase("YES")) {
            String pfn = ini.getString("Hibernate.ImportFileName", "import.sql");
            if (!pfn.startsWith(File.separator)) {
               pfn = Util.getRunDirectoryPath() + File.separator + pfn;
            }
            Work work = new ScriptRunner(pfn);
            Session s = sf.openSession();
            Transaction tx = s.beginTransaction();
            s.doWork(work);
            tx.commit();
            s.close();
         }
      }
   }

	/**
	 * returns the hibernate SessionFactory. 
	 * @return
	 */
	public static SessionFactory getSessionFactory() {
		return sf;
	}
	
	/** 
	 * Opens a session for the log4j HibernateAppender
	 * @return the session object.
	 */
	public Session openSession() throws HibernateException {
		return sf.openSession();
	}
	
	

}