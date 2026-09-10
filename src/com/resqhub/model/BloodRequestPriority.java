package com.resqhub.model;

/** Blood request urgency priority (spec: Blood Request Priority). */
public enum BloodRequestPriority {
    CRITICAL("Critical", 0),
    HIGH("High", 1),
    MEDIUM("Medium", 2),
    LOW("Low", 3);

    private final String label;
    private final int order;

    BloodRequestPriority(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String getLabel() {
        return label;
    }

    public int getOrder() {
        return order;
    }
}
