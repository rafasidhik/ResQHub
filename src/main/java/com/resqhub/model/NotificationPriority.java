package com.resqhub.model;

/** notification_priority column - how urgent an alert is. */
public enum NotificationPriority {
    CRITICAL("Critical"),
    WARNING("Warning"),
    INFO("Information");

    private final String label;

    NotificationPriority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
