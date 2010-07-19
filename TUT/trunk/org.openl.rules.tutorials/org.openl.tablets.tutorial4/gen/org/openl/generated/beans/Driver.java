/*
 * This class has been generated. Do not change it. 
*/

package org.openl.generated.beans;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import java.lang.String;

public class Driver{
  private java.lang.String name;

  private java.lang.String state;

  private java.lang.String gender;

  private int age;

  private java.lang.String maritalStatus;

  private int numAccidents;

  private int numMovingViolations;

  private int numDUI;

  private boolean hadTraining;



public Driver() {
}

public Driver(String name, String gender, int age, String maritalStatus, String state, int numAccidents, int numMovingViolations, int numDUI, boolean hadTraining) {
    this.name = name;
    this.gender = gender;
    this.age = age;
    this.maritalStatus = maritalStatus;
    this.state = state;
    this.numAccidents = numAccidents;
    this.numMovingViolations = numMovingViolations;
    this.numDUI = numDUI;
    this.hadTraining = hadTraining;
}

public boolean equals(Object obj) {
    EqualsBuilder builder = new EqualsBuilder();
    if (!(obj instanceof Driver)) {;
        return false;
    }
    Driver another = (Driver)obj;    builder.append(another.name,name);
    builder.append(another.gender,gender);
    builder.append(another.age,age);
    builder.append(another.maritalStatus,maritalStatus);
    builder.append(another.state,state);
    builder.append(another.numAccidents,numAccidents);
    builder.append(another.numMovingViolations,numMovingViolations);
    builder.append(another.numDUI,numDUI);
    builder.append(another.hadTraining,hadTraining);
    return builder.isEquals();
}

public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append(name);
    builder.append(gender);
    builder.append(age);
    builder.append(maritalStatus);
    builder.append(state);
    builder.append(numAccidents);
    builder.append(numMovingViolations);
    builder.append(numDUI);
    builder.append(hadTraining);
    return builder.toString();
}

public int hashCode() {
    HashCodeBuilder builder = new HashCodeBuilder();
    builder.append(name);
    builder.append(gender);
    builder.append(age);
    builder.append(maritalStatus);
    builder.append(state);
    builder.append(numAccidents);
    builder.append(numMovingViolations);
    builder.append(numDUI);
    builder.append(hadTraining);
    return builder.toHashCode();
}
  public java.lang.String getName() {
   return name;
}
  public java.lang.String getState() {
   return state;
}
  public void setName(java.lang.String name) {
   this.name = name;
}
  public void setState(java.lang.String state) {
   this.state = state;
}
  public java.lang.String getGender() {
   return gender;
}
  public void setGender(java.lang.String gender) {
   this.gender = gender;
}
  public int getAge() {
   return age;
}
  public void setAge(int age) {
   this.age = age;
}
  public java.lang.String getMaritalStatus() {
   return maritalStatus;
}
  public void setMaritalStatus(java.lang.String maritalStatus) {
   this.maritalStatus = maritalStatus;
}
  public int getNumAccidents() {
   return numAccidents;
}
  public void setNumAccidents(int numAccidents) {
   this.numAccidents = numAccidents;
}
  public int getNumMovingViolations() {
   return numMovingViolations;
}
  public void setNumMovingViolations(int numMovingViolations) {
   this.numMovingViolations = numMovingViolations;
}
  public int getNumDUI() {
   return numDUI;
}
  public void setNumDUI(int numDUI) {
   this.numDUI = numDUI;
}
  public boolean getHadTraining() {
   return hadTraining;
}
  public void setHadTraining(boolean hadTraining) {
   this.hadTraining = hadTraining;
}

}