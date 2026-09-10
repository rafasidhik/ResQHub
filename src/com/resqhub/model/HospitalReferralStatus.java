package com.resqhub.model;

/**
 * hospital_referrals.status - the lifecycle of an emergency victim referral
 * to a hospital (spec section 11).
 */
public enum HospitalReferralStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    ADMITTED("Admitted"),
    DISCHARGED("Discharged"),
    REJECTED("Rejected"),
    CANCELLED("Cancelled");

    private final String label;

    HospitalReferralStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** True when the referral still occupies / holds a bed commitment. */
    public boolean isOpen() {
        return this == PENDING || this == ACCEPTED || this == ADMITTED;
    }
}
