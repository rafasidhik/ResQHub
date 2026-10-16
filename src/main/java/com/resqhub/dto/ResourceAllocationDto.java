package com.resqhub.dto;

import com.resqhub.model.ResourceAllocationStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResourceAllocationDto {
    private Long id;

    @NotNull(message = "Resource item is required")
    private Long itemId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    @NotNull(message = "Disaster event is required")
    private Long disasterId;

    private Long shelterId;

    @NotBlank(message = "Destination location is required")
    @Size(max = 200, message = "Destination must not exceed 200 characters")
    private String destinationLocation;

    @NotBlank(message = "Purpose is required")
    @Size(max = 255, message = "Purpose must not exceed 255 characters")
    private String purpose;

    @NotBlank(message = "Responsible person / custodian is required")
    @Size(max = 100, message = "Responsible person must not exceed 100 characters")
    private String responsiblePerson;

    private ResourceAllocationStatus status = ResourceAllocationStatus.PLANNED;

    private String remarks;

    public ResourceAllocationDto() {
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

    public Long getShelterId() {
        return shelterId;
    }

    public void setShelterId(Long shelterId) {
        this.shelterId = shelterId;
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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
