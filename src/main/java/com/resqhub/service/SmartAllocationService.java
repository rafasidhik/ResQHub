package com.resqhub.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.resqhub.dao.ShelterFacilityDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidShelterDataException;
import com.resqhub.exception.NoSuitableShelterException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RankedShelter;
import com.resqhub.model.RoleType;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterFacility;
import com.resqhub.model.SmartAllocationRequest;
import com.resqhub.model.SmartAllocationResult;
import com.resqhub.model.Victim;
import com.resqhub.util.ValidationUtil;

/**
 * Smart Shelter Allocation engine.
 *
 * Replaces "assign the first free shelter" with a scored, multi-factor
 * decision. Given a victim / family and their requirements it:
 *
 *   1. filters out unsuitable shelters (full, inactive/closed, without
 *      enough capacity, missing required facilities or accessibility);
 *   2. ranks the survivors on available capacity, distance (proximity),
 *      emergency priority, facilities and accessibility;
 *   3. picks the best match (highest weighted score), then creates the
 *      allocation record, which updates shelter occupancy and the
 *      victim's shelter status.
 *
 * The result carries the full ranking and the per-shelter breakdown so
 * the operator can see exactly why a shelter was (or was not) chosen.
 */
public class SmartAllocationService {

    // Ranking weights (each component is normalised to 0..1).
    private static final double W_CAPACITY = 0.35;
    private static final double W_PROXIMITY = 0.30;
    private static final double W_PRIORITY = 0.15;
    private static final double W_FACILITIES = 0.10;
    private static final double W_ACCESSIBILITY = 0.10;

    private final ShelterService shelterService = new ShelterService();
    private final ShelterFacilityDAO facilityDAO = new ShelterFacilityDAO();
    private final VictimService victimService = new VictimService();
    private final SessionManager session = SessionManager.getInstance();

    // ── public API ───────────────────────────────────────────────────

    /**
     * Evaluates the request and returns the ranked candidates + best
     * match WITHOUT creating any allocation. Read-only (preview).
     */
    public SmartAllocationResult evaluate(SmartAllocationRequest request)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            NoSuitableShelterException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        validate(request);
        autoFillFromVictim(request);

        List<Shelter> candidates = candidateShelters(request);
        if (candidates.isEmpty()) {
            throw new NoSuitableShelterException(rejectionMessage(request));
        }

        SmartAllocationResult result = new SmartAllocationResult(request);
        for (Shelter shelter : candidates) {
            result.addRanked(rank(shelter, request));
        }
        result.getRanked().sort((a, b) -> {
            int byScore = Double.compare(b.getScore(), a.getScore());
            return byScore != 0 ? byScore
                    : Double.compare(b.getProximity(), a.getProximity());
        });
        result.setBest(result.getRanked().get(0));
        return result;
    }

    /**
     * Evaluates then commits the allocation at the best shelter. The
     * created allocation (ACTIVE placement or PENDING reservation) is
     * attached to the returned result.
     */
    public SmartAllocationResult allocate(SmartAllocationRequest request)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            NoSuitableShelterException, DataAccessException {

        SmartAllocationResult result = evaluate(request);
        RankedShelter best = result.getBest();
        Shelter shelter = best.getShelter();
        String familyName = resolveFamilyName(request);

        ShelterAllocation allocation;
        if (request.isCreatePending()) {
            allocation = shelterService.createPendingAllocation(
                    shelter.getId(), request.getVictimId(), familyName,
                    request.getPeopleCount(), request.getNotes());
        } else {
            allocation = shelterService.allocateVictim(
                    shelter.getId(), request.getVictimId(), familyName,
                    request.getPeopleCount(), request.getNotes());
        }
        result.setAllocation(allocation);
        return result;
    }

    // ── suitability filtering ────────────────────────────────────────

    private List<Shelter> candidateShelters(SmartAllocationRequest request)
            throws DataAccessException {
        List<Shelter> candidates = new ArrayList<>();
        for (Shelter shelter : shelterService.getAllShelters()) {
            if (!shelter.getOperationalStatus().isAccepting()) {
                continue;
            }
            if (shelter.availableCapacity() < request.getPeopleCount()) {
                continue;
            }
            List<ShelterFacility> facilities =
                    facilityDAO.findByShelter(shelter.getId());
            if (!hasRequiredFacilities(facilities, request)) {
                continue;
            }
            if (!meetsAccessibility(shelter, request)) {
                continue;
            }
            candidates.add(shelter);
        }
        return candidates;
    }

    private boolean hasRequiredFacilities(List<ShelterFacility> present,
                                          SmartAllocationRequest request) {
        if (request.getRequiredFacilities().isEmpty()) {
            return true;
        }
        Set<String> available = new HashSet<>();
        for (ShelterFacility f : present) {
            if (f.isAvailable() && f.getFacilityName() != null) {
                available.add(f.getFacilityName().toLowerCase(Locale.ROOT));
            }
        }
        for (String required : request.getRequiredFacilities()) {
            if (required == null
                    || !available.contains(required.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private boolean meetsAccessibility(Shelter shelter,
                                       SmartAllocationRequest request) {
        if (!request.requiresAccessibility()) {
            return true;
        }
        boolean ok = true;
        if (request.isNeedWheelchair() && !shelter.isWheelchairAccessible()) {
            ok = false;
        }
        if (request.isNeedElderly() && !shelter.isElderlyFriendly()) {
            ok = false;
        }
        if (request.isNeedMedical() && !shelter.isMedicalAccessible()) {
            ok = false;
        }
        if (request.isNeedSpecial() && !shelter.isSpecialAssistance()) {
            ok = false;
        }
        return ok;
    }

    // ── scoring / ranking ────────────────────────────────────────────

    private RankedShelter rank(Shelter shelter, SmartAllocationRequest request)
            throws DataAccessException {
        double capacityScore = capacityComponent(shelter);
        double proximityScore = proximityComponent(shelter, request);
        double priorityScore = priorityComponent(request);
        double facilitiesScore = facilitiesComponent(shelter, request);
        boolean accessibilityMet = meetsAccessibility(shelter, request);
        double accessibilityScore = accessibilityMet ? 1.0 : 0.0;

        double score = W_CAPACITY * capacityScore
                + W_PROXIMITY * proximityScore
                + W_PRIORITY * priorityScore
                + W_FACILITIES * facilitiesScore
                + W_ACCESSIBILITY * accessibilityScore;

        RankedShelter r = new RankedShelter(shelter, score,
                shelter.availableCapacity() - request.getPeopleCount(),
                proximityScore, accessibilityMet,
                countMatched(shelter, request),
                request.getRequiredFacilities().size());
        r.addReason("capacity " + round(capacityScore) + ", distance "
                + round(proximityScore) + ", priority " + round(priorityScore)
                + ", facilities " + round(facilitiesScore));
        if (accessibilityMet && request.requiresAccessibility()) {
            r.addReason("accessibility requirements met");
        }
        return r;
    }

    /** Lower utilisation (more spare capacity) ranks higher. */
    private double capacityComponent(Shelter shelter) {
        if (shelter.getMaxCapacity() <= 0) {
            return 0.0;
        }
        double utilisation = (double) shelter.getCurrentOccupancy()
                / shelter.getMaxCapacity();
        return clamp01(1.0 - utilisation);
    }

    /** Distance is modelled as a location-proximity heuristic: because
     *  ResQHub has no GPS co-ordinates, shelters closer to the victim's
     *  stated location (matching district / city / place tokens) score
     *  higher. */
    private double proximityComponent(Shelter shelter,
                                      SmartAllocationRequest request) {
        String location = request.getLocation();
        if (!ValidationUtil.requireNonBlank(location)
                || shelter.getDistrict() == null) {
            return 0.5;
        }
        String vLoc = location.toLowerCase(Locale.ROOT);
        String sDistrict = lower(shelter.getDistrict());
        String sCity = lower(shelter.getCity());

        boolean districtMatch = sDistrict != null && vLoc.contains(sDistrict);
        boolean cityMatch = sCity != null && vLoc.contains(sCity);
        if (districtMatch && cityMatch) {
            return 0.9;
        }
        if (districtMatch) {
            return 0.7;
        }
        if (cityMatch) {
            return 0.6;
        }
        Set<String> victimTokens = tokens(vLoc);
        Set<String> shelterTokens = new HashSet<>();
        shelterTokens.addAll(tokens(sDistrict));
        shelterTokens.addAll(tokens(sCity));
        shelterTokens.addAll(tokens(shelter.getAddress()));
        if (shelterTokens.isEmpty() || victimTokens.isEmpty()) {
            return 0.3;
        }
        int shared = 0;
        for (String t : victimTokens) {
            if (shelterTokens.contains(t)) {
                shared++;
            }
        }
        return clamp01(0.3 + 0.3 * ((double) shared / victimTokens.size()));
    }

    /** Higher emergency priority gives stronger consideration (weight). */
    private double priorityComponent(SmartAllocationRequest request) {
        PriorityLevel p = request.getPriority();
        if (p == null) {
            return 0.5;
        }
        return clamp01(p.getWeight() / 4.0);
    }

    /** Rewards shelters that are better equipped (more available
     *  facilities) beyond the hard required-facility filter. */
    private double facilitiesComponent(Shelter shelter,
                                       SmartAllocationRequest request)
            throws DataAccessException {
        int availableCount = 0;
        Set<String> present = new HashSet<>();
        for (ShelterFacility f : facilityDAO.findByShelter(shelter.getId())) {
            if (f.isAvailable() && f.getFacilityName() != null) {
                availableCount++;
                present.add(f.getFacilityName().toLowerCase(Locale.ROOT));
            }
        }
        double base = clamp01(availableCount / 6.0);
        if (!request.getRequiredFacilities().isEmpty()) {
            int matched = 0;
            for (String r : request.getRequiredFacilities()) {
                if (r != null && present.contains(r.toLowerCase(Locale.ROOT))) {
                    matched++;
                }
            }
            base = Math.max(base,
                    (double) matched / request.getRequiredFacilities().size());
        }
        return base;
    }

    private int countMatched(Shelter shelter, SmartAllocationRequest request)
            throws DataAccessException {
        if (request.getRequiredFacilities().isEmpty()) {
            return 0;
        }
        Set<String> present = new HashSet<>();
        for (ShelterFacility f : facilityDAO.findByShelter(shelter.getId())) {
            if (f.isAvailable() && f.getFacilityName() != null) {
                present.add(f.getFacilityName().toLowerCase(Locale.ROOT));
            }
        }
        int matched = 0;
        for (String r : request.getRequiredFacilities()) {
            if (r != null && present.contains(r.toLowerCase(Locale.ROOT))) {
                matched++;
            }
        }
        return matched;
    }

    // ── validation / autofill / helpers ──────────────────────────────

    private void validate(SmartAllocationRequest request)
            throws InvalidShelterDataException {
        if (request == null) {
            throw new InvalidShelterDataException(
                    "Allocation request is required");
        }
        if (!ValidationUtil.isPositive(request.getPeopleCount())) {
            throw new InvalidShelterDataException(
                    "People count must be at least 1");
        }
        if (request.getVictimId() == null
                && !ValidationUtil.requireNonBlank(request.getFamilyName())) {
            throw new InvalidShelterDataException(
                    "A victim or a family name is required");
        }
    }

    /** Pulls the emergency priority / location from the victim record
     *  when the operator did not override them. */
    private void autoFillFromVictim(SmartAllocationRequest request)
            throws DataAccessException {
        if (request.getVictimId() == null) {
            return;
        }
        Victim victim = victimService.getVictim(request.getVictimId());
        if (victim == null) {
            return;
        }
        if (request.getPriority() == null
                && victim.getEmergencyStatus() != null) {
            request.setPriority(priorityFor(victim));
        }
        if (!ValidationUtil.requireNonBlank(request.getLocation())) {
            request.setLocation(victim.getCurrentLocation());
        }
        if (!ValidationUtil.requireNonBlank(request.getFamilyName())) {
            request.setFamilyName(victim.getFullName());
        }
    }

    private PriorityLevel priorityFor(Victim victim) {
        return switch (victim.getEmergencyStatus()) {
            case CRITICAL -> PriorityLevel.CRITICAL;
            case INJURED, RESCUE_REQUIRED -> PriorityLevel.HIGH;
            case NEEDS_ASSISTANCE -> PriorityLevel.MEDIUM;
            default -> PriorityLevel.LOW;
        };
    }

    private String resolveFamilyName(SmartAllocationRequest request) {
        if (ValidationUtil.requireNonBlank(request.getFamilyName())) {
            return request.getFamilyName().trim();
        }
        if (request.getVictimId() != null) {
            return "Victim #" + request.getVictimId();
        }
        return "Family of " + request.getPeopleCount();
    }

    private String rejectionMessage(SmartAllocationRequest request) {
        String who = ValidationUtil.requireNonBlank(request.getFamilyName())
                ? request.getFamilyName() : "the family";
        return "No suitable shelter currently available for " + who
                + " (" + request.getPeopleCount() + " people). All shelters "
                + "are full, inactive/closed, or lack the required facilities "
                + "or accessibility.";
    }

    private double clamp01(double v) {
        return Math.max(0.0, Math.min(1.0, v));
    }

    private String round(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    private String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ROOT);
    }

    /** Small, meaningful location tokens used by the proximity match. */
    private Set<String> tokens(String text) {
        Set<String> out = new LinkedHashSet<>();
        if (text == null) {
            return out;
        }
        String[] parts = text.toLowerCase(Locale.ROOT)
                .split("[^a-z0-9]+");
        for (String p : parts) {
            if (p.length() >= 3) {
                out.add(p);
            }
        }
        return out;
    }
}
