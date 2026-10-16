package com.resqhub.dto;

import com.resqhub.model.Shelter;

public class ShelterRecommendation {
    private Shelter shelter;
    private int score;
    private String matchReason;
    private int availableCapacity;
    private int projectedOccupancyPercentage;

    public ShelterRecommendation() {
    }

    public ShelterRecommendation(Shelter shelter, int score, String matchReason, int availableCapacity, int projectedOccupancyPercentage) {
        this.shelter = shelter;
        this.score = score;
        this.matchReason = matchReason;
        this.availableCapacity = availableCapacity;
        this.projectedOccupancyPercentage = projectedOccupancyPercentage;
    }

    public Shelter getShelter() {
        return shelter;
    }

    public void setShelter(Shelter shelter) {
        this.shelter = shelter;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public int getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(int availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public int getProjectedOccupancyPercentage() {
        return projectedOccupancyPercentage;
    }

    public void setProjectedOccupancyPercentage(int projectedOccupancyPercentage) {
        this.projectedOccupancyPercentage = projectedOccupancyPercentage;
    }
}
