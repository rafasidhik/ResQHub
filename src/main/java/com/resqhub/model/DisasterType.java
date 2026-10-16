package com.resqhub.model;

public enum DisasterType {
    FLOOD("Flood"),
    EARTHQUAKE("Earthquake"),
    LANDSLIDE("Landslide"),
    CYCLONE("Cyclone"),
    FIRE("Fire");

    private final String displayName;

    DisasterType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
