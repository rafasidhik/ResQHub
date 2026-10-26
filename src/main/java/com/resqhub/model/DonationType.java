package com.resqhub.model;

/** donation_type column for donations - cash or material. */
public enum DonationType {
    CASH("Cash"),
    MATERIAL("Material");

    private final String label;

    DonationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
