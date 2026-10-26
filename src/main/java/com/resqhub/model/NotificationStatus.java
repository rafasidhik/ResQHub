package com.resqhub.model;

/** notification_status column - whether the recipient has seen it. */
public enum NotificationStatus {
    UNREAD("Unread"),
    READ("Read"),
    ARCHIVED("Archived");

    private final String label;

    NotificationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
