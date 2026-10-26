package com.resqhub.model;

import java.time.LocalDateTime;

/**
 * Records a user's request to have their account deleted.
 * Admin reviews and approves/denies before any action is taken.
 */
public class AccountDeletionRequest extends BaseEntity {

    private Long userId;
    private DeletionRequestStatus status;
    private LocalDateTime requestedAt;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String adminNotes;

    public AccountDeletionRequest() {
        this.status = DeletionRequestStatus.PENDING;
        this.requestedAt = LocalDateTime.now();
    }

    public AccountDeletionRequest(Long userId) {
        this();
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public DeletionRequestStatus getStatus() {
        return status;
    }

    public void setStatus(DeletionRequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    @Override
    public String getDetails() {
        return "DeletionRequest #" + getId()
                + " | user #" + userId
                + " | " + status.getLabel();
    }
}
