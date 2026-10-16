package com.resqhub.model;

public enum ShelterStatus {
    AVAILABLE("Available", "badge-active"),
    NEARLY_FULL("Nearly Full", "badge-high"),
    FULL("Full", "badge-critical"),
    TEMPORARILY_CLOSED("Temporarily Closed", "badge-cancelled");

    private final String displayName;
    private final String badgeClass;

    ShelterStatus(String displayName, String badgeClass) {
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
