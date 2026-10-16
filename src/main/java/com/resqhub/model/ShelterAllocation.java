package com.resqhub.model;

import java.time.LocalDateTime;

public class ShelterAllocation {
    private Long id;
    private Long shelterId;
    private String shelterName;
    private Long disasterId;
    private String disasterName;
    private int requestedPeopleCount = 1;
    private AllocationStatus status = AllocationStatus.RECOMMENDED;
    private String recommendationReason;
    private Long decidedByUserId;
    private String decidedByUsername;
    private LocalDateTime createdAt;

    public ShelterAllocation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShelterId() {
        return shelterId;
    }

    public void setShelterId(Long shelterId) {
        this.shelterId = shelterId;
    }

    public String getShelterName() {
        return shelterName;
    }

    public void setShelterName(String shelterName) {
        this.shelterName = shelterName;
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public String getDisasterName() {
        return disasterName;
    }

    public void setDisasterName(String disasterName) {
        this.disasterName = disasterName;
    }

    public int getRequestedPeopleCount() {
        return requestedPeopleCount;
    }

    public void setRequestedPeopleCount(int requestedPeopleCount) {
        this.requestedPeopleCount = requestedPeopleCount;
    }

    public AllocationStatus getStatus() {
        return status;
    }

    public void setStatus(AllocationStatus status) {
        this.status = status;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public Long getDecidedByUserId() {
        return decidedByUserId;
    }

    public void setDecidedByUserId(Long decidedByUserId) {
        this.decidedByUserId = decidedByUserId;
    }

    public String getDecidedByUsername() {
        return decidedByUsername;
    }

    public void setDecidedByUsername(String decidedByUsername) {
        this.decidedByUsername = decidedByUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
