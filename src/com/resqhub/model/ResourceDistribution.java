package com.resqhub.model;

import java.time.LocalDateTime;

import com.resqhub.util.ValidationUtil;

/**
 * A record of resource distribution to a destination (shelter, victim,
 * disaster-affected area, rescue team, hospital or food-distribution
 * operation). Distribution validates available quantity before reducing
 * inventory and never lets stock fall below zero.
 */
public class ResourceDistribution extends BaseEntity {

    private Long resourceId;
    private int quantity;
    private String distributedTo;
    private DistributionDestination destination;
    private Long disasterId;
    private Long shelterId;
    private Long victimId;
    private String reason;
    private LocalDateTime distributedAt;
    private Long distributedBy;

    public ResourceDistribution() {
        super();
    }

    @Override
    public String getDetails() {
        return (destination == null ? "?" : destination.getLabel())
                + " " + (distributedTo == null ? "" : distributedTo)
                + " x" + quantity;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getDistributedTo() {
        return distributedTo;
    }

    public void setDistributedTo(String distributedTo) {
        this.distributedTo = ValidationUtil.clean(distributedTo);
    }

    public DistributionDestination getDestination() {
        return destination;
    }

    public void setDestination(DistributionDestination destination) {
        this.destination = destination;
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

    public Long getVictimId() {
        return victimId;
    }

    public void setVictimId(Long victimId) {
        this.victimId = victimId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = ValidationUtil.clean(reason);
    }

    public LocalDateTime getDistributedAt() {
        return distributedAt;
    }

    public void setDistributedAt(LocalDateTime distributedAt) {
        this.distributedAt = distributedAt;
    }

    public Long getDistributedBy() {
        return distributedBy;
    }

    public void setDistributedBy(Long distributedBy) {
        this.distributedBy = distributedBy;
    }
}
