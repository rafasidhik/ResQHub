package com.resqhub.model;

import java.time.LocalDateTime;

import com.resqhub.util.ValidationUtil;

/**
 * A single inventory movement (stock-in or stock-out) against a
 * resource. Each movement snapshot refs the previous and new quantity of
 * the resource at the time of the operation so a running history of how
 * stock changed is preserved (spec sections 4, 5, 13).
 */
public class StockMovement extends BaseEntity {

    private Long resourceId;
    private StockMovementType type;
    private int quantity;
    private int previousQuantity;
    private int newQuantity;
    private String source;
    private String destination;
    private String reason;
    private Long disasterId;
    private LocalDateTime movedAt;
    private Long recordedBy;

    public StockMovement() {
        super();
    }

    @Override
    public String getDetails() {
        return (type == null ? "?" : type.getLabel()) + " x" + quantity
                + " (prev " + previousQuantity + " -> new " + newQuantity + ")";
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public StockMovementType getType() {
        return type;
    }

    public void setType(StockMovementType type) {
        this.type = type;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPreviousQuantity() {
        return previousQuantity;
    }

    public void setPreviousQuantity(int previousQuantity) {
        this.previousQuantity = previousQuantity;
    }

    public int getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(int newQuantity) {
        this.newQuantity = newQuantity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = ValidationUtil.clean(source);
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = ValidationUtil.clean(destination);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = ValidationUtil.clean(reason);
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public LocalDateTime getMovedAt() {
        return movedAt;
    }

    public void setMovedAt(LocalDateTime movedAt) {
        this.movedAt = movedAt;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(Long recordedBy) {
        this.recordedBy = recordedBy;
    }
}
