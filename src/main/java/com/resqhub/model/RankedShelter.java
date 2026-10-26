package com.resqhub.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One shelter evaluated during a smart allocation run, carrying its
 * weighted score and the per-factor breakdown so the user can see WHY it
 * was (or was not) chosen. Higher score = more suitable.
 */
public class RankedShelter {

    private final Shelter shelter;
    private final double score;
    private final int availableAfter;
    private final double proximity;
    private final boolean accessibilityMet;
    private final int facilitiesMatched;
    private final int facilitiesRequired;
    private final List<String> reasons = new ArrayList<>();

    public RankedShelter(Shelter shelter, double score, int availableAfter,
                         double proximity, boolean accessibilityMet,
                         int facilitiesMatched, int facilitiesRequired) {
        this.shelter = shelter;
        this.score = score;
        this.availableAfter = availableAfter;
        this.proximity = proximity;
        this.accessibilityMet = accessibilityMet;
        this.facilitiesMatched = facilitiesMatched;
        this.facilitiesRequired = facilitiesRequired;
    }

    public Shelter getShelter() {
        return shelter;
    }

    public double getScore() {
        return score;
    }

    public int getAvailableAfter() {
        return availableAfter;
    }

    public double getProximity() {
        return proximity;
    }

    public boolean isAccessibilityMet() {
        return accessibilityMet;
    }

    public int getFacilitiesMatched() {
        return facilitiesMatched;
    }

    public int getFacilitiesRequired() {
        return facilitiesRequired;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public void addReason(String reason) {
        reasons.add(reason);
    }
}
