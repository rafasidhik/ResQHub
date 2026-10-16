package com.resqhub.model;

public enum DonationType {
    MONETARY("Monetary Relief Fund"),
    MATERIAL("Physical Relief Supplies");

    private final String displayName;

    DonationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
