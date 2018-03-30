package edu.wustl.mir.mars.hibernate;

import edu.wustl.mir.mars.util.Util;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.*;
import org.apache.log4j.Logger;
import org.hibernate.jdbc.Work;

/**
 * Tool to run database scripts
 */
public class ScriptRunner implements Work{

	private boolean stopOnError = true;
	private boolean autoCommit = true;

	private String delimiter = ";";
	private boolean fullLineDelimiter = false;

   private Logger syslog = Util.getSyslog();
   private String pfn = Util.getRunDirectoryPath() + File.pathSeparator + "import.sql";

	/**
	 * Default constructor
	 */
   public ScriptRunner() { }
   public ScriptRunner(String pfn) {this.pfn = pfn;}

	public void setDelimiter(String delimiter, boolean fullLineDelimiter) {
		this.delimiter = delimiter;
		this.fullLineDelimiter = fullLineDelimiter;
	}

	/**
	 * Runs SQL commands in file.<br/>
    * The default file is import.sql in the runDirectory. Use setPfn() to change.<br/>
    * By default, processing will stop if an error is encountered. Use
    * setStopOnError(false) to change.<br/>
	 * @throws SQLException if any SQL errors occur
	 * @throws IOException if there is an file error
	 */
	public void execute(Connection conn) throws SQLException {
		StringBuffer command = null;

		try {
         File f = new File(pfn);
         if (!f.exists()) throw new Exception("File not found: " + pfn);
         if (!f.isFile()) throw new Exception("Not valid file: " + pfn);
         if (!f.canRead()) throw new Exception("Not readable file: " + pfn);
         BufferedReader reader = new BufferedReader(new FileReader(f));
			LineNumberReader lineReader = new LineNumberReader(reader);
			String line = null;
			while ((line = lineReader.readLine()) != null) {
				if (command == null) {
					command = new StringBuffer();
				}
				String trimmedLine = line.trim();
				if (trimmedLine.startsWith("--")) {
					syslog.trace(trimmedLine);
				} else if (trimmedLine.length() < 1
						|| trimmedLine.startsWith("//")) {
					// Do nothing
				} else if (trimmedLine.length() < 1
						|| trimmedLine.startsWith("--")) {
					// Do nothing
				} else if (!fullLineDelimiter
						&& trimmedLine.endsWith(delimiter)
						|| fullLineDelimiter
						&& trimmedLine.equals(delimiter)) {
					command.append(line.substring(0, line
							.lastIndexOf(delimiter)));
					command.append(" ");
					Statement statement = conn.createStatement();

					syslog.trace(command);

					boolean hasResults = false;
					if (stopOnError) {
						hasResults = statement.execute(command.toString());
					} else {
						try {
							statement.execute(command.toString());
						} catch (SQLException e) {
							e.fillInStackTrace();
							syslog.warn("Error executing: " + command);
							syslog.warn(e);
						}
					}

					if (autoCommit && !conn.getAutoCommit()) {
						conn.commit();
					}

					ResultSet rs = statement.getResultSet();
					if (hasResults && rs != null) {
						ResultSetMetaData md = rs.getMetaData();
						int cols = md.getColumnCount();
						for (int i = 0; i < cols; i++) {
							String name = md.getColumnLabel(i);
							syslog.trace(name + "\t");
						}
						while (rs.next()) {
							for (int i = 0; i < cols; i++) {
								String value = rs.getString(i);
								syslog.trace(value + "\t");
							}
						}
					}

					command = null;
					try {
						statement.close();
					} catch (Exception e) {
						// Ignore to workaround a bug in Jakarta DBCP
					}
					Thread.yield();
				} else {
					command.append(line);
					command.append(" ");
				}
			}
			if (!autoCommit) {
				conn.commit();
			}
		} catch (Exception e) {
			e.fillInStackTrace();
			syslog.warn("Error executing: " + command);
			syslog.warn(e);
			throw new SQLException(e.getMessage());
		} 
	} // EO runScript method


   public void setAutoCommit(boolean autoCommit) {
      this.autoCommit = autoCommit;
   }

   public void setStopOnError(boolean stopOnError) {
      this.stopOnError = stopOnError;
   }

   public void setPfn(String pfn) {
      this.pfn = pfn;
   }

} // EO ScriptRunner class

