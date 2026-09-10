package com.resqhub.model;

/** Donor availability status (spec: Donor Availability). */
public enum DonorAvailability {
    AVAILABLE("Available"),
    UNAVAILABLE("Unavailable"),
    DEFERRED("Deferred"),
    PENDING("Pending");

    private final String label;

    DonorAvailability(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
