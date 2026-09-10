package com.resqhub.model;

/**
 * Who a food distribution request is serving. A single request can
 * target individual victims, whole families, shelter occupants or a
 * general group of affected people (spec section 2).
 */
public enum BeneficiaryType {
    VICTIM("Individual Victim"),
    FAMILY("Family"),
    SHELTER("Shelter Occupants"),
    GROUP("Group of Affected People");

    private final String label;

    BeneficiaryType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
