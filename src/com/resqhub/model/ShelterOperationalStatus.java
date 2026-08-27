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
}
