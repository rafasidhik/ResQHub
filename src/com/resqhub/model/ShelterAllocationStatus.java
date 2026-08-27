package com.resqhub.model;

/**
 * Status of a shelter allocation record (a victim or family placed in
 * a shelter). ACTIVE means currently accommodated; RELEASED means the
 * allocation has been closed and the occupants no longer counted.
 */
public enum ShelterAllocationStatus {
    ACTIVE("Active"),
    RELEASED("Released");

    private final String label;

    ShelterAllocationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
