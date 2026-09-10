package com.resqhub.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.resqhub.exception.BloodUnavailableException;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidBloodDataException;
import com.resqhub.model.BloodDonor;
import com.resqhub.model.BloodGroup;
import com.resqhub.model.BloodRequest;
import com.resqhub.model.BloodRequestPriority;
import com.resqhub.model.DonorAvailability;
import com.resqhub.model.DonorEligibility;
import com.resqhub.model.DonorRanking;
import com.resqhub.util.LocationProximityUtil;

/**
 * Core Emergency Blood Matching engine implementing the full matching
 * algorithm flow (spec: Complete Matching Algorithm):
 *
 *   Blood Request
 *       |
 *   Identify Required Group
 *       |
 *   Find Donors
 *       |
 *   Check ABO Compatibility
 *       |
 *   Check Rh Compatibility
 *       |
 *   Check Donor Availability
 *       |
 *   Filter by Location
 *       |
 *   Consider Urgency
 *       |
 *   Rank Candidates
 *       |
 *   Best Match(es)
 *
 * Each stage progressively eliminates or scores donors. The engine
 * returns a ranked list of DonorRanking objects ordered by composite
 * suitability score.
 */
public class BloodMatchingEngine {

    // Weight constants for the composite score
    private static final int EXACT_GROUP_BONUS = 30;
    private static final int COMPATIBLE_GROUP_BONUS = 10;
    private static final int URGENCY_CRITICAL_BONUS = 20;
    private static final int URGENCY_HIGH_BONUS = 10;
    private static final int URGENCY_MEDIUM_BONUS = 5;
    private static final int LOCATION_MATCH_BONUS = 15;

    /**
     * Runs the complete matching pipeline and returns candidates ranked
     * by suitability. The list is ordered best-first.
     *
     * @param request           the blood request to match
     * @param allDonors         all registered donors in the system
     * @param alreadyMatchedIds donor IDs already matched to this request
     *                          (excluded from results to avoid duplicates)
     * @return ranked list of compatible, available donors
     * @throws InvalidBloodDataException if the request is invalid
     */
    public List<DonorRanking> match(BloodRequest request,
            List<BloodDonor> allDonors, java.util.Set<Long> alreadyMatchedIds)
            throws InvalidBloodDataException {

        validateRequest(request);

        BloodGroup requiredGroup = request.getBloodGroup();
        String requestLocation = request.getLocation();
        BloodRequestPriority priority = request.getPriority();

        // Stage 1-3: Progressive filtering
        List<BloodDonor> candidates = new ArrayList<>();
        for (BloodDonor donor : allDonors) {
            // Stage 5: Availability check
            if (donor.getAvailability() != DonorAvailability.AVAILABLE) {
                continue;
            }
            if (donor.getEligibility() != DonorEligibility.ELIGIBLE) {
                continue;
            }
            // Avoid duplicate matches
            if (alreadyMatchedIds.contains(donor.getId())) {
                continue;
            }
            // Stage 2-3: ABO + Rh compatibility check
            if (!donor.getBloodGroup().canDonateTo(requiredGroup)) {
                continue;
            }
            candidates.add(donor);
        }

        // Stage 4-8: Score and rank each candidate
        List<DonorRanking> rankings = new ArrayList<>();
        for (BloodDonor donor : candidates) {
            int compatScore = computeCompatibilityScore(
                    donor.getBloodGroup(), requiredGroup);
            int locationScore = computeLocationScore(
                    donor.getLocation(), requestLocation);
            int urgencyBonus = computeUrgencyBonus(priority);
            boolean exactGroup = donor.getBloodGroup() == requiredGroup;
            boolean locMatch = locationScore >= 90;

            rankings.add(new DonorRanking(donor, compatScore, locationScore,
                    urgencyBonus, exactGroup, locMatch));
        }

        // Stage 9: Sort by composite score (best first)
        Collections.sort(rankings);
        return rankings;
    }

    /**
     * Returns the best N donors from the ranking, up to the required
     * units quantity. Each donor provides 1 unit (standard donation).
     */
    public List<DonorRanking> bestMatch(List<DonorRanking> ranked,
            int unitsRequired, int maxDonors) {
        int limit = Math.min(
                Math.max(1, Math.min(maxDonors, unitsRequired)),
                ranked.size());
        return ranked.subList(0, limit);
    }

    /**
     * Re-match after a donor declines or becomes unavailable. Excludes
     * the declined donor and any already-collected donors, then runs
     * the matching pipeline on remaining candidates.
     */
    public List<DonorRanking> rematch(BloodRequest request,
            List<BloodDonor> allDonors, java.util.Set<Long> excludeIds)
            throws InvalidBloodDataException {
        return match(request, allDonors, excludeIds);
    }

    // ---- scoring helpers -----------------------------------------------

    /**
     * Compatibility score: exact match = 100, compatible different group
     * = 70 (lower priority so exact matches are preferred).
     */
    private int computeCompatibilityScore(BloodGroup donorGroup,
            BloodGroup requiredGroup) {
        if (donorGroup == requiredGroup) {
            return 100 + EXACT_GROUP_BONUS;
        }
        // Compatible but different group (e.g. O- serving A+)
        return 70 + COMPATIBLE_GROUP_BONUS;
    }

    /**
     * Location score using the proximity utility. Higher = closer.
     */
    private int computeLocationScore(String donorLocation,
            String requestLocation) {
        return LocationProximityUtil.score(donorLocation, requestLocation);
    }

    /**
     * Urgency bonus added to donors serving critical/high requests
     * so they surface first when urgency matters.
     */
    private int computeUrgencyBonus(BloodRequestPriority priority) {
        if (priority == null) return 0;
        switch (priority) {
            case CRITICAL: return URGENCY_CRITICAL_BONUS;
            case HIGH:     return URGENCY_HIGH_BONUS;
            case MEDIUM:   return URGENCY_MEDIUM_BONUS;
            default:       return 0;
        }
    }

    private void validateRequest(BloodRequest request)
            throws InvalidBloodDataException {
        if (request == null) {
            throw new InvalidBloodDataException("Blood request is required");
        }
        if (request.getBloodGroup() == null) {
            throw new InvalidBloodDataException(
                    "Request blood group is required for matching");
        }
        if (request.getUnitsRequired() <= 0) {
            throw new InvalidBloodDataException(
                    "Required units must be greater than zero");
        }
        if (request.getLocation() == null
                || request.getLocation().trim().isEmpty()) {
            throw new InvalidBloodDataException(
                    "Request location is required for matching");
        }
    }
}
