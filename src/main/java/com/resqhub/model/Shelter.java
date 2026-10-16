package com.resqhub.model;

import java.time.LocalDateTime;

public class Shelter {
    private Long id;
    private String shelterIdentifier;
    private String name;
    private String address;
    private String locationDescription;
    private String contactPerson;
    private String contactNumber;
    private int totalCapacity = 50;
    private int currentOccupancy = 0;
    private ShelterStatus status = ShelterStatus.AVAILABLE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Shelter() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShelterIdentifier() {
        return shelterIdentifier;
    }

    public void setShelterIdentifier(String shelterIdentifier) {
        this.shelterIdentifier = shelterIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public int getCurrentOccupancy() {
        return currentOccupancy;
    }

    public void setCurrentOccupancy(int currentOccupancy) {
        this.currentOccupancy = currentOccupancy;
    }

    public ShelterStatus getStatus() {
        return status;
    }

    public void setStatus(ShelterStatus status) {
        this.status = status;
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

    public int getRemainingCapacity() {
        return Math.max(0, this.totalCapacity - this.currentOccupancy);
    }

    public int getOccupancyPercentage() {
        if (this.totalCapacity <= 0) return 100;
        return Math.min(100, (int) Math.round(((double) this.currentOccupancy / this.totalCapacity) * 100));
    }

    public boolean isAvailableFor(int peopleCount) {
        return this.status != ShelterStatus.TEMPORARILY_CLOSED && getRemainingCapacity() >= peopleCount;
    }
}
