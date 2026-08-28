package com.resqhub.model;

/**
 * hospitals.status - the hospital's current operational ability to
 * receive emergency patients (spec section 9).
 */
public enum HospitalStatus {
    AVAILABLE("Available"),
    LIMITED_CAPACITY("Limited Capacity"),
    FULL("Full"),
    INACTIVE("Inactive"),
    EMERGENCY_ONLY("Emergency Only");

    private final String label;

    HospitalStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True when the hospital can still accept new patients. */
    public boolean isAccepting() {
        return this == AVAILABLE || this == LIMITED_CAPACITY
                || this == EMERGENCY_ONLY;
    }
}
