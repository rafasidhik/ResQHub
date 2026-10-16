package com.resqhub.model;

public enum NotificationType {
    RESCUE("Rescue Operations"),
    SHELTER("Shelter & Relief Camps"),
    VOLUNTEER("Volunteer Missions"),
    INVENTORY("Supplies & Inventory"),
    DISASTER("Disaster Alerts"),
    DONATION("Donations & Aid"),
    SYSTEM("System Announcement");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
