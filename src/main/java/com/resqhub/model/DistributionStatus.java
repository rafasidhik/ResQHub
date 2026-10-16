package com.resqhub.model;

public enum DistributionStatus {
    PLANNED("Planned"),
    COMPLETED("Completed / Distributed");

    private final String displayName;

    DistributionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
