package com.resqhub.model;

public enum VolunteerAvailability {
    AVAILABLE("Available for Deployment"),
    ASSIGNED("Assigned on Active Task"),
    UNAVAILABLE("Temporarily Unavailable");

    private final String displayName;

    VolunteerAvailability(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
