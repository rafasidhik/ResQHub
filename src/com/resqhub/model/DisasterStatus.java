package com.resqhub.model;

/** disasters.status column. */
public enum DisasterStatus {
    REPORTED("Reported"),
    ACTIVE("Active"),
    CONTAINED("Contained"),
    RESOLVED("Resolved");

    private final String label;

    DisasterStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
