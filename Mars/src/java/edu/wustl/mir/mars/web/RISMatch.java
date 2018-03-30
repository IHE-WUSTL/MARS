package edu.wustl.mir.mars.web;

import edu.wustl.mir.mars.db.Request;
import edu.wustl.mir.syngo.db.Patient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Backing object for select matching patient from RIS
 * @author rmoult01
 */

public class RISMatch implements Serializable {

   static final long serialVersionUID = 1L;

   public enum State {SELECT_PATIENT, MATCH_PATIENT}

   private State state = State.SELECT_PATIENT;
   private Patient patient;
   private Patient[] patients;
   private RequestBean bean;
   private Request request;
   private String sortColumnName="name", oldSortColumnName="none";
   private boolean ascending=true, oldAscending=true;

   public boolean isSelectPatient() { return state == State.SELECT_PATIENT; }
   public boolean isMatchPatient() { return state == State.MATCH_PATIENT; }

   
    public RISMatch() { }

    public void setup(RequestBean bean, Request request) throws Exception {
       this.bean = bean;
       this.request = request;
       patients = Patient.getPatients(request);
    }


    /* Patient selected for match action listener */
    public void patientSelected() {
      patient = null;
      for (Patient p : patients) {
         if (p.isSelected()) {
            patient = p;
            state = State.MATCH_PATIENT;
            return;
         }
      }
      state = State.SELECT_PATIENT;
    }

    public Patient[] getPatients() {
      if (!sortColumnName.equals(oldSortColumnName) ||
              ascending != oldAscending) {
         oldSortColumnName = sortColumnName;
         oldAscending = ascending;
         Comparator<Patient> comp = new Patient.PatientComparator(sortColumnName, ascending);
         Arrays.sort(patients, comp);
      }
      return patients;
   }

    /* Match button action listener */
    public void match() {
       request.setFirstName(patient.getPtFirst());
       request.setLastName(patient.getPtLast());
       request.setMpi(patient.getPtMedRecNo());
       request.setDob(patient.getPtBirthDtime());
       request.setSex(patient.getPtSex());
       request.setPtItn(patient.getPatItn());
       FacesUtils.addErrorMessage("Request matched to selected patient");
       bean.clearRISMatch();
    }

    /* Cancel button action listener */
    public void cancel() {
       request.setPtItn("");
       FacesUtils.addErrorMessage("RIS match cleared");
       bean.clearRISMatch();
    }


}
