package com.resqhub.model;

/**
 * Composite ranking of a blood donor against a specific blood request.
 * Captures ABO/Rh compatibility quality, location proximity, and a
 * weighted total used to order candidates for the matching algorithm
 * (spec: Donor Ranking, Best Match Identification).
 */
public class DonorRanking implements Comparable<DonorRanking> {

    private final BloodDonor donor;
    private final int compatibilityScore;
    private final int locationScore;
    private final int urgencyBonus;
    private final int totalScore;
    private final boolean exactGroupMatch;
    private final boolean locationMatch;

    public DonorRanking(BloodDonor donor, int compatibilityScore,
            int locationScore, int urgencyBonus, boolean exactGroupMatch,
            boolean locationMatch) {
        this.donor = donor;
        this.compatibilityScore = compatibilityScore;
        this.locationScore = locationScore;
        this.urgencyBonus = urgencyBonus;
        this.exactGroupMatch = exactGroupMatch;
        this.locationMatch = locationMatch;
        this.totalScore = compatibilityScore + locationScore + urgencyBonus;
    }

    public BloodDonor getDonor() { return donor; }
    public int getCompatibilityScore() { return compatibilityScore; }
    public int getLocationScore() { return locationScore; }
    public int getUrgencyBonus() { return urgencyBonus; }
    public int getTotalScore() { return totalScore; }
    public boolean isExactGroupMatch() { return exactGroupMatch; }
    public boolean isLocationMatch() { return locationMatch; }

    /**
     * Higher total score = better match. Ties broken by exact group match
     * first, then location match, then name.
     */
    @Override
    public int compareTo(DonorRanking other) {
        int cmp = Integer.compare(other.totalScore, this.totalScore);
        if (cmp != 0) return cmp;
        cmp = Boolean.compare(other.exactGroupMatch, this.exactGroupMatch);
        if (cmp != 0) return cmp;
        cmp = Boolean.compare(other.locationMatch, this.locationMatch);
        if (cmp != 0) return cmp;
        return this.donor.getFullName().compareTo(
                other.donor.getFullName());
    }

    @Override
    public String toString() {
        return donor.getFullName() + " [" + donor.getBloodGroup().getLabel()
                + "] score=" + totalScore + " (compat=" + compatibilityScore
                + " loc=" + locationScore + " urg=" + urgencyBonus + ")";
    }
}
