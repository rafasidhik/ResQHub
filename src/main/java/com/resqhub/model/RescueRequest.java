package com.resqhub.model;

import java.time.LocalDateTime;

public class RescueRequest {
    private Long id;
    private String requestNumber;
    private Long victimId;
    private String victimName;
    private Long disasterId;
    private String disasterName;
    private Long reportedByUserId;
    private String reportedByUsername;
    private String location;
    private String description;
    private String contactNumber;
    private int peopleCount = 1;
    private Urgency urgency = Urgency.MEDIUM;
    private VictimPriority calculatedPriority = VictimPriority.MEDIUM;
    private RescueRequestStatus status = RescueRequestStatus.PENDING;
    private String activeTeamName; // Joined from rescue_assignments / rescue_teams
    private Long activeTeamId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RescueRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public Long getVictimId() {
        return victimId;
    }

    public void setVictimId(Long victimId) {
        this.victimId = victimId;
    }

    public String getVictimName() {
        return victimName;
    }

    public void setVictimName(String victimName) {
        this.victimName = victimName;
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

    public Long getReportedByUserId() {
        return reportedByUserId;
    }

    public void setReportedByUserId(Long reportedByUserId) {
        this.reportedByUserId = reportedByUserId;
    }

    public String getReportedByUsername() {
        return reportedByUsername;
    }

    public void setReportedByUsername(String reportedByUsername) {
        this.reportedByUsername = reportedByUsername;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(int peopleCount) {
        this.peopleCount = peopleCount;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public VictimPriority getCalculatedPriority() {
        return calculatedPriority;
    }

    public void setCalculatedPriority(VictimPriority calculatedPriority) {
        this.calculatedPriority = calculatedPriority;
    }

    public RescueRequestStatus getStatus() {
        return status;
    }

    public void setStatus(RescueRequestStatus status) {
        this.status = status;
    }

    public String getActiveTeamName() {
        return activeTeamName;
    }

    public void setActiveTeamName(String activeTeamName) {
        this.activeTeamName = activeTeamName;
    }

    public Long getActiveTeamId() {
        return activeTeamId;
    }

    public void setActiveTeamId(Long activeTeamId) {
        this.activeTeamId = activeTeamId;
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
