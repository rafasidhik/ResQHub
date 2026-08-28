package com.resqhub.model;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import com.resqhub.util.ValidationUtil;

/**
 * A referral of a victim (or family) to a suitable hospital for emergency
 * medical care (spec section 11). The referral captures why medical care is
 * needed, how many beds are required and which emergency facilities the case
 * depends on. Capacity is only reduced on the hospital once the referral is
 * accepted / admitted, and released again on discharge.
 */
public class HospitalReferral extends BaseEntity {

    private Long hospitalId;
    private Long victimId;
    private String victimName;
    private String reason;
    private int bedsRequired;
    private Set<HospitalFacility> requiredFacilities = new LinkedHashSet<>();
    private HospitalReferralStatus status = HospitalReferralStatus.PENDING;
    private boolean bedsApplied;
    private Long referredBy;
    private LocalDateTime referredAt;
    private LocalDateTime closedAt;
    private String notes;
    private Long disasterId;

    public HospitalReferral() {
        super();
    }

    @Override
    public String getDetails() {
        return (victimName == null ? "?" : victimName) + " -> hospital #"
                + (hospitalId == null ? "?" : hospitalId) + " ("
                + (status == null ? "?" : status.getLabel()) + ")";
    }

    public String requiredFacilitiesSummary() {
        if (requiredFacilities == null || requiredFacilities.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (HospitalFacility f : requiredFacilities) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(f.getLabel());
        }
        return sb.toString();
    }

    public Long getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(Long hospitalId) {
        this.hospitalId = hospitalId;
    }

    public Long getVictimId() {
        return victimId;
    }

    public void setVictimId(Long victimId) {
        this.victimId = victimId;
    }

    public String getVictimName() {
        return victimName;
    }

    public void setVictimName(String victimName) {
        this.victimName = ValidationUtil.clean(victimName);
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = ValidationUtil.clean(reason);
    }

    public int getBedsRequired() {
        return bedsRequired;
    }

    public void setBedsRequired(int bedsRequired) {
        this.bedsRequired = bedsRequired;
    }

    public Set<HospitalFacility> getRequiredFacilities() {
        return requiredFacilities;
    }

    public void setRequiredFacilities(Set<HospitalFacility> facilities) {
        this.requiredFacilities = facilities == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(facilities);
    }

    public HospitalReferralStatus getStatus() {
        return status;
    }

    public void setStatus(HospitalReferralStatus status) {
        this.status = status;
    }

    /** True once the referral has bumped the hospital's occupied bed count. */
    public boolean isBedsApplied() {
        return bedsApplied;
    }

    public void setBedsApplied(boolean bedsApplied) {
        this.bedsApplied = bedsApplied;
    }

    public Long getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(Long referredBy) {
        this.referredBy = referredBy;
    }

    public LocalDateTime getReferredAt() {
        return referredAt;
    }

    public void setReferredAt(LocalDateTime referredAt) {
        this.referredAt = referredAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = ValidationUtil.clean(notes);
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }
}
