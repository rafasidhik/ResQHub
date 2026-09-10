package com.resqhub.model;

/** Blood request lifecycle status (spec: Blood Request Status Tracking). */
public enum BloodRequestStatus {
    PENDING("Pending"),
    MATCHING_DONORS("Matching Donors"),
    DONOR_FOUND("Donor Found"),
    BLOOD_COLLECTED("Blood Collected"),
    FULFILLED("Fulfilled"),
    CANCELLED("Cancelled");

    private final String label;

    BloodRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isOpen() {
        return this != FULFILLED && this != CANCELLED;
    }
}
