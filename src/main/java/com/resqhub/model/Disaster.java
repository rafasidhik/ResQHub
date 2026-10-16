package com.resqhub.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Disaster {
    private Long id;
    private String name;
    private DisasterType disasterType;
    private String description;
    private String location;
    private Severity severity;
    private DisasterStatus status;
    private LocalDate startDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Derived operational impact counters
    private int victimCount;
    private int rescueRequestCount;
    private int pendingRequestCount;
    private int shelterResidentCount;

    public Disaster() {
    }

    public Disaster(Long id, String name, DisasterType disasterType, String description,
                    String location, Severity severity, DisasterStatus status, LocalDate startDate) {
        this.id = id;
        this.name = name;
        this.disasterType = disasterType;
        this.description = description;
        this.location = location;
        this.severity = severity;
        this.status = status;
        this.startDate = startDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DisasterType getDisasterType() {
        return disasterType;
    }

    public void setDisasterType(DisasterType disasterType) {
        this.disasterType = disasterType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public DisasterStatus getStatus() {
        return status;
    }

    public void setStatus(DisasterStatus status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
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

    public int getVictimCount() {
        return victimCount;
    }

    public void setVictimCount(int victimCount) {
        this.victimCount = victimCount;
    }

    public int getRescueRequestCount() {
        return rescueRequestCount;
    }

    public void setRescueRequestCount(int rescueRequestCount) {
        this.rescueRequestCount = rescueRequestCount;
    }

    public int getPendingRequestCount() {
        return pendingRequestCount;
    }

    public void setPendingRequestCount(int pendingRequestCount) {
        this.pendingRequestCount = pendingRequestCount;
    }

    public int getShelterResidentCount() {
        return shelterResidentCount;
    }

    public void setShelterResidentCount(int shelterResidentCount) {
        this.shelterResidentCount = shelterResidentCount;
    }

    public boolean isActive() {
        return this.status == DisasterStatus.ACTIVE;
    }
}
