package edu.wustl.mir.mars.util;

import java.io.File;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import java.sql.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;

/**
 * Provide static utility methods for mars system
 */
public class Util {

   // **************************************************************************
   // *********************** Properties
   // **************************************************************************
   private static String runDirectory;
   private static String runDirectoryPath;
   private static HierarchicalINIConfiguration ini;
   private static Logger syslog, msglog, translog;
   private static boolean initialized = false;
   private static HashMap<String, DataBaseConnection> dbs =
        new HashMap<String, DataBaseConnection>();
    private static HashSet<String> drvrs = new HashSet<String>();


   /**
    * @param runDir
    * @param iniName
    * @throws Exception
    */
	public static synchronized void initialize(String runDir, String iniName)
      throws Exception {
      if (initialized) return;
      initialized = true;

      runDirectory = runDir;
		// ---------------------------------------- Validate run Directory Path
		File f = new File(runDirectory);
		runDirectoryPath = f.getAbsolutePath();
		if (!f.exists())
			throw new Exception(runDirectoryPath + " not found");
		if (!f.isDirectory())
			throw new Exception(runDirectoryPath + " not directory");
		if (!f.canRead() || !f.canWrite())
			throw new Exception(runDirectoryPath + " not read/write/execute");

		// -------------------------------------------- Validate log4j properties
		String log4jPfn = runDirectoryPath + File.separator +"log4j.properties";
		f = new File(log4jPfn);
		if (!f.exists())
			throw new Exception(log4jPfn + " not found");
		if (!f.isFile())
			throw new Exception(log4jPfn + " not valid file");
		if (!f.canRead())
			throw new Exception(log4jPfn + " not readable");
      PropertyConfigurator.configure(log4jPfn);

		// ----------------------------------------------- Validate ini file
		String iniPfn = runDirectoryPath + File.separator + iniName + ".ini";
		f = new File(iniPfn);
		if (!f.exists())
			throw new Exception(iniPfn + " not found");
		if (!f.isFile())
			throw new Exception(iniPfn + " not valid file");
		if (!f.canRead() || !f.canWrite())
			throw new Exception(iniPfn + " not read/write");
      ini = new HierarchicalINIConfiguration(iniPfn);

      syslog = Logger.getLogger("system");
      msglog = Logger.getLogger("message");
      translog = Logger.getLogger("transaction");

      syslog.info("run directory: " + runDirectoryPath);
      syslog.info("ini file: " + iniPfn);
      syslog.info("log4j file: " + log4jPfn);

   } // EO initialize method

      //************************************************************************
		//************************************************************************
		//************************* logging Methods ******************************
		//************************************************************************
		//************************************************************************

	/**
	 * Gets the syslog {@link org.apache.log4j.Logger Logger} object.<p/>
	 * syslog is the general system log used for debug warning and error logging.
	 * @return Logger object
	 */
	public static Logger getSyslog() { return syslog; }

	/**
	 * Gets the translog {@link org.apache.log4j.Logger Logger} object.<p/>
	 * translog is used to log alert transactions, that is, messages which
    * resulted in an alert being sent.
	 * @return Logger object
	 */
   public static Logger getTranslog() { return translog; }
	/**
	 * Gets the msglog {@link org.apache.log4j.Logger Logger} object.<p/>
	 * msglog is used to log all messages and responses
	 * @return Logger object
	 */
   public static Logger getMsglog() { return msglog; }



      //************************************************************************
		//************************************************************************
		//************************ ini file Methods ******************************
		//************************************************************************
		//************************************************************************

	/**
	 * Returns string value of configuration parameter with section/key format.
	 * If parameter does not exist, the default value is returned.
	 *
	 * @param section of configuration file
	 * @param key parameter key
	 * @param defaultValue returned if parameter does not exist
	 * @return parameter value, or default value
	 * @throws Exception
	 */
	public static String getParameterString(String section, String key,
			String defaultValue) throws Exception {
		String kv = ini.getString(section + "." + key, defaultValue);
		syslog.debug("getParam [" + section + "] " + key + " value: " + kv);
		return kv;
	}
	/**
	 * sets value of configuration parameter with section/key format.
	 * if parameter does not exist, it will be created.
	 * @param section of configuration
	 * @param key parameter key
	 * @param value stored for parameter
	 * @throws Exception
	 */
   public static void setParameterString(String section, String key,
           String value) throws Exception {
      ini.setProperty(section + "." + key, value);
      syslog.debug("setParam [" + section + "] " + key + " to: " + value);
   }

	/**
	 * Returns integer value of configuration parameter with section/key format.
	 * If parameter does not exist, the default value will be returned.
	 * @param section of configuration
	 * @param key parameter key
	 * @param defaultValue value returned if key is not found.
	 * @return parameter value, or default value
	 * @throws Exception
	 */
   public static int getParameterInt(String section, String key, int defaultValue)
           throws Exception {
      int kv = ini.getInt(section + "." + key, defaultValue);
      syslog.debug("getParam [" + section + "] " + key + " value: " + kv);
      return kv;
   }

	/**
	 * Stores integer value of configuration parameter with section/key
	 * format. if parameter does not exist, it will be created.
	 * @param section of configuration
	 * @param key parameter key
	 * @param value to store
	 * @throws Exception on error
	 */
   public static void setParameterInt(String section, String key, int value)
           throws Exception {
      ini.setProperty(section + "." + key, value);
      syslog.debug("setParam [" + section + "] " + key + " to: " + value);
   }




   //************************************************************************
   //************************************************************************
   //********************* Getter/Setter Methods ****************************
   //************************************************************************
   //************************************************************************

   /**
    * Gets the ini file object.
    * @return <@link org.apache.commons.configuration.HierarchicalINIConfiguration
    * HierarchicalINIConfiguration> object
    */
   public static HierarchicalINIConfiguration getIni() {
      return ini;
   }

   /**
    * Gets the absolute path of the run directory
    * @return
    */
   public static String getRunDirectoryPath() {
      return runDirectoryPath;
   }
   //************************************************************************
   //************************************************************************
   //********************* Direct JDBC Methods ******************************
   //************************************************************************
   //************************************************************************

   /*------------------------------------------------------------------------
    * Databases are set up in the configuration file using a section:<br/>
    *   [databaseName_DB]<br/>
    *   UserId=userId            (def "syslog")<br/>
    *   Password=password        (def "syslog")<br/>
    *   ConnectionString=connStr (def "jdbc:postgresql://127.0.0.1/databaseName")<br/>
    *   DriverName=driverName    (def "org.postgresql.Driver")<br/>
    *----------------------------------------------------------------------*/


        /**
         * Returns connection information for named database, creates if needed
         * @param databaseName
         * @return DataBaseconnection object for this database name
         * @throws Exception if error occurs
         */
        private static DataBaseConnection getDataBase(String databaseName)
            throws Exception {
            String section = databaseName + "_DB";
            //------------------------- Retrieve database connection object from map
            DataBaseConnection c = dbs.get(databaseName);
            //--------------------------------- Create new database object if needed
            if (c == null) {
                c = new DataBaseConnection();
                //-------------------------------------- loads JDBC driver if needed
                String drv = getParameterString(section, "DriverName",
                "org.postgresql.Driver");
                if (!drvrs.contains(drv)) {
                    try {
                        Class.forName(drv).newInstance();
                    } catch (Exception e) {
                        syslog.error("Couldn't load " + drv + " " + e.getMessage());
                        throw new Exception(e);
                    }
                    drvrs.add(drv);
                }
                dbs.put(databaseName, c);
            }
            //----------------------------------------- get new connection if needed
            if (c.conn == null) {
                String url = getParameterString(section, "ConnectionString",
                    "jdbc:postgresql://127.0.0.1/" + databaseName);
                String user = getParameterString(section, "UserId", databaseName);
                String password = getParameterString(section, "Password", "actor");
                try {
                    c.conn = DriverManager.getConnection(url, user, password);
                } catch (Exception e) {
                    c.conn = null;
                    StringBuilder b = new StringBuilder();
                    b.append("Couldn't connect to ").append(url);
                    b.append(" u=").append(user).append(" pw=").append(password);
                    syslog.error(b.toString());
                    throw e;
                }
            }
            return c;
        }

        /**
         * Perform SQL query on database, returning ResultSet
         * This routine will load driver if required, and will make or re-establish
         * JDBC connection.  user is responsible for freeing JDBC resources, and
         * should call dbClose(databaseName) when finished using db. 
         * @param databaseName (from configuration file)
         * @param querySQL String, StringBuffer or StringBuilder
         * @param resultSetType the {@link java.sql.ResultSet ResultSet} type
         * for this transaction, 
         * {@link java.sql.ResultSet#TYPE_FORWARD_ONLY TYPE_FORWARD_ONLY},
         * {@link java.sql.ResultSet#TYPE_SCROLL_INSENSITIVE TYPE_SCROLL_INSENSITIVE},
         * or {@link java.sql.ResultSet#TYPE_SCROLL_SENSITIVE TYPE_SCROLL_SENSITIVE}
         * @param resultSetConcurrency the {@link java.sql.ResultSet ResultSet}
         * concurrency mode for this transaction,
         * {@link java.sql.ResultSet#CONCUR_READ_ONLY CONCUR_READ_ONLY} or
         * {@link java.sql.ResultSet#CONCUR_UPDATABLE CONCUR_UPDATABLE}
         * @return {@link java.sql.ResultSet ResultSet}, null on error
         * @throws Exception on error
         */
        public static ResultSet dbQuery(String databaseName, String querySQL,
                int resultSetType, int resultSetConcurrency)
            throws Exception {
            try {
                syslog.info(databaseName + " query = " + querySQL);
                DataBaseConnection c = getDataBase(databaseName);
                Statement select =
                    c.conn.createStatement(resultSetType, resultSetConcurrency);
                c.lastResultSet = select.executeQuery(querySQL);
                c.lastSQLMetaData = c.lastResultSet.getMetaData();
                return c.lastResultSet;
            } catch (Exception e) {
                StringBuilder b = new StringBuilder();
                b.append(databaseName);
                b.append(" query\n\n   ").append(querySQL);
                b.append("\n\n   Error: ").append(e.getMessage());
                StackTraceElement[] s = e.getStackTrace();
                for (int i = 0; i < s.length; i++) {
                    if (i == 0) b.append("\n\n  Stack Trace:\n");
                    b.append("\n   ").append(s.toString());
                }
                syslog.error(b.toString());
                throw e;
            }
        }/**
         * Perform SQL query on database, returning ResultSet
         * This routine will load driver if required, and will make or re-establish
         * JDBC connection.  user is responsible for freeing JDBC resources, and
         * should call dbClose(databaseName) when finished using db. ResultSet
         * returned is scrollable read-only<p/>
         * @param databasename (from configuration file)
         * @param querySQL String, StringBuffer or StringBuilder
         * @return ResultSet, null on error
         * @throws Exception on error
         */
        /**
         * Perform SQL query on database, returning ResultSet
         * This routine will load driver if required, and will make or re-establish
         * JDBC connection.  user is responsible for freeing JDBC resources, and
         * should call dbClose(databaseName) when finished using db. Returned
         * ResultSet is
         * {@link java.sql.ResultSet#TYPE_SCROLL_SENSITIVE TYPE_SCROLL_SENSITIVE}
         * {@link java.sql.ResultSet#CONCUR_READ_ONLY CONCUR_READ_ONLY}.
         *
         * @param databasename (from configuration file)
         * @param querySQL String or StringBuffer
         * @return {@link java.sql.ResultSet ResultSet}, null on error
         * @throws Exception on error
         */
        public static ResultSet dbQuery(String databasename, String querySQL)
            throws Exception {
           return dbQuery(databasename, querySQL,
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
        }
        /**
         * Convenience method to call
         * {@link edu.wustl.mir.mars.util.Util#dbQuery(String databasename,
           String querySQL) dqQuery}
         * using a StringBuffer to hold the query.
         */
        public static ResultSet dbQuery(String databasename, StringBuffer querySQL)
            throws Exception {
            return dbQuery(databasename, querySQL.toString());
        }/**
         * Perform SQL query on database, returning ResultSet
         * This routine will load driver if required, and will make or re-establish
         * JDBC connection.  user is responsible for freeing JDBC resources, and
         * should call dbClose(databaseName) when finished using db. ResultSet
         * returned is scrollable read-only<p/>
         * @param databasename (from configuration file)
         * @param querySQL String, StringBuffer or StringBuilder
         * @return ResultSet, null on error
         * @throws Exception on error
         */
        public static ResultSet dbQuery(String databasename, StringBuilder querySQL)
            throws Exception {
            return dbQuery(databasename, querySQL.toString());
        }
        /**
         * Perform SQL query on database, returning record counts
         * This routine will load driver if required, and will make or re-establish
         * JDBC connection.  user is responsible for freeing JDBC resources, and
         * should call dbClose(databaseName) when finished using db.<p/>
         * @param databaseName (from configuration file)
         * @param querySQL
         * @return integer record count, -1 on error
         * @throws Exception on error
         */
        public static int dbUpdate(String databaseName, String querySQL)
            throws Exception {
            int recordCount = -1;
            try {
                syslog.debug(databaseName + " query = " + querySQL);
                DataBaseConnection c = getDataBase(databaseName);
                Statement update = c.conn.createStatement();
                recordCount = update.executeUpdate(querySQL);
                return recordCount;
            } catch (Exception e) {
                StringBuilder b = new StringBuilder();
                b.append(databaseName);
                b.append(" query\n\n   ").append(querySQL);
                b.append("\n\n   Error: ").append(e.getMessage());
                StackTraceElement[] s = e.getStackTrace();
                for (int i = 0; i < s.length; i++) {
                    if (i == 0) b.append("\n\n  Stack Trace:\n");
                    b.append("\n   ").append(s.toString());
                }
                syslog.error(b.toString());
                throw e;
            }
        }
        public static int dbUpdate(String databasename, StringBuffer querySQL)
            throws Exception {
            return dbUpdate(databasename, querySQL.toString());
        }

        /**
         * Closes JDBC connection for databaseName<p/>
         * @param databaseName (from configuration file)
         * @throws Exception on error
         */
        public static void dbClose(String databaseName) throws Exception {
            try {
                syslog.info(databaseName + " closed");
                DataBaseConnection c = getDataBase(databaseName);
                if (c.conn != null) {
                    c.conn.close();
                    c.conn = null;
                    return;
                }
            } catch (Exception e) {
                syslog.error(databaseName + " dbClose failed: " + e.getMessage());
                throw e;
            }
        }

        public static String formatSSN(String ssn) {
           if (ssn == null || ssn.length() == 0) return "";
           String s = ssn;
           while (s.length() < 9) s = "0" + s;
           return s.substring(0,3) + "-" + s.substring(3, 5) + "-" +
                   s.substring(5, 8);
        }
        /**
         * Determines if the two passed string are significantly equal, that is,
         * they are not null, empty, or just whitespace, and their contents are
         * equal. Ignores case
         * @param one first string
         * @param two second string
         * @return boolean true if strings are significantly equal, false
         * otherwise.
         */
        public static boolean significantlyEqual(String one, String two) {
           String a = StringUtils.trimToEmpty(one);
           String b = StringUtils.trimToEmpty(two);
           if (a.length() > 0 && a.equalsIgnoreCase(b)) return true;
           return false;
        }
        /**
         * Determines if the two passed {@link java.security.Date Date} objects
         * are significantly equal, that is, they are not null and represent the
         * same date.
         * @param one first Date
         * @param two second Date
         * @return boolean true if dates are significantly equal, false
         * otherwise.
         */
        public static boolean significantlyEqual(java.sql.Date one, java.sql.Date two) {
           if (one != null && two != null && one.compareTo(two) == 0)
              return true;
           return false;
        }


} // EO MessageProcessor class
