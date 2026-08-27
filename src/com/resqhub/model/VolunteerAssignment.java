package com.resqhub.model;

import java.time.LocalDateTime;

/** An emergency task assigned to a volunteer. */
public class VolunteerAssignment extends BaseEntity {

    private Long volunteerId;
    private String taskName;
    private String description;
    private String location;
    private int priority;
    private VolunteerTaskStatus status =
            VolunteerTaskStatus.ASSIGNED;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;

    public VolunteerAssignment() {
        super();
    }

    @Override
    public String getDetails() {
        String statusLabel = status == null ? "?" : status.getLabel();
        return taskName + " [" + statusLabel + "]";
    }

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public VolunteerTaskStatus getStatus() {
        return status;
    }

    public void setStatus(VolunteerTaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
