package com.resqhub.model;

public enum RescueStatus {
    AWAITING_RESCUE("Awaiting Rescue", "badge-critical"),
    ASSIGNED("Team Assigned", "badge-in-progress"),
    RESCUED("Rescued", "badge-completed"),
    SHELTERED("Sheltered", "badge-active"),
    ACCOUNTED_FOR("Accounted For", "badge-cancelled");

    private final String displayName;
    private final String badgeClass;

    RescueStatus(String displayName, String badgeClass) {
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
