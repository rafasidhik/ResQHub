package com.resqhub.model;

/** Task assignment status for a volunteer. */
public enum VolunteerTaskStatus {
    ASSIGNED("Assigned"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String label;

    VolunteerTaskStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
