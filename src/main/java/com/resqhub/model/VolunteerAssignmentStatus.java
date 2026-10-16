package com.resqhub.model;

public enum VolunteerAssignmentStatus {
    PENDING("Pending Notification"),
    OFFERED("Task Offered to Volunteer"),
    ACCEPTED("Accepted / Scheduled"),
    DECLINED("Declined by Volunteer"),
    IN_PROGRESS("In Progress / On Site"),
    COMPLETED("Successfully Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    VolunteerAssignmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
