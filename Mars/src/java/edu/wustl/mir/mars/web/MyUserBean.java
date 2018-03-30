package edu.wustl.mir.mars.web;

import edu.wustl.mir.mars.db.StudyUser;
import edu.wustl.mir.mars.db.User;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Backing bean for the User maintenance functions: User.xhtml
 */
@ManagedBean
@SessionScoped
public class MyUserBean extends BeanBase implements Serializable {

   @SuppressWarnings("FieldNameHidesFieldInSuperclass")
   static final long serialVersionUID = 1L;

    /** Creates a new instance of UsersBean */
    public MyUserBean() {
       sessionBean.newTaskBean("myUserBean");
       initialize();
    }



   /****************************************************************************
    *********************************** Change tab methods
    ***************************************************************************/

   
   private User chgUser = null;   
   public User getChgUser() {return chgUser;}
   public void setChgUser(User chgUser) {this.chgUser = chgUser;}

  /*
   * 1 = begin; No action to take selected
   * 2 = Modify user
   * 3 = Delete user
   * 4 = Reset pw for user
   */
  private int currentChangeStep = 1;
  public boolean renderSelectAction()   { return currentChangeStep == 1; }
  public boolean renderModifyUser()     { return currentChangeStep == 2; }
  public boolean renderDeleteUser()     { return currentChangeStep == 3; }
  public boolean renderResetUserPw()    { return currentChangeStep == 4; }

   public final void initialize() {
      getNewSession();
      chgUser = (User) session.createQuery("from user u fetch all properties where u.dba = :dba")
              .setInteger("dba", sessionBean.getUserDba())
              .uniqueResult();
      currentChangeStep = 1;
   }

   public void chgUser() { currentChangeStep = 2; }
   public void delUser() { currentChangeStep = 3; }


   public void chgOk() {
      Valid v = new Valid();
      try {
         v.startValidations();
         v.NB("User Id", chgUser.getId());
         newTransaction();
         User u = (User) session.createQuery("from user u where u.id = :id and u.dba <> :dba").
              setString("id", chgUser.getId()).
              setInteger("dba", chgUser.getDba()).
              uniqueResult();
         if (u != null) v.error("User Id", "is already in use");
         rollback();
         v.NB("First Name", chgUser.getFirstName());
         v.NB("Last Name", chgUser.getLastName());
         if (v.isErrors()) return;
         newTransaction();
         session.saveOrUpdate(chgUser);
         commit();
         initialize();
      } catch (Exception e) {
         String em = "change MyUser " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
     }
   }

   public void delOk() {
      try {
         newTransaction();
         session.delete(chgUser);
         commit();
         initialize();
         sessionBean.logoff();
      } catch (Exception e) {
         String em = "delete MyUser " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }

   public void canChg() {
      initialize();
   }

   /****************************************************************************
    *********************************** Reset password methods
    ***************************************************************************/

   public void chgPw() {
      currentChangeStep = 4;
      newPw = "";
      confirmPw = "";
   }
      
   public void resetPw() {
      Valid v = new Valid();
      v.startValidations();
      v.NB("Password ", newPw);
      if(!newPw.equals(confirmPw)) v.error("Passwords", "Don't match");
      if (v.isErrors()) {
         return;
      }
      try {
         newTransaction();
         chgUser.setPw(newPw);
         session.update(chgUser);
         commit();
         initialize();
      } catch (Exception e) {
         String em = "reset pw MyUser " + e.getMessage();
         syslog.warn(em);
         FacesUtils.addErrorMessage(em);
      }
   }

   private String newPw;
   public String getNewPw() { return newPw; }
   public void setNewPw(String newPw) {this.newPw = newPw; }

   private String confirmPw;
   public String getConfirmPw() {return confirmPw;}
   public void setConfirmPw(String confirmPw) {this.confirmPw = confirmPw;}

} // EO MyUserBean
