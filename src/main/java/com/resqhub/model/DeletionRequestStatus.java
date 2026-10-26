package com.resqhub.model;

/** Lifecycle of an account deletion request. */
public enum DeletionRequestStatus {

    PENDING("Pending"),
    APPROVED("Approved"),
    DENIED("Denied");

    private final String label;

    DeletionRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
