package com.resqhub.model;

public enum Urgency {
    LOW("Low", 10),
    MEDIUM("Medium", 25),
    HIGH("High", 50),
    CRITICAL("Critical", 80);

    private final String displayName;
    private final int baseWeight;

    Urgency(String displayName, int baseWeight) {
        this.displayName = displayName;
        this.baseWeight = baseWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBaseWeight() {
        return baseWeight;
    }
}
