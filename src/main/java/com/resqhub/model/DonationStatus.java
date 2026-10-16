package com.resqhub.model;

public enum DonationStatus {
    SUBMITTED("Submitted / Under Review"),
    VERIFIED("Verified by Officer"),
    RECEIVED("Received & Stored in Warehouse"),
    ALLOCATED("Allocated to Field Mission"),
    CANCELLED("Cancelled");

    private final String displayName;

    DonationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
