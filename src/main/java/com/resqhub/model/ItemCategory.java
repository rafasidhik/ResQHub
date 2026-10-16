package com.resqhub.model;

public enum ItemCategory {
    FOOD("Food & Dry Rations"),
    DRINKING_WATER("Drinking Water"),
    CLOTHING("Clothing & Garments"),
    BLANKETS("Blankets & Bedding"),
    HYGIENE_KITS("Hygiene & Sanitation Kits"),
    RESCUE_EQUIPMENT("Rescue & Safety Equipment");

    private final String displayName;

    ItemCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
