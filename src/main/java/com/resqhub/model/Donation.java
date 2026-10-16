package com.resqhub.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Donation {
    private Long id;
    private String donationReference;
    private String donorName;
    private String donorEmail;
    private String donorPhone;
    private DonationType donationType = DonationType.MONETARY;
    private BigDecimal amount;
    private Long itemId;
    private String itemName;
    private String itemUnit;
    private Integer itemQuantity;
    private LocalDate donationDate = LocalDate.now();
    private DonationStatus status = DonationStatus.SUBMITTED;
    private String notes;
    private Long verifiedByUserId;
    private String verifiedByUsername;
    private boolean inventoryAdded = false;
    private LocalDateTime createdAt;

    public Donation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDonationReference() {
        return donationReference;
    }

    public void setDonationReference(String donationReference) {
        this.donationReference = donationReference;
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

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemUnit() {
        return itemUnit;
    }

    public void setItemUnit(String itemUnit) {
        this.itemUnit = itemUnit;
    }

    public Integer getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(Integer itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    public LocalDate getDonationDate() {
        return donationDate;
    }

    public void setDonationDate(LocalDate donationDate) {
        this.donationDate = donationDate;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getVerifiedByUserId() {
        return verifiedByUserId;
    }

    public void setVerifiedByUserId(Long verifiedByUserId) {
        this.verifiedByUserId = verifiedByUserId;
    }

    public String getVerifiedByUsername() {
        return verifiedByUsername;
    }

    public void setVerifiedByUsername(String verifiedByUsername) {
        this.verifiedByUsername = verifiedByUsername;
    }

    public boolean isInventoryAdded() {
        return inventoryAdded;
    }

    public void setInventoryAdded(boolean inventoryAdded) {
        this.inventoryAdded = inventoryAdded;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
