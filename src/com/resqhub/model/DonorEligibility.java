package com.resqhub.model;

/** Donor eligibility status (spec: Donor Eligibility Tracking). */
public enum DonorEligibility {
    ELIGIBLE("Eligible"),
    TEMPORARY_DEFERRED("Temporary Deferred"),
    PERMANENTLY_INELIGIBLE("Permanently Ineligible"),
    MEDICAL_HOLD("Medical Hold");

    private final String label;

    DonorEligibility(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean canDonate() {
        return this == ELIGIBLE;
    }
}
