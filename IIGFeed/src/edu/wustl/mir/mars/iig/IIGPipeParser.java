/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.wustl.mir.mars.iig;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.util.Terser;


/**
 * Overrides PipeParser to provide special processing for IIG feed.
 * @author rmoult01
 */
public class IIGPipeParser extends ca.uhn.hl7v2.parser.PipeParser {
   
   @Override
   public String getVersion(String message) {
      return "2.4";
   }

   @Override
   public Segment getCriticalResponseData(String message) throws HL7Exception {
            // try to get MSH segment
            int locStartMSH = message.indexOf("MSH");
            if (locStartMSH < 0)
                throw new HL7Exception("Couldn't find MSH segment in message: " + message, HL7Exception.SEGMENT_SEQUENCE_ERROR);
            int locEndMSH = message.indexOf('\r', locStartMSH + 1);
            if (locEndMSH < 0)
                locEndMSH = message.length();
            String mshString = message.substring(locStartMSH, locEndMSH);

            // find out what the field separator is
            char fieldSep = mshString.charAt(3);

            // get field array
            String[] fields = split(mshString, String.valueOf(fieldSep));

            Segment msh = null;
            try {
                // parse required fields
                String encChars = fields[1];
                char compSep = encChars.charAt(0);
                String messControlID = fields[9];
                String[] procIDComps = split(fields[10], String.valueOf(compSep));
                if (procIDComps.length == 0)
                   procIDComps = new String[] { "NA" };
                // fill MSH segment
                String version = "2.4"; // default
                try {
                    version = this.getVersion(message);
                } catch (Exception e) { /* use the default */
                }

                msh = Parser.makeControlMSH(version, getFactory());
                Terser.set(msh, 1, 0, 1, 1, String.valueOf(fieldSep));
                Terser.set(msh, 2, 0, 1, 1, encChars);
                Terser.set(msh, 10, 0, 1, 1, messControlID);
                Terser.set(msh, 11, 0, 1, 1, procIDComps[0]);
                Terser.set(msh, 12, 0, 1, 1, version);

            } catch (Exception e) {
                throw new HL7Exception("Can't parse critical fields from MSH segment (" + e.getClass().getName() + ": " + e.getMessage() + "): " + mshString, HL7Exception.REQUIRED_FIELD_MISSING, e);
            }

            return msh;
        }

}
