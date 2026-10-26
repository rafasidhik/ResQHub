package com.resqhub.model;

/** donation_status column for donations - operational lifecycle. */
public enum DonationStatus {
    RECEIVED("Received"),
    ALLOCATED("Allocated"),
    PARTIALLY_DISTRIBUTED("Partially Distributed"),
    DISTRIBUTED("Distributed");

    private final String label;

    DonationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
