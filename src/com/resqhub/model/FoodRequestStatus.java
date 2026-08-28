package com.resqhub.model;

/**
 * Lifecycle of a food distribution request (spec section 10). The
 * PARTIALLY_FULFILLED status is used when only part of the required
 * food could be allocated (spec section 11).
 */
public enum FoodRequestStatus {
    PENDING("Pending"),
    APPROVED("Approved"),
    ALLOCATED("Allocated"),
    PARTIALLY_FULFILLED("Partially Fulfilled"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    FoodRequestStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True for statuses that are still open / awaiting fulfilment. */
    public boolean isOpen() {
        return this != COMPLETED && this != CANCELLED;
    }
}
