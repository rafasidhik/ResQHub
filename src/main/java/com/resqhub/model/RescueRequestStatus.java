package com.resqhub.model;

public enum RescueRequestStatus {
    PENDING("Pending Verification", "badge-pending"),
    VERIFIED("Verified", "badge-info"),
    ASSIGNED("Team Assigned", "badge-in-progress"),
    IN_PROGRESS("Operation In Progress", "badge-primary"),
    COMPLETED("Rescue Completed", "badge-completed"),
    CANCELLED("Cancelled", "badge-cancelled");

    private final String displayName;
    private final String badgeClass;

    RescueRequestStatus(String displayName, String badgeClass) {
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
