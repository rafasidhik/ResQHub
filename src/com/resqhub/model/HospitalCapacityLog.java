package com.resqhub.model;

import java.time.LocalDateTime;

import com.resqhub.util.ValidationUtil;

/**
 * A hospital_capacity_logs record - one row describing a change in a
 * hospital's occupied / available bed counts (spec section 19). Keeps a
 * history of how capacity drifted during a disaster and why.
 */
public class HospitalCapacityLog extends BaseEntity {

    private Long hospitalId;
    private int previousOccupied;
    private int updatedOccupied;
    private int availableBeds;
    private String reason;
    private Long changedBy;
    private LocalDateTime changedAt;

    public HospitalCapacityLog() {
        super();
    }

    @Override
    public String getDetails() {
        return "hospital #" + (hospitalId == null ? "?" : hospitalId)
                + " occ " + previousOccupied + " -> " + updatedOccupied
                + " (av " + availableBeds + ")";
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public int getPreviousOccupied() {
        return previousOccupied;
    }

    public void setPreviousOccupied(int previousOccupied) {
        this.previousOccupied = previousOccupied;
    }

    public int getUpdatedOccupied() {
        return updatedOccupied;
    }

    public void setUpdatedOccupied(int updatedOccupied) {
        this.updatedOccupied = updatedOccupied;
    }

    public int getAvailableBeds() {
        return availableBeds;
    }

    public void setAvailableBeds(int availableBeds) {
        this.availableBeds = availableBeds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = ValidationUtil.clean(reason);
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
