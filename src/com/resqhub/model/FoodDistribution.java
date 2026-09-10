package com.resqhub.model;

import java.time.LocalDateTime;

import com.resqhub.util.ValidationUtil;

/**
 * A single food distribution event against a request (spec sections
 * 14, 15). Records the actual quantity handed out, the people served
 * and when / where / by whom, providing the distribution history and
 * feeding back into the request's quantity tracking.
 */
public class FoodDistribution extends BaseEntity {

    private Long requestId;
    private Long resourceId;
    private int quantity;
    private int beneficiariesServed;
    private String distributedTo;
    private String location;
    private LocalDateTime distributedAt;
    private Long distributedBy;
    private String note;

    public FoodDistribution() {
        super();
    }

    @Override
    public String getDetails() {
        return (location == null ? "?" : location) + " x" + quantity
                + " to " + (distributedTo == null ? "?" : distributedTo)
                + " (" + beneficiariesServed + " served)";
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
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

    public int getBeneficiariesServed() {
        return beneficiariesServed;
    }

    public void setBeneficiariesServed(int beneficiariesServed) {
        this.beneficiariesServed = beneficiariesServed;
    }

    public String getDistributedTo() {
        return distributedTo;
    }

    public void setDistributedTo(String distributedTo) {
        this.distributedTo = ValidationUtil.clean(distributedTo);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = ValidationUtil.clean(location);
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = ValidationUtil.clean(note);
    }
}
