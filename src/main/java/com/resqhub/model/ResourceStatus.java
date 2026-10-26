package com.resqhub.model;

/**
 * Availability status of an inventory resource, derived from how the
 * current quantity compares with its minimum stock level:
 *
 *   quantity = 0                -> OUT_OF_STOCK
 *   quantity <  minimum level   -> LOW_STOCK
 *   otherwise                   -> AVAILABLE
 */
public enum ResourceStatus {
    AVAILABLE("Available"),
    LOW_STOCK("Low Stock"),
    OUT_OF_STOCK("Out of Stock");

    private final String label;

    ResourceStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
