package com.resqhub.dto;

import com.resqhub.model.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class VolunteerAssignmentDto {
    private Long id;

    @NotNull(message = "Volunteer is required")
    private Long volunteerId;

    @NotNull(message = "Disaster event is required")
    private Long disasterId;

    @NotBlank(message = "Task title is required")
    @Size(min = 3, max = 150, message = "Task title must be between 3 and 150 characters")
    private String taskTitle;

    @NotBlank(message = "Task description and instructions are required")
    private String description;

    @NotBlank(message = "Assignment location is required")
    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    private TaskPriority priority = TaskPriority.MEDIUM;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime expectedCompletion;

    private String progressNotes;

    public VolunteerAssignmentDto() {
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

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
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

    public LocalDateTime getExpectedCompletion() {
        return expectedCompletion;
    }

    public void setExpectedCompletion(LocalDateTime expectedCompletion) {
        this.expectedCompletion = expectedCompletion;
    }

    public String getProgressNotes() {
        return progressNotes;
    }

    public void setProgressNotes(String progressNotes) {
        this.progressNotes = progressNotes;
    }
}
