package com.resqhub.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A cash or material donation received for disaster-response operations.
 * Amounts and quantities are mutually exclusive based on donation type.
 */
public class Donation extends BaseEntity {

    private Long donorId;
    private DonationType donationType;
    private BigDecimal amount;
    private String materialName;
    private Integer quantity;
    private String description;
    private DonationStatus status = DonationStatus.RECEIVED;
    private LocalDateTime donatedAt;

    public Donation() {
        super();
    }

    @Override
    public String getDetails() {
        String what;
        if (donationType == DonationType.CASH) {
            what = "CASH \u20B9" + (amount == null ? "0" : amount);
        } else {
            what = materialName + " x"
                    + (quantity == null ? "?" : quantity);
        }
        return what + " [" + (status == null
                ? "?" : status.getLabel()) + "]";
    }

    public Long getDonorId() {
        return donorId;
    }

    public void setDonorId(Long donorId) {
        this.donorId = donorId;
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

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DonationStatus getStatus() {
        return status;
    }

    public void setStatus(DonationStatus status) {
        this.status = status;
    }

    public LocalDateTime getDonatedAt() {
        return donatedAt;
    }

    public void setDonatedAt(LocalDateTime donatedAt) {
        this.donatedAt = donatedAt;
    }
}
