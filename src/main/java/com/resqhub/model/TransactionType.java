package com.resqhub.model;

public enum TransactionType {
    RECEIVED("Stock Received / Donated"),
    ISSUED("Stock Issued / Dispatched"),
    ALLOCATED("Reserved for Allocation"),
    ADJUSTED("Inventory Audit Adjustment");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
