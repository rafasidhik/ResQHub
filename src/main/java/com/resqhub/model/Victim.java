package com.resqhub.model;

import java.time.LocalDateTime;

public class Victim {
    private Long id;
    private String referenceNumber;
    private Long disasterId;
    private String disasterName; // Joined from disasters table
    private String fullName;
    private Integer age;
    private String contactNumber;
    private String location;
    private int affectedFamilyMembers = 1;
    private String accessibilityNeeds;
    private String currentSituation;
    private RescueStatus rescueStatus = RescueStatus.AWAITING_RESCUE;
    private VictimPriority priority = VictimPriority.MEDIUM;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Victim() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public String getDisasterName() {
        return disasterName;
    }

    public void setDisasterName(String disasterName) {
        this.disasterName = disasterName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getAffectedFamilyMembers() {
        return affectedFamilyMembers;
    }

    public void setAffectedFamilyMembers(int affectedFamilyMembers) {
        this.affectedFamilyMembers = affectedFamilyMembers;
    }

    public String getAccessibilityNeeds() {
        return accessibilityNeeds;
    }

    public void setAccessibilityNeeds(String accessibilityNeeds) {
        this.accessibilityNeeds = accessibilityNeeds;
    }

    public String getCurrentSituation() {
        return currentSituation;
    }

    public void setCurrentSituation(String currentSituation) {
        this.currentSituation = currentSituation;
    }

    public RescueStatus getRescueStatus() {
        return rescueStatus;
    }

    public void setRescueStatus(RescueStatus rescueStatus) {
        this.rescueStatus = rescueStatus;
    }

    public VictimPriority getPriority() {
        return priority;
    }

    public void setPriority(VictimPriority priority) {
        this.priority = priority;
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
