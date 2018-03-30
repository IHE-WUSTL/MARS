/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package edu.wustl.mir.mars.web;

import java.io.Serializable;
import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.event.ActionEvent;

/**
 * Manages the popups on the lambda template
 * @author rmoult01
 */
@ManagedBean(name="popup")
@SessionScoped
public class PopupBean implements Serializable {

   static final long serialVersionUID = 1L;

   private String headerMessage = "";
   private String bodyMessage = "";
   private boolean show = false;
   private String style = "width=200px";
   private Integer level = 0;   // 0=no icon 1=info 2=warning, 3=error


    /** Creates a new instance of PopupBean */
    public PopupBean() {
    }

   public boolean isShow() {
      return show;
   }

   public void setShow(boolean show) {
      this.show = show;
   }

   public String getStyle() {
      return style;
   }

   public void setStyle(String style) {
      this.style = style;
   }

   
   public String getBodyMessage() {
      return bodyMessage;
   }

   public void setBodyMessage(String bodyMessage) {
      this.bodyMessage = bodyMessage;
   }


   public String getHeaderMessage() {
      return headerMessage;
   }

   public void setHeaderMessage(String headerMessage) {
      this.headerMessage = headerMessage;
   }

   public void close(ActionEvent e) {
      show = false;
      bodyMessage = "";
      headerMessage = "";
      level = 0;
   }


   public void message(String header, String body) {
      headerMessage = header;
      bodyMessage = body;
      show = true;
      level = 0;
   }
   public void infoMessage(String header, String body) {
      headerMessage = header;
      bodyMessage = body;
      show = true;
      level = 1;
   }
   public void warningMessage(String header, String body) {
      headerMessage = header;
      bodyMessage = body;
      show = true;
      level = 2;
   }
   public void errorMessage(String header, String body) {
      headerMessage = header;
      bodyMessage = body;
      show = true;
      level = 3;
   }
   public Integer getLevel() { return level; }

} // EO Class
