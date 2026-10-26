package com.resqhub.model;

/**
 * Operational status of a relief shelter/camp. Distinct from the
 * victim-facing {@link ShelterStatus} flag; this reflects whether the
 * shelter can currently accept new victims (Near Capacity / Full are
 * derived from occupancy but may also be hand-set).
 */
public enum ShelterOperationalStatus {
    ACTIVE("Active"),
    AVAILABLE("Available"),
    NEAR_CAPACITY("Near Capacity"),
    FULL("Full"),
    INACTIVE("Inactive"),
    CLOSED("Closed");

    private final String label;

    ShelterOperationalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True when the shelter can still accept new people (not full /
     *  inactive / closed). Used by smart allocation's suitability gate. */
    public boolean isAccepting() {
        return this == ACTIVE || this == AVAILABLE || this == NEAR_CAPACITY;
    }
}
