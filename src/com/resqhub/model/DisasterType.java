package com.resqhub.model;

/** disasters.disaster_type column. */
public enum DisasterType {
    FLOOD("Flood"),
    EARTHQUAKE("Earthquake"),
    CYCLONE("Cyclone"),
    LANDSLIDE("Landslide"),
    FIRE("Fire"),
    BUILDING_COLLAPSE("Building Collapse"),
    INDUSTRIAL_ACCIDENT("Industrial Accident"),
    ACCIDENT("Accident"),
    EPIDEMIC("Epidemic"),
    OTHER("Other");

    private final String label;

    DisasterType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
