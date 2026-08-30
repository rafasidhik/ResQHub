package com.resqhub.model;

/**
 * Lifecycle status of a shelter allocation record. ACTIVE and CHECKED_IN
 * currently count toward the shelter's occupancy; PENDING is a
 * reservation that does NOT yet occupy space; COMPLETED / CANCELLED /
 * RELEASED are terminal states that free the occupants.
 */
public enum ShelterAllocationStatus {
    PENDING("Pending"),
    ACTIVE("Active"),
    CHECKED_IN("Checked In"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
    RELEASED("Released");

    private final String label;

    ShelterAllocationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True when the occupants currently count toward shelter occupancy. */
    public boolean isOccupying() {
        return this == ACTIVE || this == CHECKED_IN;
    }
}
