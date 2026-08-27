package com.resqhub.model;

/** notification_type column - the module/event the alert relates to. */
public enum NotificationType {
    CRITICAL_RESCUE("Critical Rescue"),
    LOW_STOCK("Low Stock"),
    ASSIGNMENT("Assignment"),
    SYSTEM("System");

    private final String label;

    NotificationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
