package com.resqhub.model;

/** donor_type column for donors. */
public enum DonorType {
    INDIVIDUAL("Individual"),
    ORGANIZATION("Organization"),
    COMPANY("Company");

    private final String label;

    DonorType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
