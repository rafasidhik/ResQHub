package com.resqhub.model;

import java.time.LocalDateTime;

public class RescueTeam {
    private Long id;
    private String teamIdentifier;
    private String name;
    private String teamLeader;
    private String contactNumber;
    private String skills;
    private String operatingArea;
    private int capacity = 10;
    private TeamAvailability availabilityStatus = TeamAvailability.AVAILABLE;
    private String status = "ACTIVE"; // ACTIVE, INACTIVE
    private int memberCount;
    private Long activeAssignmentId;
    private String activeMissionLocation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RescueTeam() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTeamIdentifier() {
        return teamIdentifier;
    }

    public void setTeamIdentifier(String teamIdentifier) {
        this.teamIdentifier = teamIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeamLeader() {
        return teamLeader;
    }

    public void setTeamLeader(String teamLeader) {
        this.teamLeader = teamLeader;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getOperatingArea() {
        return operatingArea;
    }

    public void setOperatingArea(String operatingArea) {
        this.operatingArea = operatingArea;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public TeamAvailability getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(TeamAvailability availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public Long getActiveAssignmentId() {
        return activeAssignmentId;
    }

    public void setActiveAssignmentId(Long activeAssignmentId) {
        this.activeAssignmentId = activeAssignmentId;
    }

    public String getActiveMissionLocation() {
        return activeMissionLocation;
    }

    public void setActiveMissionLocation(String activeMissionLocation) {
        this.activeMissionLocation = activeMissionLocation;
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

    public boolean isAvailable() {
        return this.availabilityStatus == TeamAvailability.AVAILABLE && "ACTIVE".equalsIgnoreCase(this.status);
    }
}
