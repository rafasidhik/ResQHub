package com.resqhub.model;

public enum Role {
    ADMIN("Administrator"),
    RESCUE_OFFICER("Rescue Officer"),
    CAMP_MANAGER("Camp Manager"),
    VOLUNTEER("Volunteer"),
    CITIZEN("Citizen");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
