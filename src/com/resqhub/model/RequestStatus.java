package com.resqhub.model;

/** rescue_requests.status column. */
public enum RequestStatus {
    PENDING("Pending"),
    UNDER_REVIEW("Under Review"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    RESCUED("Rescued"),
    CANCELLED("Cancelled");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
