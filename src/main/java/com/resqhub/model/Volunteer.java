package com.resqhub.model;

import java.time.LocalDateTime;

public class Volunteer {
    private Long id;
    private String volunteerIdentifier;
    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String operatingArea;
    private String skills;
    private int experienceYears = 0;
    private VolunteerAvailability availabilityStatus = VolunteerAvailability.AVAILABLE;
    private VolunteerApprovalStatus approvalStatus = VolunteerApprovalStatus.PENDING;
    private String emergencyContact;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Volunteer() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVolunteerIdentifier() {
        return volunteerIdentifier;
    }

    public void setVolunteerIdentifier(String volunteerIdentifier) {
        this.volunteerIdentifier = volunteerIdentifier;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOperatingArea() {
        return operatingArea;
    }

    public void setOperatingArea(String operatingArea) {
        this.operatingArea = operatingArea;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public VolunteerAvailability getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(VolunteerAvailability availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public VolunteerApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(VolunteerApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
