
package edu.wustl.mir.mars.web;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import javax.faces.context.FacesContext;
import org.apache.commons.lang.StringUtils;

/**
 * Used to provide validation for screen input values
 */
public class Valid implements Serializable {

   static final long serialVersionUID = 1L;
   private FacesContext context;
   private boolean errors;

   public FacesContext getContext() {
      return context;
   }

   public void setContext(FacesContext context) {
      this.context = context;
   }

   public boolean isErrors() {
      return errors;
   }

   public void setErrors(boolean errors) {
      this.errors = errors;
   }

   //**************************************************************************
   // General purpose validation methods
   //**************************************************************************
   public void startValidations() {
      context = FacesContext.getCurrentInstance();
      errors = false;
   }

   public void error(String id, String msg) {
      FacesUtils.addErrorMessage(id + " " + msg);
      errors = true;
   }

   public void NB(String id, String v) {
      if (StringUtils.isBlank(v)) {
         error(id, " Can't be null, empty, or just whitespace");
      }
   }
   public void Port(String id, int p) {
      if (p < 1 || p > 65535) {
         error(id, "Invalid port number");
      }
   }

   public void URL(String id, String v) {
      try {
         new URL(v);
      } catch (MalformedURLException ex) {
         error(id, "Invalid URL");
      }
   }

   /** Validate IPV4 address string, for example 127.0.0.1 */
   public void Ip(String id, String v) {
      boolean valid = true;
      String[] tuples = v.split("\\.");
      if (tuples.length == 4) {
         for (String tuple : tuples) {
            int i = Integer.parseInt(tuple);
            if (i >= 0 && i <= 255) {
               continue;
            }
            valid = false;
            break;
         }
      } else {
         valid = false;
      }
      if (!valid) {
         error(id, "Not a valid IP V4 address");
      }
   }

   /** Validate DICOM AE Title String */
   public void AeTitle(String id, String ae) {
      if (!StringUtils.trimToEmpty(ae).matches("\\w{1,16}"))
         error(id, "Invalid AE Title");
   }

} // EO Class Valid
