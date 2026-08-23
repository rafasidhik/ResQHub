package com.resqhub.model;

/**
 * Output of the Rescue Priority Algorithm.
 * weight gives natural ordering (higher = more urgent) for
 * sorting pending requests in the operations queue.
 */
public enum PriorityLevel {
    CRITICAL("Critical", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1);

    private final String label;
    private final int weight;

    PriorityLevel(String label, int weight) {
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
