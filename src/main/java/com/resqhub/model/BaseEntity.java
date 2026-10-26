package com.resqhub.model;

import java.time.LocalDateTime;

/**
 * Abstract root of every persistent entity.
 * Holds the primary key and audit timestamps shared by all tables.
 * Subclasses must implement getDetails() - a human-readable summary
 * that enables dynamic method dispatch when displaying mixed lists.
 */
public abstract class BaseEntity {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected BaseEntity() {
    }

    public abstract String getDetails();

    @Override
    public String toString() {
        String type = getClass().getSimpleName();
        if (id == null) {
            return type + " (unsaved)";
        }
        return type + " #" + id + ": " + getDetails();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
