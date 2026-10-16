package com.resqhub.model;

public enum TaskPriority {
    LOW("Low Priority"),
    MEDIUM("Medium Priority"),
    HIGH("High Priority");

    private final String displayName;

    TaskPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
