package com.resqhub.model;

public enum ResourceAllocationStatus {
    PLANNED("Planned"),
    APPROVED("Approved"),
    DISPATCHED("Dispatched / In Transit"),
    DELIVERED("Delivered / Received"),
    CANCELLED("Cancelled");

    private final String displayName;

    ResourceAllocationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
