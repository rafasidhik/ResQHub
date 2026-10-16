package com.resqhub.model;

import java.time.LocalDateTime;

public class ResourceAllocation {
    private Long id;
    private Long itemId;
    private String itemName;
    private int quantity;
    private Long disasterId;
    private String disasterName;
    private Long shelterId;
    private String shelterName;
    private String destinationLocation;
    private String purpose;
    private String responsiblePerson;
    private ResourceAllocationStatus status = ResourceAllocationStatus.PLANNED;
    private Long allocatedByUserId;
    private String allocatedByUsername;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ResourceAllocation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public String getDisasterName() {
        return disasterName;
    }

    public void setDisasterName(String disasterName) {
        this.disasterName = disasterName;
    }

    public Long getShelterId() {
        return shelterId;
    }

    public void setShelterId(Long shelterId) {
        this.shelterId = shelterId;
    }

    public String getShelterName() {
        return shelterName;
    }

    public void setShelterName(String shelterName) {
        this.shelterName = shelterName;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getResponsiblePerson() {
        return responsiblePerson;
    }

    public void setResponsiblePerson(String responsiblePerson) {
        this.responsiblePerson = responsiblePerson;
    }

    public ResourceAllocationStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceAllocationStatus status) {
        this.status = status;
    }

    public Long getAllocatedByUserId() {
        return allocatedByUserId;
    }

    public void setAllocatedByUserId(Long allocatedByUserId) {
        this.allocatedByUserId = allocatedByUserId;
    }

    public String getAllocatedByUsername() {
        return allocatedByUsername;
    }

    public void setAllocatedByUsername(String allocatedByUsername) {
        this.allocatedByUsername = allocatedByUsername;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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
