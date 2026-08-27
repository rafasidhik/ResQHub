package com.resqhub.model;

/** volunteer_availability column for volunteers. */
public enum VolunteerAvailability {
    AVAILABLE("Available"),
    BUSY("Busy"),
    UNAVAILABLE("Unavailable");

    private final String label;

    VolunteerAvailability(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
