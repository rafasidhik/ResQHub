package com.resqhub.model;

/** Per-donor match status against a blood request (spec: Donor Matching). */
public enum BloodMatchStatus {
    SUGGESTED("Suggested"),
    CONTACTED("Contacted"),
    CONFIRMED("Confirmed"),
    COLLECTED("Collected"),
    DECLINED("Declined"),
    CANCELLED("Cancelled");

    private final String label;

    BloodMatchStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isActive() {
        return this == SUGGESTED || this == CONTACTED
                || this == CONFIRMED;
    }
}
