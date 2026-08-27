package com.resqhub.model;

/**
 * Where a distributed resource was sent. Resource distribution can serve
 * shelters, victims, disaster-affected areas, rescue teams, hospitals or
 * food-distribution operations (spec section 10).
 */
public enum DistributionDestination {
    SHELTER("Shelter"),
    VICTIM("Victim"),
    DISASTER_AREA("Disaster-Affected Area"),
    RESCUE_TEAM("Rescue Team"),
    HOSPITAL("Hospital"),
    FOOD_DISTRIBUTION("Food Distribution"),
    OTHER("Other");

    private final String label;

    DistributionDestination(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
