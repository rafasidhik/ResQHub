package com.resqhub.model;

public enum RescueAssignmentStatus {
    ASSIGNED("Assigned", "badge-in-progress"),
    IN_PROGRESS("In Progress", "badge-primary"),
    COMPLETED("Completed", "badge-completed"),
    CANCELLED("Cancelled", "badge-cancelled");

    private final String displayName;
    private final String badgeClass;

    RescueAssignmentStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
