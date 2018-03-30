package edu.wustl.mir.mars.iig;


import edu.wustl.mir.mars.hibernate.HibernateUtil;
import edu.wustl.mir.mars.util.Util;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
/**
 *
 * @author rmoult01
 */
public class IIGFeed {

   private static ExecutorService exec = Executors.newCachedThreadPool();
   private static HL7V2Server hl7v2server;

public static void main(String[] args) throws Exception {

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
         iniName = line.getOptionValue("config", "IIGFeed");

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
      } catch (Exception e) {
         String em = "Initialization error: " + e.getMessage();
         System.err.println(em);
         throw new Exception(em);
      }

      hl7v2server = new HL7V2Server(exec);
      hl7v2server.addHandler("DFT", "P03", new DFTHandler());
      hl7v2server.addSecureHandler("DFT", "P03", new DFTHandler());
      hl7v2server.boot();

   } // EO main(String[] args)


} // EO IIGFeed class

