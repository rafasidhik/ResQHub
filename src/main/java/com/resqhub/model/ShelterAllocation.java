package com.resqhub.model;

import java.time.LocalDateTime;

import com.resqhub.util.ValidationUtil;

/**
 * A victim or family allocated to a shelter. peopleCount reflects the
 * total number of people moved (a single victim is 1; a family is the
 * family size), driving the shelter's current occupancy and capacity
 * guard.
 */
public class ShelterAllocation extends BaseEntity {

    private Long shelterId;
    private Long victimId;
    private String familyName;
    private int peopleCount = 1;
    private String notes;
    private LocalDateTime allocatedAt;
    private LocalDateTime releasedAt;
    private ShelterAllocationStatus status = ShelterAllocationStatus.ACTIVE;
    private Long allocatedBy;

    public ShelterAllocation() {
        super();
    }

    @Override
    public String getDetails() {
        return (familyName == null ? "Victim #" + victimId : familyName)
                + " -> shelter #" + shelterId + " x" + peopleCount
                + " (" + (status == null ? "?" : status.getLabel()) + ")";
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

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = ValidationUtil.clean(familyName);
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(int peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = ValidationUtil.clean(notes);
    }

    public LocalDateTime getAllocatedAt() {
        return allocatedAt;
    }

    public void setAllocatedAt(LocalDateTime allocatedAt) {
        this.allocatedAt = allocatedAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public ShelterAllocationStatus getStatus() {
        return status;
    }

    public void setStatus(ShelterAllocationStatus status) {
        this.status = status;
    }

    public Long getAllocatedBy() {
        return allocatedBy;
    }

    public void setAllocatedBy(Long allocatedBy) {
        this.allocatedBy = allocatedBy;
    }
}
