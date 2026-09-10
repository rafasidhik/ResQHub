package com.resqhub.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.resqhub.util.ValidationUtil;

/**
 * A recorded blood donation from a donor (spec: Blood Donation Recording /
 * Donation History). Tracks quantity donated, the related request (if any),
 * the date and who recorded it.
 */
public class BloodDonation extends BaseEntity {

    private Long donorId;
    private BloodGroup bloodGroup;
    private LocalDate donationDate;
    private Long requestId;
    private int unitsDonated;
    private String donationStatus;
    private String notes;
    private Long recordedBy;
    private LocalDateTime recordedAt;

    public BloodDonation() {
        super();
    }

    @Override
    public String getDetails() {
        return "donor #" + (donorId == null ? "?" : donorId)
                + " [" + (bloodGroup == null ? "?" : bloodGroup.getLabel())
                + "] " + unitsDonated + " unit(s) on "
                + (donationDate == null ? "?" : donationDate);
    }

    // ---- accessors ----------------------------------------------------

    public Long getDonorId() { return donorId; }
    public void setDonorId(Long donorId) { this.donorId = donorId; }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public LocalDate getDonationDate() { return donationDate; }
    public void setDonationDate(LocalDate donationDate) { this.donationDate = donationDate; }
    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public int getUnitsDonated() { return unitsDonated; }
    public void setUnitsDonated(int unitsDonated) { this.unitsDonated = unitsDonated; }
    public String getDonationStatus() { return donationStatus; }
    public void setDonationStatus(String donationStatus) { this.donationStatus = ValidationUtil.clean(donationStatus); }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = ValidationUtil.clean(notes); }
    public Long getRecordedBy() { return recordedBy; }
    public void setRecordedBy(Long recordedBy) { this.recordedBy = recordedBy; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
