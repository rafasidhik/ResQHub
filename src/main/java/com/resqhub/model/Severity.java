package com.resqhub.model;

public enum Severity {
    LOW("Low", "badge-low"),
    MODERATE("Moderate", "badge-medium"),
    HIGH("High", "badge-high"),
    CRITICAL("Critical", "badge-critical");

    private final String displayName;
    private final String badgeClass;

    Severity(String displayName, String badgeClass) {
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
