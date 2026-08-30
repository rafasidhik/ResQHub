package com.resqhub.model;

/** resources.category column - groups inventory items for easier
 *  searching, filtering and reporting. */
public enum ResourceCategory {
    FOOD("Food"),
    WATER("Water"),
    MEDICINE("Medicine"),
    CLOTHING("Clothing"),
    SHELTER_SUPPLIES("Shelter Supplies"),
    MEDICAL_SUPPLIES("Medical Supplies"),
    RESCUE_EQUIPMENT("Rescue Equipment"),
    OTHER("Other Emergency Supplies");

    private final String label;

    ResourceCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
