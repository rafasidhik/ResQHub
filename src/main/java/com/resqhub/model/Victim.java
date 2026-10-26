package com.resqhub.model;

/**
 * Registered victim. Multilevel inheritance: Victim -> Person -> BaseEntity.
 */
public class Victim extends Person {

    private int age;
    private Gender gender;
    private EmergencyStatus emergencyStatus = EmergencyStatus.SAFE;
    private String medicalCondition;
    private String familyInfo;
    private String currentLocation;
    private ShelterStatus shelterStatus = ShelterStatus.NOT_SHELTERED;
    private Long disasterId;
    private Long registeredBy;

    public Victim() {
        super();
    }

    public Victim(String fullName, int age, Gender gender) {
        super(fullName);
        this.age = age;
        this.gender = gender;
    }

    public Victim(String fullName, int age, Gender gender, String phone) {
        super(fullName, phone);
        this.age = age;
        this.gender = gender;
    }

    @Override
    public String getDetails() {
        String status = emergencyStatus == null ? "?" : emergencyStatus.getLabel();
        return fullName + ", " + age + " (" + status + ") @ "
                + (currentLocation == null ? "unknown" : currentLocation);
    }

    /** Children and senior citizens are treated as vulnerable persons. */
    public boolean isVulnerableAge() {
        return age <= 12 || age >= 60;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public EmergencyStatus getEmergencyStatus() {
        return emergencyStatus;
    }

    public void setEmergencyStatus(EmergencyStatus emergencyStatus) {
        this.emergencyStatus = emergencyStatus;
    }

    public String getMedicalCondition() {
        return medicalCondition;
    }

    public void setMedicalCondition(String medicalCondition) {
        this.medicalCondition = medicalCondition;
    }

    public String getFamilyInfo() {
        return familyInfo;
    }

    public void setFamilyInfo(String familyInfo) {
        this.familyInfo = familyInfo;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public ShelterStatus getShelterStatus() {
        return shelterStatus;
    }

    public void setShelterStatus(ShelterStatus shelterStatus) {
        this.shelterStatus = shelterStatus;
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public Long getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(Long registeredBy) {
        this.registeredBy = registeredBy;
    }
}
