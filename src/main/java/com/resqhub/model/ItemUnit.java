package com.resqhub.model;

public enum ItemUnit {
    KG("Kilograms (kg)"),
    LITERS("Liters (L)"),
    BOXES("Boxes / Cartons"),
    PIECES("Pieces / Units"),
    SETS("Sets / Kits");

    private final String displayName;

    ItemUnit(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
