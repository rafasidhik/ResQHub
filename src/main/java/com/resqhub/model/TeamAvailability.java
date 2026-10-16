package com.resqhub.model;

public enum TeamAvailability {
    AVAILABLE("Available", "badge-active"),
    ASSIGNED("Assigned", "badge-in-progress"),
    ON_OPERATION("On Operation", "badge-primary"),
    UNAVAILABLE("Unavailable", "badge-cancelled");

    private final String displayName;
    private final String badgeClass;

    TeamAvailability(String displayName, String badgeClass) {
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
