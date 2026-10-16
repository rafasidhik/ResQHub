package com.resqhub.model;

public enum VolunteerApprovalStatus {
    PENDING("Pending Verification"),
    APPROVED("Approved & Vetted"),
    REJECTED("Rejected / Disqualified");

    private final String displayName;

    VolunteerApprovalStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
