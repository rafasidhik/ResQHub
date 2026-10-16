package com.resqhub.model;

import java.time.LocalDateTime;

public class RescueAssignment {
    private Long id;
    private Long requestId;
    private String requestNumber;
    private Long teamId;
    private String teamName;
    private String teamLeader;
    private Long assignedByUserId;
    private String assignedByUsername;
    private LocalDateTime startTime;
    private LocalDateTime expectedCompletion;
    private LocalDateTime actualCompletion;
    private RescueAssignmentStatus status = RescueAssignmentStatus.ASSIGNED;
    private String progressNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RescueAssignment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamLeader() {
        return teamLeader;
    }

    public void setTeamLeader(String teamLeader) {
        this.teamLeader = teamLeader;
    }

    public Long getAssignedByUserId() {
        return assignedByUserId;
    }

    public void setAssignedByUserId(Long assignedByUserId) {
        this.assignedByUserId = assignedByUserId;
    }

    public String getAssignedByUsername() {
        return assignedByUsername;
    }

    public void setAssignedByUsername(String assignedByUsername) {
        this.assignedByUsername = assignedByUsername;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getExpectedCompletion() {
        return expectedCompletion;
    }

    public void setExpectedCompletion(LocalDateTime expectedCompletion) {
        this.expectedCompletion = expectedCompletion;
    }

    public LocalDateTime getActualCompletion() {
        return actualCompletion;
    }

    public void setActualCompletion(LocalDateTime actualCompletion) {
        this.actualCompletion = actualCompletion;
    }

    public RescueAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(RescueAssignmentStatus status) {
        this.status = status;
    }

    public String getProgressNotes() {
        return progressNotes;
    }

    public void setProgressNotes(String progressNotes) {
        this.progressNotes = progressNotes;
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
