/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.wustl.mir.mars.iig;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.DataTypeException;
import org.apache.commons.lang.StringUtils;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.primitive.CommonTS;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.MessageIDGenerator;
import ca.uhn.hl7v2.util.Terser;
import ca.uhn.hl7v2.validation.impl.NoValidation;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Helper method for Hl7 version 2 messages
 *
 * @author Ralph Moulton / MIR WUSTL IHE Development Project
 * @see <a href="mailto:moultonr@mir.wustl.edu">moultonr@mir.wustl.edu</a>
 * @see <a href="http://ihewiki.wustl.edu/wiki/index.php/KISimulator_Utilities">
  		KISimulator Utilities wiki page</a>
 * @version 1.0 - 2010-09-24
 */
public class HL7V2 {

	// **********************************************************************
	// *********************** constants
	// **********************************************************************
	static final String ACKNOWLEDGEMENT_CODE  = "AcknowledgementCode";  // MSA-1
	static final String ERROR_LOCATION        = "ErrorLocation";        // ERR-2
	static final String HL7_ERROR_CODE        = "HL7ErrorCode";         // ERR-3-1
	static final String HL7_ERROR_DESCRIPTION = "HL7ErrorDescription";  // ERR-3-2
	static final String SEVERITY              = "Severity";             // ERR-4


	// **********************************************************************
	// *********************** static methods
	// **********************************************************************

	/**
	 * adjusts formatting of hl7 v2 string message for printing one segment per
	 * line. Removes all line feed and carriage returns, replacing them with the
	 * system line.separator character. Places an additional line.separator at
	 * the beginning of the message. Routine is intended to preprocess hl7 v2
	 * messages for text logging. For example:
	 * <code>syssLog.info("Sent to server:" + Hl7v2.format(message)</code>.
	 *
	 * @param msg
	 *            the message to be formatted
	 * @return String the formatted message
	 */
	public static String format(String msg) {

		String n = System.getProperty("line.separator");

		String m = StringUtils.stripToEmpty(msg);
		m = n + StringUtils.replaceEach(m, new String[] { "\n\r", "\r\n", "\n",
				"\r" }, new String[] { n, n, n, n });
		return m;
	}

   public static String format(Message msg) {
      Parser p = new PipeParser();
      p.setValidationContext(new NoValidation());
      try { return format(p.encode(msg)); }
      catch (Exception e) { return "Could not parse message";}
   }
   /**
	 * adjusts formatting of hl7 v2 string message for printing one segment per
	 * line on a web page. Removes all line feed and carriage returns, replacing
    * them with "<br/>".
	 *
	 * @param msg
	 *            the message to be formatted
	 * @return String the formatted message
	 */
   public static String formatWeb(String msg) {

		String n = "<br/>";

		String m = StringUtils.stripToEmpty(msg);
		m = n + StringUtils.replaceEach(m, new String[] { "\n\r", "\r\n", "\n",
				"\r" }, new String[] { n, n, n, n });
		return m;
	}


   /**
     * Creates an ACK message with the minimum required information from an
     * inbound message. Optional fields can be filled in afterwards, before the
     * message is returned. Note that MSH-10, the outbound message control ID,
     * is set using <code>ca.uhn.hl7v2.util.MessageIDGenerator</code>. Also note
     * that the ACK messages returned is the same version as the inbound MSH if
     * there is a generic ACK for that version, otherwise a version 2.4 ACK is
     * returned. MSA-1 is set to AA by default.
     *
     * @param inMSH
     *            the MSH segment if the inbound message
     * @return ACK AA message
     * @throws IOException
     *             if there is a I/O problem with the message ID file
     * @throws DataTypeException
     *             if there is a problem setting ACK values
     */
    @SuppressWarnings("unchecked")
    public static Message makeACK(Segment inMSH) throws HL7Exception,
            IOException {
        if (!inMSH.getName().equals("MSH")) {
            throw new HL7Exception("Need an MSH segment to create a ACK");
        }

        // -------------------------------------- find version of inbound
        // message
        String version = null;
        String v = null;
        try {
            version = Terser.get(inMSH, 12, 0, 1, 1);
        } catch (HL7Exception e) { /* proceed with null */

        }
        if (version == null) {
            version = "2.4";
        }
        v = version.replace(".", "");

        String ackClassName = "ca.uhn.hl7v2.model.v" + v + ".message.ACK";

        Message out = null;
        try {
            Class ackClass = Class.forName(ackClassName);
            out = (Message) ackClass.newInstance();
        } catch (Exception e) {
            throw new HL7Exception("Can't instantiate ACK of class "
                    + ackClassName + ": " + e.getClass().getName());
        }
        Terser terser = new Terser(out);

        // -----------------populate outbound MSH using data from inbound
        // message
        Segment outMSH = (Segment) out.get("MSH");

        // ------------------------------------get MSH data from incoming
        // message
        String encChars = Terser.get(inMSH, 2, 0, 1, 1);
        String fieldSep = Terser.get(inMSH, 1, 0, 1, 1);
        String procID = Terser.get(inMSH, 11, 0, 1, 1);

        // -----------------populate outbound MSH using data from inbound
        // message
        Terser.set(outMSH, 2, 0, 1, 1, encChars);
        Terser.set(outMSH, 1, 0, 1, 1, fieldSep);
        GregorianCalendar now = new GregorianCalendar();
        now.setTime(new Date());
        Terser.set(outMSH, 7, 0, 1, 1, CommonTS.toHl7TSFormat(now));
        Terser.set(outMSH, 10, 0, 1, 1, MessageIDGenerator.getInstance().getNewID());
        Terser.set(outMSH, 11, 0, 1, 1, procID);

        terser.set("/MSH-9", "ACK");
        terser.set("/MSH-12", version);
        terser.set("/MSA-1", "AA");
        terser.set("/MSA-2", Terser.get(inMSH, 10, 0, 1, 1));

        return out;

    } // EO makeACK


}

