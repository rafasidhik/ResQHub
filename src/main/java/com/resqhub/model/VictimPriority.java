package com.resqhub.model;

public enum VictimPriority {
    LOW("Low", "badge-low"),
    MEDIUM("Medium", "badge-medium"),
    HIGH("High", "badge-high"),
    CRITICAL("Critical", "badge-critical");

    private final String displayName;
    private final String badgeClass;

    VictimPriority(String displayName, String badgeClass) {
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
