package com.resqhub.model;

public enum AllocationStatus {
    RECOMMENDED("Recommended", "badge-pending"),
    CONFIRMED("Confirmed", "badge-completed"),
    REJECTED("Rejected", "badge-cancelled"),
    CANCELLED("Cancelled", "badge-cancelled");

    private final String displayName;
    private final String badgeClass;

    AllocationStatus(String displayName, String badgeClass) {
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
