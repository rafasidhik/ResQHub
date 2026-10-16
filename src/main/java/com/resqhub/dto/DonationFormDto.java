package com.resqhub.dto;

import com.resqhub.model.DonationType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class DonationFormDto {
    @NotBlank(message = "Donor name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String donorName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please enter a valid email address")
    private String donorEmail;

    @Size(max = 30, message = "Phone number is too long")
    private String donorPhone;

    @NotNull(message = "Donation type is required")
    private DonationType donationType = DonationType.MONETARY;

    @DecimalMin(value = "1.00", message = "Minimum monetary donation is $1.00")
    private BigDecimal amount;

    private Long itemId;

    @Min(value = 1, message = "Material quantity must be at least 1")
    private Integer itemQuantity;

    private String notes;

    public DonationFormDto() {
    }

    public String getDonorName() {
        return donorName;
    }

    public void setDonorName(String donorName) {
        this.donorName = donorName;
    }

    public String getDonorEmail() {
        return donorEmail;
    }

    public void setDonorEmail(String donorEmail) {
        this.donorEmail = donorEmail;
    }

    public String getDonorPhone() {
        return donorPhone;
    }

    public void setDonorPhone(String donorPhone) {
        this.donorPhone = donorPhone;
    }

    public DonationType getDonationType() {
        return donationType;
    }

    public void setDonationType(DonationType donationType) {
        this.donationType = donationType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Integer getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(Integer itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
