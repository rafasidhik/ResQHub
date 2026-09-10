package com.resqhub.model;

import java.time.LocalDateTime;

/**
 * A donor offered / confirmed against a blood request (spec: Donor Matching).
 * Stores the donor, the request, the ABO/Rh compatibility outcome and the
 * match lifecycle status.
 */
public class BloodMatch extends BaseEntity {

    private Long requestId;
    private Long donorId;
    private BloodMatchStatus status = BloodMatchStatus.SUGGESTED;
    private int unitsMatched;
    private boolean locationMatched;
    private int donorDistanceRank;
    private String notes;
    private Long matchedBy;
    private LocalDateTime matchedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime collectedAt;

    public BloodMatch() {
        super();
    }

    @Override
    public String getDetails() {
        return "request #" + (requestId == null ? "?" : requestId)
                + " <-> donor #" + (donorId == null ? "?" : donorId)
                + " (" + (status == null ? "?" : status.getLabel()) + ")";
    }

    // ---- accessors ----------------------------------------------------

    public Long getRequestId() { return requestId; }
    public void setRequestId(Long requestId) { this.requestId = requestId; }
    public Long getDonorId() { return donorId; }
    public void setDonorId(Long donorId) { this.donorId = donorId; }
    public BloodMatchStatus getStatus() { return status; }
    public void setStatus(BloodMatchStatus status) { this.status = status; }
    public int getUnitsMatched() { return unitsMatched; }
    public void setUnitsMatched(int unitsMatched) { this.unitsMatched = unitsMatched; }
    public boolean isLocationMatched() { return locationMatched; }
    public void setLocationMatched(boolean locationMatched) { this.locationMatched = locationMatched; }
    public int getDonorDistanceRank() { return donorDistanceRank; }
    public void setDonorDistanceRank(int donorDistanceRank) { this.donorDistanceRank = donorDistanceRank; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Long getMatchedBy() { return matchedBy; }
    public void setMatchedBy(Long matchedBy) { this.matchedBy = matchedBy; }
    public LocalDateTime getMatchedAt() { return matchedAt; }
    public void setMatchedAt(LocalDateTime matchedAt) { this.matchedAt = matchedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(LocalDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
    public void setCollectedAt(LocalDateTime collectedAt) { this.collectedAt = collectedAt; }
}
