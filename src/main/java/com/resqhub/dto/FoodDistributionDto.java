package com.resqhub.dto;

import com.resqhub.model.DistributionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class FoodDistributionDto {
    private Long id;

    private Long shelterId;

    @NotBlank(message = "Distribution location / relief post is required")
    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    @NotNull(message = "Food / water item is required")
    private Long itemId;

    @Min(value = 1, message = "Distributed quantity must be at least 1")
    private int quantity = 1;

    @Min(value = 1, message = "Recipient count must be at least 1")
    private int recipientCount = 1;

    @NotNull(message = "Distribution date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate distributionDate = LocalDate.now();

    private DistributionStatus status = DistributionStatus.PLANNED;

    public FoodDistributionDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShelterId() {
        return shelterId;
    }

    public void setShelterId(Long shelterId) {
        this.shelterId = shelterId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }

    public LocalDate getDistributionDate() {
        return distributionDate;
    }

    public void setDistributionDate(LocalDate distributionDate) {
        this.distributionDate = distributionDate;
    }

    public DistributionStatus getStatus() {
        return status;
    }

    public void setStatus(DistributionStatus status) {
        this.status = status;
    }
}
