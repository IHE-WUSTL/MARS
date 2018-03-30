package edu.wustl.mir.mars.web;

import edu.wustl.mir.mars.util.Util;
import edu.wustl.mir.mars.hibernate.HibernateUtil;
import java.io.Serializable;
import java.util.TimeZone;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ApplicationScoped;

@ManagedBean(eager = true)
@ApplicationScoped
public class ApplicationBean  implements Serializable {
   static final long serialVersionUID = 1L;

   static final String RUN_DIRECTORY = "/home/rmoult01/NetBeansProjects/Mars/runDirectory";
   static final String INI_FILE_NAME = "mars";

   /** Creates a new instance of ApplicationBean */
   public ApplicationBean() {
      try {
         Util.initialize(RUN_DIRECTORY, INI_FILE_NAME);
         String str = Util.getParameterString("HibernateProperties", "default_schema", "");
         if (!str.equalsIgnoreCase("mars")) name = str;
         HibernateUtil.Initialize(Util.getIni());
      } catch (Exception e) {
         System.err.println("Could not instatiate ApplicationBean: " +
                 e.getMessage());
      }
   }

   public String getTimeZone() {
      TimeZone tz = TimeZone.getDefault();
      return tz.getID();
   }

   private String name = "Mallinckrodt Alert Request System";

   public String getName() {
      return name;
   }
}
