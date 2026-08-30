package com.resqhub.model;

import java.time.LocalDateTime;

/** A record of volunteer activity. */
public class VolunteerActivity extends BaseEntity {

    private Long volunteerId;
    private String activityType;
    private String description;
    private LocalDateTime activityTime;

    public VolunteerActivity() {
        super();
    }

    @Override
    public String getDetails() {
        return activityType + ": " + description;
    }

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(LocalDateTime activityTime) {
        this.activityTime = activityTime;
    }
}
