package com.resqhub.model;

/** victims.emergency_status column. */
public enum EmergencyStatus {
    SAFE("Safe"),
    INJURED("Injured"),
    CRITICAL("Critical"),
    MISSING("Missing");

    private final String label;

    EmergencyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
