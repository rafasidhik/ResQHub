package com.resqhub.model;

/** disasters.severity column. */
public enum DisasterSeverity {
    LOW("Low", 1),
    MODERATE("Moderate", 2),
    SEVERE("Severe", 3),
    CATASTROPHIC("Catastrophic", 4);

    private final String label;
    private final int weight;

    DisasterSeverity(String label, int weight) {
        this.label = label;
        this.weight = weight;
    }

    public String getLabel() {
        return label;
    }

    public int getWeight() {
        return weight;
    }
}
