package com.resqhub.model;

import java.time.LocalDateTime;

public class VolunteerAssignment {
    private Long id;
    private Long volunteerId;
    private String volunteerName;
    private String volunteerIdentifier;
    private Long disasterId;
    private String disasterName;
    private String taskTitle;
    private String description;
    private String location;
    private TaskPriority priority = TaskPriority.MEDIUM;
    private LocalDateTime startTime;
    private LocalDateTime expectedCompletion;
    private VolunteerAssignmentStatus status = VolunteerAssignmentStatus.PENDING;
    private String progressNotes;
    private Long assignedByUserId;
    private String assignedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VolunteerAssignment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getVolunteerName() {
        return volunteerName;
    }

    public void setVolunteerName(String volunteerName) {
        this.volunteerName = volunteerName;
    }

    public String getVolunteerIdentifier() {
        return volunteerIdentifier;
    }

    public void setVolunteerIdentifier(String volunteerIdentifier) {
        this.volunteerIdentifier = volunteerIdentifier;
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

    public String getTaskTitle() {
        return taskTitle;
    }

    public void setTaskTitle(String taskTitle) {
        this.taskTitle = taskTitle;
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

    public TaskPriority getPriority() {
        return priority;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
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

    public VolunteerAssignmentStatus getStatus() {
        return status;
    }

    public void setStatus(VolunteerAssignmentStatus status) {
        this.status = status;
    }

    public String getProgressNotes() {
        return progressNotes;
    }

    public void setProgressNotes(String progressNotes) {
        this.progressNotes = progressNotes;
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
