package com.resqhub.model;

import java.time.LocalDateTime;

/**
 * A distribution record showing how a donation was allocated to a
 * beneficiary, camp or relief operation.
 */
public class DonationDistribution extends BaseEntity {

    private Long donationId;
    private String distributedTo;
    private int quantity;
    private LocalDateTime distributedAt;
    private String description;

    public DonationDistribution() {
        super();
    }

    @Override
    public String getDetails() {
        return (distributedTo == null ? "?" : distributedTo)
                + " x" + quantity;
    }

    public Long getDonationId() {
        return donationId;
    }

    public void setDonationId(Long donationId) {
        this.donationId = donationId;
    }

    public String getDistributedTo() {
        return distributedTo;
    }

    public void setDistributedTo(String distributedTo) {
        this.distributedTo = distributedTo;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getDistributedAt() {
        return distributedAt;
    }

    public void setDistributedAt(LocalDateTime distributedAt) {
        this.distributedAt = distributedAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
