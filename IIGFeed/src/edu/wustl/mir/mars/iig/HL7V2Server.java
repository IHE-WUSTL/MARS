package edu.wustl.mir.mars.iig;

import edu.wustl.mir.mars.util.Util;
import java.io.FileInputStream;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.SecureRandom;
import org.apache.log4j.Logger;

import ca.uhn.hl7v2.app.Connection;
import ca.uhn.hl7v2.app.HL7Service;
import ca.uhn.hl7v2.llp.LowerLayerProtocol;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.app.Application;
import ca.uhn.hl7v2.validation.impl.NoValidation;
import java.net.ServerSocket;
import java.net.Socket;



/**
 *
 * @author rmoult01
 */
public class HL7V2Server {

   // ***********************************************************************
	// *********************** Properties
	// ***********************************************************************
   private static final String n = System.getProperty("line.separator");
   private Logger syslog;

   private ExecutorService exec = null;
   List<Handler> handlers = new ArrayList<Handler>();
   List<Handler> secureHandlers = new ArrayList<Handler>();
   Thread unsecureServerThread;
   HL7V2Server.Server unsecureServer;
   Thread secureServerThread;
   HL7V2Server.Server secureServer;


	// ***********************************************************************
	// *********************** Constructor(s)
	// ***********************************************************************

   public HL7V2Server(ExecutorService e) {
      exec = e;
      syslog = Util.getSyslog();
   }

	// ***********************************************************************
	// *********************** Public methods
	// ***********************************************************************
   /**
    * Add message handler. <b>Note: </b>Must be added in the order they are to
    * be processed by the server.
    * @param messageType HL7V2 message type, for example, "QBP"
    * @param triggerEvent HL7V2 trigger event, for example, "Q22"
    * @param handler message handler which implements hapi Application.
    */
   public void addHandler(String messageType, String triggerEvent,
           Application handler) {
      handlers.add(new Handler(messageType, triggerEvent, handler));
   }
   public void addSecureHandler(String messageType, String triggerEvent,
           Application handler) {
      secureHandlers.add(new Handler(messageType, triggerEvent, handler));
   }

   public void boot() throws Exception {
      IIGPipeParser p;
      LowerLayerProtocol l;
      int port = Util.getParameterInt("Foundation", "port", -1);
      if (port > 0) {
         p = new IIGPipeParser();
         p.setValidationContext(new NoValidation());
         l = LowerLayerProtocol.makeLLP();
         unsecureServer = new Server(p,l,port,false,handlers);
         exec.execute(unsecureServer);
         syslog.info("unsecure server started on port " + port);
      }
      port = Util.getParameterInt("Foundation", "securePort", -1);
      if (port > 0) {
         p = new IIGPipeParser();
         p.setValidationContext(new NoValidation());
         l = LowerLayerProtocol.makeLLP();
         secureServer = new Server(p,l,port,true,secureHandlers);
         exec.execute(secureServer);
         syslog.info("secure server started on port " + port);
      }
   }

   //---------------------------------------------- Getters and Setters
   public void setExecutorServer(ExecutorService e) {exec = e;}

	// ***********************************************************************
	// *********************** Inner classes
	// ***********************************************************************

    /**
     * Container class for HAPI message handlers.
     */
    private class Handler {

        public String messageType;
        public String triggerEvent;
        public Application handler;

        public Handler(String t, String e, Application a) {
            messageType = t;
            triggerEvent = e;
            handler = a;
        }
    }

    /**
     * Inner class which implements server like Hapi simple server but which
     * does not have main() or use the hapi log facility. start server by
     * running this object in a thread. stop server by setting serverRunning to
     * false or terminating simulator.
     */
    class Server extends HL7Service implements Runnable {

	// ***********************************************************************
	// *********************** Properties
	// ***********************************************************************

        private Logger syslog = HL7V2Server.this.syslog;

        private boolean serverRunning = true;
        int port;
        private final int timeout = 60000;

        //-------------------------------------------- secure server: SSL/TLS

        private boolean secure = false;
        private SecureRandom rand;
        private KeyStore keyStore;
        private KeyManagerFactory kmf;
        private TrustManagerFactory tmf;
        private SSLContext sslContext;
        private SSLServerSocketFactory ssFactory;
        private SSLServerSocket ss;
        private ServerSocket socket;
        private List<Handler> handlers;


	// ***********************************************************************
	// *********************** Constructor(s)
	// ***********************************************************************

        /**
         * @param p
         * @param l
         */
        public Server(PipeParser p, LowerLayerProtocol l, int port,
                boolean secure, List<Handler> handlers) {
            super(p, l);
            this.port = port;
            this.secure = secure;
            this.handlers = handlers;
        }

        public void run() {
           for (Handler h : handlers) {
               this.registerApplication(h.messageType, h.triggerEvent,
                        h.handler);
            }
            try {
               if (secure) {
                rand = new SecureRandom();
                rand.nextInt();

                //-------------------------------------- keystore
                String ks = Util.getParameterString("Foundation", "ServerKeyStore", "certs/server.keystore");
                String pw = Util.getParameterString("Foundation", "Password", "password");
                keyStore = KeyStore.getInstance("JKS");
                keyStore.load(new FileInputStream(ks), pw.toCharArray());
                kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(keyStore, pw.toCharArray());

                //------------------------------------ truststore
                tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(keyStore);
                syslog.info("server certificates initialized");

                //-------------------------------- Context and socket factory
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), rand);
                syslog.info("TLS trust context initialized");

                ssFactory = sslContext.getServerSocketFactory();
                ss = (SSLServerSocket) ssFactory.createServerSocket(port);
                ss.setNeedClientAuth(true);
                ss.setSoTimeout(timeout);
                syslog.info("TLS listen socketcreated");
                socket = (ServerSocket) ss;

                } else {
                  socket = new ServerSocket(port);
                  socket.setSoTimeout(timeout);
                }

                while (serverRunning) {
                    try {
                        Socket newSocket = socket.accept();
                        syslog.info("Accepted connection from "
                                + newSocket.getInetAddress().getHostAddress());
                        Connection conn = new Connection(parser, llp, newSocket);
                        newConnection(conn);
                    } catch (InterruptedIOException ie) {
                    } catch (Exception e) {
                        syslog.warn("Error while accepting connections: "
                                + e.getMessage());
                    }
                }

                ss.close();
            } catch (Exception e) {
                syslog.error(e.getMessage());
            } finally {
                stop();
            }
        }
    } // EO Server Inner class


} // EO HL7V2Server Class