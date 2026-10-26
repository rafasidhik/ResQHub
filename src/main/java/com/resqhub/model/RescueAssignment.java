package com.resqhub.model;

import java.time.LocalDateTime;

/**
 * Junction-table row linking a rescue request to a rescue team (M:N).
 */
public class RescueAssignment extends BaseEntity {

    private Long rescueRequestId;
    private Long rescueTeamId;
    private Long assignedBy;
    private AssignmentStatus assignmentStatus = AssignmentStatus.ASSIGNED;
    private String notes;
    private LocalDateTime completedAt;   // set when COMPLETED

    @Override
    public String getDetails() {
        String statusLabel = assignmentStatus == null ? "?" : assignmentStatus.getLabel();
        return "request #" + rescueRequestId + " -> team #" + rescueTeamId
                + " (" + statusLabel + ")";
    }

    public Long getRescueRequestId() {
        return rescueRequestId;
    }

    public void setRescueRequestId(Long rescueRequestId) {
        this.rescueRequestId = rescueRequestId;
    }

    public Long getRescueTeamId() {
        return rescueTeamId;
    }

    public void setRescueTeamId(Long rescueTeamId) {
        this.rescueTeamId = rescueTeamId;
    }

    public Long getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(Long assignedBy) {
        this.assignedBy = assignedBy;
    }

    public AssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
