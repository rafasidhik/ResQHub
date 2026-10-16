package com.resqhub.model;

public enum DisasterStatus {
    ACTIVE("Active", "badge-active"),
    CONTAINED("Contained", "badge-pending"),
    CLOSED("Closed", "badge-completed");

    private final String displayName;
    private final String badgeClass;

    DisasterStatus(String displayName, String badgeClass) {
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
