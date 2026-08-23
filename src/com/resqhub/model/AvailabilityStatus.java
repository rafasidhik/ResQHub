package com.resqhub.model;

/** rescue_teams.availability_status column. */
public enum AvailabilityStatus {
    AVAILABLE("Available"),
    DEPLOYED("Deployed"),
    OFF_DUTY("Off Duty");

    private final String label;

    AvailabilityStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
