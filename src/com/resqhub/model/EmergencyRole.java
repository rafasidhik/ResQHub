package com.resqhub.model;

/** Emergency role a volunteer is associated with. */
public enum EmergencyRole {
    MEDICAL("Medical support"),
    SHELTER("Shelter support"),
    FOOD("Food distribution"),
    RESCUE("Rescue support"),
    COMMUNICATION("Communication"),
    TRANSPORT("Resource handling"),
    GENERAL("General");

    private final String label;

    EmergencyRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
