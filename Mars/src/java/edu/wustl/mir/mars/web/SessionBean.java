/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.wustl.mir.mars.web;

import edu.wustl.mir.mars.db.User;
import edu.wustl.mir.mars.hibernate.HibernateUtil;
import java.io.Serializable;
import java.sql.ResultSet;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author rmoult01
 */
@ManagedBean
@SessionScoped
public class SessionBean implements Serializable {

   static final long serialVersionUID = 1L;

   private String userId = "";
   private int userDba;
   private boolean admin = false;

   private String id;
   private String pw;
   private String userName = "";

    /** Creates a new instance of SessionBean */
    public SessionBean()  {
    }

    public void login() {
      ResultSet res = null;
      userId = "";
      admin = false;
      Valid valid = new Valid();
      valid.startValidations();
      valid.NB("user id", id);
      valid.NB("password", pw);
      if (valid.isErrors()) return;
      try {
         Session session = HibernateUtil.getSessionFactory().openSession();
         Transaction trans = session.beginTransaction();
         User user = (User) session.createQuery("from user u where u.id = :val")
                 .setString("val", id)
                 .uniqueResult();
         trans.rollback();
         session.close();
         if (user == null) {
            FacesUtils.addErrorMessage("No such user");
            return;
         }
         if (!user.getPw().equals(pw)) {
            FacesUtils.addErrorMessage("No such user/pw");
            return;
         }
         userId = id;
         userDba = user.getDba();
         admin = user.isAdmin();
         userName = user.getFirstName() + " " + user.getLastName();
         if (admin) userName += " - Administrator";
         return;

      } catch (Exception ex) {
         FacesUtils.addErrorMessage("database error: " + ex.getMessage());
      }
   }

    public void logoff() {
       userId = "";
       admin = false;
       id = "";
       pw = "";
       newTaskBean(null);
    }

   public int getUserDba() {
      return userDba;
   }

   public void setUserDba(int userDba) {
      this.userDba = userDba;
   }


   public String getUserId() {
      return userId;
   }

   public void setUserId(String userId) {
      this.userId = userId;
   }

   public boolean isAdmin() {
      return admin;
   }

   public void setAdmin(boolean admin) {
      this.admin = admin;
   }


   public boolean isLoggedIn() {
      return (!userId.isEmpty());
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public String getPw() {
      return pw;
   }

   public void setPw(String pw) {
      this.pw = pw;
   }

   public String getUserName() {
      return userName;
   }

   public void setUserName(String userName) {
      this.userName = userName;
   }

   private String currentTaskBean = null;
   /**
    * Establishes new "current task bean", which mars understands to be the
    * bean handling the current task. The previous value (if any) is removed
    * from the context.  By doing this, mars eliminates any context from a task
    * when the user selects a new task.  If the user then returns to the 
    * previous task, they get a new context, which makes more sense to them.
    * This method should be called from the new bean's constructor or initialize
    * method.
    * @param beanName new current bean name
    */
   public void newTaskBean(String beanName) {
      if (currentTaskBean != null)
         FacesUtils.resetManagedBean(currentTaskBean);
      currentTaskBean =  beanName;
   }
    
}
