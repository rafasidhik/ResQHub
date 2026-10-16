package com.resqhub.model;

public enum ResidentStatus {
    ACTIVE("Checked In / Active", "badge-active"),
    CHECKED_OUT("Checked Out", "badge-cancelled");

    private final String displayName;
    private final String badgeClass;

    ResidentStatus(String displayName, String badgeClass) {
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
