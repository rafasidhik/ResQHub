package com.resqhub.model;

/** rescue_assignments.assignment_status column. */
public enum AssignmentStatus {
    ASSIGNED("Assigned"),
    EN_ROUTE("En Route"),
    ON_SITE("On Site"),
    COMPLETED("Completed"),
    ABORTED("Aborted");

    private final String label;

    AssignmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
