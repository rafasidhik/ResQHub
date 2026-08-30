package com.resqhub.model;

/** Operational lifecycle of a rescue team during an operation. */
public enum TeamOperationalStatus {
    STANDBY("Standby"),
    ASSIGNED("Assigned"),
    EN_ROUTE("En Route"),
    ON_MISSION("On Mission"),
    RETURNING("Returning"),
    OPERATION_COMPLETED("Operation Completed"),
    INACTIVE("Inactive");

    private final String label;

    TeamOperationalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
