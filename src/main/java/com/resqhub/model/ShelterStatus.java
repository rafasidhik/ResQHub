package com.resqhub.model;

/** victims.shelter_status column (display flag; real linkage lives in Ameya's shelter_allocations). */
public enum ShelterStatus {
    NOT_SHELTERED("Not Sheltered"),
    IN_SHELTER("In Shelter"),
    RELOCATED("Relocated");

    private final String label;

    ShelterStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
