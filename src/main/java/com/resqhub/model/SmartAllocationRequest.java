package com.resqhub.model;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The requirements gathered for a smart shelter allocation run. Captures
 * who is being placed (victim / family), how many people, the emergency
 * priority, the current location (for distance/proximity), required
 * facilities and accessibility needs, plus whether the allocation should
 * be created as a PENDING reservation or an immediate ACTIVE placement.
 */
public class SmartAllocationRequest {

    private Long victimId;
    private String familyName;
    private int peopleCount = 1;
    private PriorityLevel priority;
    private String location;
    private final Set<String> requiredFacilities = new LinkedHashSet<>();
    private boolean needWheelchair;
    private boolean needElderly;
    private boolean needMedical;
    private boolean needSpecial;
    private Long disasterId;
    private String notes;
    private boolean createPending;

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
        this.familyName = familyName;
    }

    public int getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(int peopleCount) {
        this.peopleCount = peopleCount;
    }

    public PriorityLevel getPriority() {
        return priority;
    }

    public void setPriority(PriorityLevel priority) {
        this.priority = priority;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Set<String> getRequiredFacilities() {
        return requiredFacilities;
    }

    public boolean isNeedWheelchair() {
        return needWheelchair;
    }

    public void setNeedWheelchair(boolean needWheelchair) {
        this.needWheelchair = needWheelchair;
    }

    public boolean isNeedElderly() {
        return needElderly;
    }

    public void setNeedElderly(boolean needElderly) {
        this.needElderly = needElderly;
    }

    public boolean isNeedMedical() {
        return needMedical;
    }

    public void setNeedMedical(boolean needMedical) {
        this.needMedical = needMedical;
    }

    public boolean isNeedSpecial() {
        return needSpecial;
    }

    public void setNeedSpecial(boolean needSpecial) {
        this.needSpecial = needSpecial;
    }

    /** Any one of the accessibility options satisfies the requirement. */
    public boolean requiresAccessibility() {
        return needWheelchair || needElderly || needMedical || needSpecial;
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isCreatePending() {
        return createPending;
    }

    public void setCreatePending(boolean createPending) {
        this.createPending = createPending;
    }
}
