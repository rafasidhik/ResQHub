package com.resqhub.model;

/** A person who volunteers for disaster-response activities. */
public class Volunteer extends BaseEntity {

    private String fullName;
    private String contactNumber;
    private String email;
    private Long userId;
    private String location;
    private String skills;
    private VolunteerAvailability availability =
            VolunteerAvailability.AVAILABLE;
    private EmergencyRole emergencyRole;
    private int maxWorkload = 2;

    public Volunteer() {
        super();
    }

    public Volunteer(String fullName, String contactNumber,
                     String location) {
        super();
        this.fullName = fullName;
        this.contactNumber = contactNumber;
        this.location = location;
    }

    @Override
    public String getDetails() {
        String avail = availability == null
                ? "?" : availability.getLabel();
        return fullName + " [" + avail + "] " + location;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public VolunteerAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(VolunteerAvailability availability) {
        this.availability = availability;
    }

    public EmergencyRole getEmergencyRole() {
        return emergencyRole;
    }

    public void setEmergencyRole(EmergencyRole emergencyRole) {
        this.emergencyRole = emergencyRole;
    }

    public int getMaxWorkload() {
        return maxWorkload;
    }

    public void setMaxWorkload(int maxWorkload) {
        this.maxWorkload = maxWorkload;
    }
}
