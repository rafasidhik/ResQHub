package com.resqhub.model;

import java.time.LocalDateTime;

import com.resqhub.util.ValidationUtil;

/**
 * Emergency blood requirement (spec: Blood Request Management). Captures the
 * required blood group, quantity, location, priority, status and related
 * emergency/hospital context.
 */
public class BloodRequest extends BaseEntity {

    private String requestCode;
    private BloodGroup bloodGroup;
    private int unitsRequired;
    private String location;
    private BloodRequestPriority priority = BloodRequestPriority.MEDIUM;
    private BloodRequestStatus status = BloodRequestStatus.PENDING;
    private String emergencyDetails;
    private Long hospitalId;
    private Long victimId;
    private Long requireForDisaster;
    private Long createdBy;
    private LocalDateTime requestDate;
    private LocalDateTime requiredDate;
    private Long fulfilledByMatchId;

    public BloodRequest() {
        super();
    }

    @Override
    public String getDetails() {
        return requestCode + " [" + (bloodGroup == null ? "?" : bloodGroup.getLabel())
                + " x" + unitsRequired + "] @" + (location == null ? "?" : location)
                + " (" + (priority == null ? "?" : priority.getLabel())
                + ", " + (status == null ? "?" : status.getLabel()) + ")";
    }

    // ---- accessors ----------------------------------------------------

    public String getRequestCode() { return requestCode; }
    public void setRequestCode(String requestCode) { this.requestCode = ValidationUtil.clean(requestCode); }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public int getUnitsRequired() { return unitsRequired; }
    public void setUnitsRequired(int unitsRequired) { this.unitsRequired = unitsRequired; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = ValidationUtil.clean(location); }
    public BloodRequestPriority getPriority() { return priority; }
    public void setPriority(BloodRequestPriority priority) { this.priority = priority; }
    public BloodRequestStatus getStatus() { return status; }
    public void setStatus(BloodRequestStatus status) { this.status = status; }
    public String getEmergencyDetails() { return emergencyDetails; }
    public void setEmergencyDetails(String emergencyDetails) { this.emergencyDetails = ValidationUtil.clean(emergencyDetails); }
    public Long getHospitalId() { return hospitalId; }
    public void setHospitalId(Long hospitalId) { this.hospitalId = hospitalId; }
    public Long getVictimId() { return victimId; }
    public void setVictimId(Long victimId) { this.victimId = victimId; }
    public Long getRequireForDisaster() { return requireForDisaster; }
    public void setRequireForDisaster(Long requireForDisaster) { this.requireForDisaster = requireForDisaster; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }
    public LocalDateTime getRequiredDate() { return requiredDate; }
    public void setRequiredDate(LocalDateTime requiredDate) { this.requiredDate = requiredDate; }
    public Long getFulfilledByMatchId() { return fulfilledByMatchId; }
    public void setFulfilledByMatchId(Long fulfilledByMatchId) { this.fulfilledByMatchId = fulfilledByMatchId; }
}
