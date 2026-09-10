package com.resqhub.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.resqhub.dao.BloodDonationDAO;
import com.resqhub.dao.BloodDonorDAO;
import com.resqhub.dao.BloodMatchDAO;
import com.resqhub.dao.BloodRequestDAO;
import com.resqhub.dao.HospitalDAO;
import com.resqhub.dao.NotificationDAO;
import com.resqhub.dao.RequestHistoryDAO;
import com.resqhub.dao.UserDAO;
import com.resqhub.dao.VictimDAO;
import com.resqhub.exception.BloodUnavailableException;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidBloodDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.BloodDonation;
import com.resqhub.model.BloodDonor;
import com.resqhub.model.BloodGroup;
import com.resqhub.model.BloodMatch;
import com.resqhub.model.BloodMatchStatus;
import com.resqhub.model.BloodRequest;
import com.resqhub.model.BloodRequestPriority;
import com.resqhub.model.BloodRequestStatus;
import com.resqhub.model.DonorAvailability;
import com.resqhub.model.DonorEligibility;
import com.resqhub.model.DonorRanking;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationPriority;
import com.resqhub.model.NotificationType;
import com.resqhub.model.RequestHistory;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.util.LocationProximityUtil;
import com.resqhub.util.ValidationUtil;

/**
 * Blood Donor Management service. Registers donors and their blood-group /
 * availability / eligibility / location profile, records emergency blood
 * requests by priority, matches suitable donors (ABO/Rh group, availability,
 * eligibility, location), records donations + history, and raises blood
 * shortage alerts (spec sections 1-28).
 *
 * Matching rules:
 *   - a donor must be AVAILABLE and ELIGIBLE,
 *   - the donor's blood group must satisfy the request's group (ABO/Rh),
 *   - donors nearer the request location rank first.
 */
public class BloodDonorService {

    private static final long ALERT_DEDUP_SECONDS = 6 * 60 * 60;
    /** A donor is generally eligible to donate again after this gap. */
    private static final int MIN_DONATION_GAP_DAYS = 90;

    private final BloodDonorDAO donorDAO = new BloodDonorDAO();
    private final BloodRequestDAO requestDAO = new BloodRequestDAO();
    private final BloodMatchDAO matchDAO = new BloodMatchDAO();
    private final BloodDonationDAO donationDAO = new BloodDonationDAO();
    private final RequestHistoryDAO historyDAO = new RequestHistoryDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final NotificationService notificationService =
            new NotificationService();
    private final UserDAO userDAO = new UserDAO();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final VictimDAO victimDAO = new VictimDAO();
    private final SessionManager session = SessionManager.getInstance();
    private final BloodMatchingEngine matchingEngine = new BloodMatchingEngine();

    // ==================== DONOR REGISTRATION =========================

    public BloodDonor registerDonor(String fullName, BloodGroup bloodGroup,
            String location, String phone, String email,
            DonorAvailability availability, LocalDate lastDonationDate,
            DonorEligibility eligibility, String notes)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        requireWriteRole();
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(fullName)) {
            errors.add("donor name is required");
        }
        if (bloodGroup == null) {
            errors.add("blood group must be selected");
        }
        if (!ValidationUtil.requireNonBlank(location)) {
            errors.add("location is required");
        }
        if (phone != null && !phone.trim().isEmpty()
                && !ValidationUtil.isValidPhone(phone)) {
            errors.add("contact number must be 10 digits");
        }
        if (email != null && !email.trim().isEmpty()
                && !ValidationUtil.isValidEmail(email)) {
            errors.add("email is invalid");
        }
        if (lastDonationDate != null
                && lastDonationDate.isAfter(LocalDate.now())) {
            errors.add("last donation date cannot be in the future");
        }
        if (!errors.isEmpty()) {
            throw new InvalidBloodDataException(String.join("; ", errors));
        }

        if (phone != null && !phone.trim().isEmpty()
                && donorDAO.findByPhone(phone) != null) {
            throw new InvalidBloodDataException(
                    "donor already registered with contact " + phone.trim());
        }

        BloodDonor d = new BloodDonor(fullName, bloodGroup, location);
        d.setPhone(phone == null ? null : phone.trim());
        d.setEmail(email == null ? null : email.trim());
        d.setAvailability(availability == null
                ? DonorAvailability.AVAILABLE : availability);
        d.setLastDonationDate(lastDonationDate);
        d.setEligibility(eligibility == null
                ? DonorEligibility.TEMPORARY_DEFERRED : eligibility);
        d.setNotes(notes);
        d.setRegisteredBy(session.currentUserId());
        BloodDonor saved = donorDAO.save(d);
        notifySelf("Blood donor registered: " + saved.getFullName() + " ["
                + saved.getBloodGroup().getLabel() + "]",
                NotificationType.BLOOD, NotificationPriority.INFO);
        return saved;
    }

    public BloodDonor updateDonor(BloodDonor donor)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        requireWriteRole();
        if (donor == null || donor.getId() == null) {
            throw new InvalidBloodDataException(
                    "a donor to update is required");
        }
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(donor.getFullName())) {
            errors.add("donor name is required");
        }
        if (donor.getBloodGroup() == null) {
            errors.add("blood group must be selected");
        }
        if (!ValidationUtil.requireNonBlank(donor.getLocation())) {
            errors.add("location is required");
        }
        if (donor.getPhone() != null && !donor.getPhone().isEmpty()
                && !ValidationUtil.isValidPhone(donor.getPhone())) {
            errors.add("contact number must be 10 digits");
        }
        if (donor.getEmail() != null && !donor.getEmail().isEmpty()
                && !ValidationUtil.isValidEmail(donor.getEmail())) {
            errors.add("email is invalid");
        }
        if (!errors.isEmpty()) {
            throw new InvalidBloodDataException(String.join("; ", errors));
        }
        return donorDAO.save(donor);
    }

    /** Quick availability / eligibility toggle from the donor profile. */
    public BloodDonor updateDonorState(long donorId,
            DonorAvailability availability, DonorEligibility eligibility)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        requireWriteRole();
        BloodDonor d = requireDonor(donorId);
        if (availability != null) {
            d.setAvailability(availability);
        }
        if (eligibility != null) {
            d.setEligibility(eligibility);
        }
        return donorDAO.save(d);
    }

    public void deleteDonor(long donorId)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN);
        requireDonor(donorId);
        BloodDonor donor = requireDonor(donorId);
        if (!donationDAO.findByDonor(donorId).isEmpty()) {
            throw new InvalidBloodDataException(
                    "Donor " + donor.getFullName()
                            + " has donation history and cannot be deleted");
        }
        if (!donorDAO.deleteById(donorId)) {
            throw new InvalidBloodDataException(
                    "Could not delete donor " + donorId);
        }
    }

    // ==================== BLOOD REQUESTS =============================

    public BloodRequest createBloodRequest(String requestCode,
            BloodGroup bloodGroup, int unitsRequired, String location,
            BloodRequestPriority priority, String emergencyDetails,
            Long hospitalId, Long victimId, Long disasterId,
            LocalDateTime requiredDate)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        requireWriteRole();
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(requestCode)) {
            errors.add("request code is required");
        }
        if (bloodGroup == null) {
            errors.add("blood group is required");
        }
        if (unitsRequired <= 0) {
            errors.add("required units must be greater than zero");
        }
        if (!ValidationUtil.requireNonBlank(location)) {
            errors.add("location is required");
        }
        if (!errors.isEmpty()) {
            throw new InvalidBloodDataException(String.join("; ", errors));
        }
        if (requestDAO.findByCode(requestCode) != null) {
            throw new InvalidBloodDataException(
                    "request code already used: " + requestCode);
        }
        if (hospitalId != null && hospitalDAO.findById(hospitalId) == null) {
            throw new InvalidBloodDataException(
                    "hospital #" + hospitalId + " not found");
        }
        if (victimId != null && victimDAO.findById(victimId) == null) {
            throw new InvalidBloodDataException(
                    "victim #" + victimId + " not found");
        }
        if (disasterId != null && new com.resqhub.dao.DisasterDAO()
                .findById(disasterId) == null) {
            throw new InvalidBloodDataException(
                    "disaster #" + disasterId + " not found");
        }

        BloodRequest r = new BloodRequest();
        r.setRequestCode(requestCode);
        r.setBloodGroup(bloodGroup);
        r.setUnitsRequired(unitsRequired);
        r.setLocation(location);
        r.setPriority(priority == null
                ? BloodRequestPriority.MEDIUM : priority);
        r.setStatus(BloodRequestStatus.PENDING);
        r.setEmergencyDetails(emergencyDetails);
        r.setHospitalId(hospitalId);
        r.setVictimId(victimId);
        r.setRequireForDisaster(disasterId);
        r.setCreatedBy(session.currentUserId());
        r.setRequestDate(LocalDateTime.now());
        r.setRequiredDate(requiredDate);
        BloodRequest saved = requestDAO.save(r);
        recordHistory(saved.getId(), "Request Created",
                "Request " + saved.getRequestCode() + " for "
                        + saved.getBloodGroup().getLabel() + " x"
                        + saved.getUnitsRequired() + " unit(s) at "
                        + saved.getLocation(), session.currentUserId());
        notifyAll("Blood request raised: " + saved.getRequestCode() + " ["
                + saved.getBloodGroup().getLabel() + " x"
                + saved.getUnitsRequired() + "] @ " + saved.getLocation()
                + " (" + saved.getPriority().getLabel() + ")",
                NotificationType.BLOOD, priorityOf(saved.getPriority()));
        return saved;
    }

    public BloodRequest updateRequest(BloodRequest request)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        requireWriteRole();
        if (request == null || request.getId() == null) {
            throw new InvalidBloodDataException(
                    "a blood request is required");
        }
        if (!ValidationUtil.requireNonBlank(request.getRequestCode())) {
            throw new InvalidBloodDataException("request code is required");
        }
        if (request.getBloodGroup() == null) {
            throw new InvalidBloodDataException("blood group is required");
        }
        if (request.getUnitsRequired() <= 0) {
            throw new InvalidBloodDataException(
                    "required units must be greater than zero");
        }
        BloodRequest saved = requestDAO.save(request);
        recordHistory(saved.getId(), "Request Updated",
                "Details revised for " + saved.getRequestCode(),
                session.currentUserId());
        return saved;
    }

    public BloodRequest setRequestStatus(long requestId,
            BloodRequestStatus target)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        requireWriteRole();
        if (target == null) {
            throw new InvalidBloodDataException(
                    "a request status must be selected");
        }
        BloodRequest r = requireRequest(requestId);
        if (target == BloodRequestStatus.FULFILLED) {
            // Do not allow fulfilment beyond what has actually been collected.
            int collected = matchDAO.sumCollectedUnits(requestId);
            if (collected < r.getUnitsRequired()) {
                throw new InvalidBloodDataException(
                        r.getUnitsRequired() + " units required but only "
                                + collected + " collected so far");
            }
        }
        r.setStatus(target);
        recordHistory(r.getId(), "Status Changed",
                "Request " + r.getRequestCode() + " moved to "
                        + target.getLabel(), session.currentUserId());
        notifySelf("Blood request " + r.getRequestCode() + " -> "
                + target.getLabel(), NotificationType.BLOOD,
                NotificationPriority.INFO);
        return requestDAO.save(r);
    }

    public void deleteRequest(long requestId)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN);
        requireRequest(requestId);
        if (!matchDAO.findByRequest(requestId).isEmpty()) {
            throw new InvalidBloodDataException(
                    "Blood request has donor matches and cannot be deleted");
        }
        if (!requestDAO.deleteById(requestId)) {
            throw new InvalidBloodDataException(
                    "Could not delete blood request " + requestId);
        }
    }

    // ==================== DONOR MATCHING =============================

    /**
     * Finds all donors who can serve a request: AVAILABLE + ELIGIBLE and
     * whose blood group satisfies the requested group (ABO/Rh). Results are
     * ranked by the BloodMatchingEngine using composite scoring: ABO/Rh
     * compatibility quality, location proximity, and urgency consideration.
     * (spec 2, 3, 5, 6, 7, 8, 9, 25)
     */
    public List<BloodDonor> findSuitableDonors(long requestId)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.BLOOD_COORDINATOR, RoleType.MEDICAL_OFFICER);
        BloodRequest r = requireRequest(requestId);
        Set<Long> alreadyMatched = new HashSet<>();
        for (BloodMatch m : matchDAO.findByRequest(requestId)) {
            if (m.getDonorId() != null) {
                alreadyMatched.add(m.getDonorId());
            }
        }
        List<DonorRanking> ranked = matchingEngine.match(r,
                donorDAO.findAll(), alreadyMatched);
        List<BloodDonor> result = new ArrayList<>();
        for (DonorRanking rk : ranked) {
            result.add(rk.getDonor());
        }
        return result;
    }

    /**
     * Returns ranked candidates with full scoring details for UI display
     * (spec 9: Donor Ranking, spec 10: Best Match Identification).
     */
    public List<DonorRanking> findRankedDonors(long requestId)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.BLOOD_COORDINATOR, RoleType.MEDICAL_OFFICER);
        BloodRequest r = requireRequest(requestId);
        Set<Long> alreadyMatched = new HashSet<>();
        for (BloodMatch m : matchDAO.findByRequest(requestId)) {
            if (m.getDonorId() != null) {
                alreadyMatched.add(m.getDonorId());
            }
        }
        return matchingEngine.match(r, donorDAO.findAll(), alreadyMatched);
    }

    /**
     * Proposes the best available donor(s) for a request by running the
     * full matching algorithm. The engine ranks candidates by composite
     * score (ABO/Rh, location, urgency) and selects the best N. Returns
     * the created match records. Throws BloodUnavailableException when no
     * suitable donor exists (spec 10, 11, 12, 14, 21).
     */
    public List<BloodMatch> matchRequest(long requestId, int maxDonors)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            BloodUnavailableException, DataAccessException {

        requireWriteRole();
        BloodRequest r = requireRequest(requestId);

        Set<Long> alreadyMatched = new HashSet<>();
        for (BloodMatch m : matchDAO.findByRequest(requestId)) {
            if (m.getDonorId() != null) {
                alreadyMatched.add(m.getDonorId());
            }
        }

        List<DonorRanking> ranked = matchingEngine.match(r,
                donorDAO.findAll(), alreadyMatched);
        List<DonorRanking> best = matchingEngine.bestMatch(ranked,
                r.getUnitsRequired(), maxDonors);

        if (best.isEmpty()) {
            detectShortage(r);
            throw new BloodUnavailableException(
                    r.getBloodGroup().getLabel(), r.getUnitsRequired(),
                    "No compatible available donor found for "
                            + r.getBloodGroup().getLabel() + " x"
                            + r.getUnitsRequired() + " ("
                            + r.getRequestCode() + ")");
        }

        int rank = 1;
        List<BloodMatch> created = new ArrayList<>();
        for (DonorRanking rk : best) {
            BloodDonor donor = rk.getDonor();
            BloodMatch m = new BloodMatch();
            m.setRequestId(requestId);
            m.setDonorId(donor.getId());
            m.setStatus(BloodMatchStatus.SUGGESTED);
            m.setUnitsMatched(1);
            m.setLocationMatched(rk.isLocationMatch());
            m.setDonorDistanceRank(rank);
            m.setMatchedBy(session.currentUserId());
            m.setMatchedAt(LocalDateTime.now());
            m.setNotes("Score: " + rk.getTotalScore() + " (compat="
                    + rk.getCompatibilityScore() + " loc="
                    + rk.getLocationScore() + " urg=" + rk.getUrgencyBonus()
                    + ") " + (rk.isExactGroupMatch() ? "EXACT" : "COMPAT")
                    + " " + LocationProximityUtil.rankLabel(
                            rk.getLocationScore()));
            created.add(matchDAO.save(m));
            rank++;
        }
        if (r.getStatus() == BloodRequestStatus.PENDING) {
            r.setStatus(BloodRequestStatus.MATCHING_DONORS);
            requestDAO.save(r);
        }
        recordHistory(r.getId(), "Donor Search Started",
                "Proposed " + created.size() + " ranked donor(s) for "
                        + r.getRequestCode() + " (best score: "
                        + best.get(0).getTotalScore() + ")",
                session.currentUserId());
        notifyDonorsForMatch(r, created);
        detectShortage(r);
        notifyAllShortageIfNeeded();
        return created;
    }

    /**
     * Re-matches a request after one or more donors decline or become
     * unavailable (spec 15: Re-Matching). Excludes previously matched
     * donors (COLLECTED and DECLINED) and re-runs the matching engine
     * on remaining candidates.
     */
    public List<BloodMatch> rematchRequest(long requestId, int maxDonors)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            BloodUnavailableException, DataAccessException {

        requireWriteRole();
        BloodRequest r = requireRequest(requestId);

        Set<Long> excludeIds = new HashSet<>();
        for (BloodMatch m : matchDAO.findByRequest(requestId)) {
            if (m.getDonorId() != null) {
                excludeIds.add(m.getDonorId());
            }
        }

        List<DonorRanking> ranked = matchingEngine.rematch(r,
                donorDAO.findAll(), excludeIds);
        List<DonorRanking> best = matchingEngine.bestMatch(ranked,
                r.getUnitsRequired(), maxDonors);

        if (best.isEmpty()) {
            detectShortage(r);
            throw new BloodUnavailableException(
                    r.getBloodGroup().getLabel(), r.getUnitsRequired(),
                    "Re-match: no additional compatible donors for "
                            + r.getBloodGroup().getLabel() + " ("
                            + r.getRequestCode() + ")");
        }

        int rank = 1;
        List<BloodMatch> created = new ArrayList<>();
        for (DonorRanking rk : best) {
            BloodDonor donor = rk.getDonor();
            BloodMatch m = new BloodMatch();
            m.setRequestId(requestId);
            m.setDonorId(donor.getId());
            m.setStatus(BloodMatchStatus.SUGGESTED);
            m.setUnitsMatched(1);
            m.setLocationMatched(rk.isLocationMatch());
            m.setDonorDistanceRank(rank);
            m.setMatchedBy(session.currentUserId());
            m.setMatchedAt(LocalDateTime.now());
            m.setNotes("Re-match score: " + rk.getTotalScore() + " ("
                    + LocationProximityUtil.rankLabel(
                            rk.getLocationScore()) + ")");
            created.add(matchDAO.save(m));
            rank++;
        }
        recordHistory(r.getId(), "Re-Match Initiated",
                "Re-matched " + created.size() + " new donor(s) for "
                        + r.getRequestCode() + " after declines",
                session.currentUserId());
        notifyDonorsForMatch(r, created);
        return created;
    }

    /** Updates a single match's status through its lifecycle. */
    public BloodMatch setMatchStatus(long matchId, BloodMatchStatus target)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        requireWriteRole();
        if (target == null) {
            throw new InvalidBloodDataException(
                    "a match status must be selected");
        }
        BloodMatch m = findMatch(matchId);
        if (target == BloodMatchStatus.CONFIRMED
                && m.getStatus() == BloodMatchStatus.SUGGESTED) {
            m.setConfirmedAt(LocalDateTime.now());
        }
        m.setStatus(target);
        BloodMatch saved = matchDAO.save(m);
        BloodRequest r = saved.getRequestId() == null
                ? null : requireRequest(saved.getRequestId());
        if (r != null) {
            if (target == BloodMatchStatus.COLLECTED) {
                saved.setCollectedAt(LocalDateTime.now());
                matchDAO.save(saved);
                recordDonation(saved.getDonorId(),
                        r.getBloodGroup(), saved.getUnitsMatched(),
                        r.getId(), "Blood collected for " + r.getRequestCode());
                recordHistory(r.getId(), "Donation Received",
                        saved.getUnitsMatched() + " unit(s) collected from donor "
                                + saved.getDonorId(),
                        session.currentUserId());
            } else if (target == BloodMatchStatus.DECLINED) {
                recordHistory(r.getId(), "Donor Declined",
                        "Donor " + saved.getDonorId() + " declined match #"
                                + saved.getId() + " for " + r.getRequestCode(),
                        session.currentUserId());
                notifySingleDonor(saved.getDonorId(),
                        "Your blood donation match for " + r.getRequestCode()
                                + " has been recorded as declined.",
                        NotificationPriority.INFO);
            }
            refreshRequestStatus(r);
        }
        return saved;
    }

    private void refreshRequestStatus(BloodRequest r)
            throws DataAccessException {
        int collected = matchDAO.sumCollectedUnits(r.getId());
        int committed = matchDAO.sumActiveUnits(r.getId());
        BloodRequestStatus next;
        if (collected >= r.getUnitsRequired()) {
            next = BloodRequestStatus.FULFILLED;
        } else if (collected > 0) {
            next = BloodRequestStatus.BLOOD_COLLECTED;
        } else if (committed > 0) {
            next = BloodRequestStatus.DONOR_FOUND;
        } else {
            next = BloodRequestStatus.PENDING;
        }
        if (next != r.getStatus()) {
            r.setStatus(next);
            requestDAO.save(r);
            if (next == BloodRequestStatus.FULFILLED) {
                recordHistory(r.getId(), "Request Fulfilled",
                        "Requirement fully met", session.currentUserId());
            }
        }
    }

    // ==================== DONATION RECORDING =========================

    public BloodDonation recordDonation(long donorId, BloodGroup bloodGroup,
            int units, Long requestId, String notes)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {

        requireWriteRole();
        BloodDonor donor = requireDonor(donorId);
        if (units <= 0) {
            throw new InvalidBloodDataException(
                    "donation units must be greater than zero");
        }
        LocalDate today = LocalDate.now();
        if (donor.getLastDonationDate() != null
                && donor.getLastDonationDate().plusDays(MIN_DONATION_GAP_DAYS)
                        .isAfter(today)) {
            throw new InvalidBloodDataException(
                    "Donor last donated on " + donor.getLastDonationDate()
                            + " - not yet eligible for a new donation");
        }

        BloodDonation donation = new BloodDonation();
        donation.setDonorId(donorId);
        donation.setBloodGroup(bloodGroup == null
                ? donor.getBloodGroup() : bloodGroup);
        donation.setDonationDate(today);
        donation.setRequestId(requestId);
        donation.setUnitsDonated(units);
        donation.setDonationStatus("COMPLETED");
        donation.setNotes(notes);
        donation.setRecordedBy(session.currentUserId());
        donation.setRecordedAt(LocalDateTime.now());
        BloodDonation saved = donationDAO.save(donation);

        donor.setLastDonationDate(today);
        donor.setAvailability(DonorAvailability.UNAVAILABLE);
        donorDAO.save(donor);
        return saved;
    }

    // ==================== SHORTAGE DETECTION / ALERTS ================

    /**
     * Determines whether a request can currently be fully served by the
     * matching donor pool. Shortage = fewer eligible available donors in the
     * matching group pool than the units required (spec 17, 18).
     */
    public boolean isShortage(long requestId)
            throws UnauthorizedOperationException, InvalidBloodDataException,
            DataAccessException {
        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.BLOOD_COORDINATOR, RoleType.MEDICAL_OFFICER);
        BloodRequest r = requireRequest(requestId);
        return donorDAO.countInMatchingPool(r.getBloodGroup())
                < r.getUnitsRequired();
    }

    /** Returns a map of blood group -> shortage request needing attention. */
    public List<String> findShortageMessages()
            throws UnauthorizedOperationException, DataAccessException {
        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.BLOOD_COORDINATOR, RoleType.MEDICAL_OFFICER);
        List<String> messages = new ArrayList<>();
        for (BloodRequest r : requestDAO.findOpen()) {
            int pool = donorDAO.countInMatchingPool(r.getBloodGroup());
            if (pool < r.getUnitsRequired()) {
                messages.add("BLOOD SHORTAGE ALERT: " + r.getRequestCode()
                        + " needs " + r.getUnitsRequired() + " unit(s) of "
                        + r.getBloodGroup().getLabel() + " @ " + r.getLocation()
                        + " but only " + pool
                        + " eligible available donor(s) match it.");
            }
        }
        return messages;
    }

    private void detectShortage(BloodRequest r) throws DataAccessException {
        int pool = donorDAO.countInMatchingPool(r.getBloodGroup());
        if (pool < r.getUnitsRequired()) {
            sendShortageAlert(r, pool);
        }
    }

    private void notifyAllShortageIfNeeded() throws DataAccessException {
        for (BloodRequest r : requestDAO.findOpen()) {
            int pool = donorDAO.countInMatchingPool(r.getBloodGroup());
            if (pool < r.getUnitsRequired()) {
                sendShortageAlert(r, pool);
            }
        }
    }

    private void sendShortageAlert(BloodRequest r, int availablePool)
            throws DataAccessException {
        String dedup = "BLOOD_SHORTAGE:" + r.getId();
        if (notificationDAO.findRecentByDedupKey(dedup,
                ALERT_DEDUP_SECONDS) != null) {
            return;
        }
        String message = "BLOOD SHORTAGE ALERT: " + r.getBloodGroup().getLabel()
                + " -> " + r.getRequestCode() + " needs "
                + r.getUnitsRequired() + " unit(s), matching donor pool only "
                + availablePool + ". Urgency: " + r.getPriority().getLabel();
        NotificationPriority priority = r.getPriority()
                == BloodRequestPriority.CRITICAL
                ? NotificationPriority.CRITICAL : NotificationPriority.WARNING;
        Set<Long> audience = coordinatorAudience();
        for (Long userId : audience) {
            createNotificationWithDedup(userId, dedup, message, r.getId(),
                    priority);
        }
    }

    private void createNotificationWithDedup(Long userId, String dedup,
            String message, Long eventId, NotificationPriority priority) {
        try {
            Notification n = new Notification();
            n.setRecipientUserId(userId);
            n.setType(NotificationType.BLOOD);
            n.setPriority(priority);
            n.setStatus(com.resqhub.model.NotificationStatus.UNREAD);
            n.setMessage(message);
            n.setRelatedModule("Blood Donors");
            n.setRelatedEventId(eventId);
            n.setAutoGenerated(true);
            n.setDedupKey(dedup);
            notificationDAO.save(n);
        } catch (DataAccessException ignored) {
        }
    }

    private Set<Long> coordinatorAudience() throws DataAccessException {
        Set<Long> ids = new LinkedHashSet<>();
        for (RoleType role : new RoleType[]{
                RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.BLOOD_COORDINATOR, RoleType.MEDICAL_OFFICER}) {
            for (User u : userDAO.findByRole(role)) {
                ids.add(u.getId());
            }
        }
        return ids;
    }

    // ==================== READS / QUERIES ============================

    public BloodDonor getDonor(long id) throws DataAccessException {
        return donorDAO.findById(id);
    }

    public List<BloodDonor> getAllDonors() throws DataAccessException {
        return donorDAO.findAll();
    }

    public List<BloodDonor> searchDonors(String keyword)
            throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return donorDAO.findAll();
        }
        return donorDAO.search(keyword.trim());
    }

    public List<BloodDonor> filterDonors(BloodGroup group,
            DonorAvailability availability, DonorEligibility eligibility)
            throws DataAccessException {
        List<BloodDonor> result = donorDAO.findAll();
        if (group != null) {
            result.removeIf(d -> d.getBloodGroup() != group);
        }
        if (availability != null) {
            result.removeIf(d -> d.getAvailability() != availability);
        }
        if (eligibility != null) {
            result.removeIf(d -> d.getEligibility() != eligibility);
        }
        return result;
    }

    public List<BloodDonor> findEligibleAvailable() throws DataAccessException {
        List<BloodDonor> result = new ArrayList<>();
        for (BloodDonor d : donorDAO.findAll()) {
            if (d.isSuitable()) {
                result.add(d);
            }
        }
        return result;
    }

    public BloodRequest getRequest(long id) throws DataAccessException {
        return requestDAO.findById(id);
    }

    public List<BloodRequest> getAllRequests() throws DataAccessException {
        return requestDAO.findAll();
    }

    public List<BloodRequest> getOpenRequests() throws DataAccessException {
        return requestDAO.findOpen();
    }

    /** Filtered request search (spec 23). */
    public List<BloodRequest> searchRequests(BloodGroup group,
            BloodRequestPriority priority, BloodRequestStatus status,
            Long hospitalId, String keyword) throws DataAccessException {
        return requestDAO.search(group, priority, status, hospitalId, keyword);
    }

    public List<BloodMatch> getMatchesForRequest(long requestId)
            throws DataAccessException {
        return matchDAO.findByRequest(requestId);
    }

    public List<BloodMatch> getMatchesForDonor(long donorId)
            throws DataAccessException {
        return matchDAO.findByDonor(donorId);
    }

    /** Full lifecycle history for a request, newest first (spec 25). */
    public List<RequestHistory> getRequestHistory(long requestId)
            throws DataAccessException {
        return historyDAO.findByRequest(requestId);
    }

    /** Units actually collected / received against a request (spec 4). */
    public int receivedUnits(long requestId) throws DataAccessException {
        return matchDAO.sumCollectedUnits(requestId);
    }

    /** Units still needed to fulfil the request (spec 4, 18). */
    public int remainingUnits(long requestId) throws DataAccessException {
        BloodRequest r = requestDAO.findById(requestId);
        if (r == null) {
            return 0;
        }
        int remaining = r.getUnitsRequired() - receivedUnits(requestId);
        return Math.max(0, remaining);
    }

    public BloodMatch findMatch(long id) throws InvalidBloodDataException,
            DataAccessException {
        BloodMatch m = matchDAO.findById(id);
        if (m == null) {
            throw new InvalidBloodDataException("No blood match with id " + id);
        }
        return m;
    }

    public List<BloodDonation> getDonationHistory(long donorId)
            throws DataAccessException {
        return donationDAO.findByDonor(donorId);
    }

    public List<BloodDonation> getAllDonations() throws DataAccessException {
        return donationDAO.findAll();
    }

    public BloodDonation getLastDonation(long donorId)
            throws DataAccessException {
        List<BloodDonation> list = donationDAO.findByDonor(donorId);
        return list.isEmpty() ? null : list.get(0);
    }

    // ---- statistics ---------------------------------------------------

    public int countDonors() throws DataAccessException {
        return donorDAO.findAll().size();
    }

    public int countEligibleAvailable() throws DataAccessException {
        return findEligibleAvailable().size();
    }

    public int countRequests() throws DataAccessException {
        return requestDAO.findAll().size();
    }

    public int countCriticalOpen() throws DataAccessException {
        int n = 0;
        for (BloodRequest r : requestDAO.findOpen()) {
            if (r.getPriority() == BloodRequestPriority.CRITICAL) {
                n++;
            }
        }
        return n;
    }

    public int countShortages() throws DataAccessException {
        try {
            return findShortageMessages().size();
        } catch (UnauthorizedOperationException e) {
            return 0;
        }
    }

    public int countUnitsCollected() throws DataAccessException {
        int total = 0;
        for (BloodDonation d : donationDAO.findAll()) {
            total += d.getUnitsDonated();
        }
        return total;
    }

    /** Per blood-group donor availability summary for reports. */
    public List<BloodGroupSummary> bloodGroupSummaries()
            throws DataAccessException {
        List<BloodDonor> donors = donorDAO.findAll();
        List<BloodGroupSummary> out = new ArrayList<>();
        for (BloodGroup g : BloodGroup.values()) {
            int total = 0, available = 0, eligible = 0;
            for (BloodDonor d : donors) {
                if (d.getBloodGroup() == g) {
                    total++;
                    if (d.getAvailability() == DonorAvailability.AVAILABLE) {
                        available++;
                    }
                    if (d.getEligibility() == DonorEligibility.ELIGIBLE) {
                        eligible++;
                    }
                }
            }
            out.add(new BloodGroupSummary(g, total, available, eligible));
        }
        return out;
    }

    /**
     * Aggregated blood request statistics for reporting (spec 32): totals by
     * status and blood group, plus the count of critical open requests.
     */
    public RequestStats requestStats() throws DataAccessException {
        List<BloodRequest> all = requestDAO.findAll();
        int pending = 0, matching = 0, collected = 0;
        int fulfilled = 0, cancelled = 0, criticalOpen = 0;
        Map<BloodGroup, Integer> byGroup = new EnumMap<>(BloodGroup.class);
        for (BloodRequest r : all) {
            if (r.getBloodGroup() != null) {
                byGroup.merge(r.getBloodGroup(), 1, Integer::sum);
            }
            switch (r.getStatus()) {
                case PENDING -> pending++;
                case MATCHING_DONORS, DONOR_FOUND -> matching++;
                case BLOOD_COLLECTED -> collected++;
                case FULFILLED -> fulfilled++;
                case CANCELLED -> cancelled++;
                default -> { }
            }
            if (r.getStatus().isOpen()
                    && r.getPriority() == BloodRequestPriority.CRITICAL) {
                criticalOpen++;
            }
        }
        return new RequestStats(all.size(), pending, matching, collected,
                fulfilled, cancelled, criticalOpen, byGroup);
    }

    // ==================== INTERNALS ==================================

    private BloodDonor requireDonor(long id)
            throws InvalidBloodDataException, DataAccessException {
        BloodDonor d = donorDAO.findById(id);
        if (d == null) {
            throw new InvalidBloodDataException("No blood donor with id " + id);
        }
        return d;
    }

    private BloodRequest requireRequest(long id)
            throws InvalidBloodDataException, DataAccessException {
        BloodRequest r = requestDAO.findById(id);
        if (r == null) {
            throw new InvalidBloodDataException("No blood request with id " + id);
        }
        return r;
    }

    private NotificationPriority priorityOf(BloodRequestPriority p) {
        if (p == BloodRequestPriority.CRITICAL) {
            return NotificationPriority.CRITICAL;
        }
        if (p == BloodRequestPriority.HIGH) {
            return NotificationPriority.WARNING;
        }
        return NotificationPriority.INFO;
    }

    private void requireWriteRole() throws UnauthorizedOperationException {
        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.BLOOD_COORDINATOR, RoleType.MEDICAL_OFFICER);
    }

    private void notifySelf(String message, NotificationType type,
            NotificationPriority priority) {
        try {
            notificationService.createNotification(session.currentUserId(),
                    type, priority, message, "Blood Donors", null);
        } catch (Exception ignored) {
        }
    }

    private void notifyAll(String message, NotificationType type,
            NotificationPriority priority) {
        try {
            Set<Long> audience = coordinatorAudience();
            for (Long userId : audience) {
                notificationService.createNotification(userId, type, priority,
                        message, "Blood Donors", null);
            }
        } catch (Exception ignored) {
        }
    }

    /** Notifies the coordination audience once donors have been matched. */
    private void notifyDonorsForMatch(BloodRequest r,
            List<BloodMatch> matches) {
        String message = "URGENT BLOOD REQUEST - donors matched: "
                + r.getRequestCode() + " needs " + r.getBloodGroup().getLabel()
                + " x" + r.getUnitsRequired() + " @ " + r.getLocation()
                + " (" + r.getPriority().getLabel() + "). "
                + matches.size() + " suitable donor(s) proposed.";
        notifyAll(message, NotificationType.BLOOD,
                priorityOf(r.getPriority()));
    }

    /** Notifies an individual donor about a specific match event (spec 14). */
    private void notifySingleDonor(Long donorId, String message,
            NotificationPriority priority) {
        if (donorId == null) {
            return;
        }
        try {
            BloodDonor donor = donorDAO.findById(donorId);
            if (donor == null || donor.getEmail() == null
                    || donor.getEmail().isEmpty()) {
                // No email on file - notify coordinator audience instead
                notifyAll("Donor #" + donorId + " notification: " + message,
                        NotificationType.BLOOD, priority);
                return;
            }
            Notification n = new Notification();
            n.setRecipientUserId(session.currentUserId());
            n.setType(NotificationType.BLOOD);
            n.setPriority(priority);
            n.setStatus(com.resqhub.model.NotificationStatus.UNREAD);
            n.setMessage("BLOOD MATCH NOTIFICATION for "
                    + donor.getFullName() + " [" + donor.getBloodGroup()
                    + "]: " + message);
            n.setRelatedModule("Blood Donors");
            n.setAutoGenerated(true);
            n.setDedupKey("DONOR_MATCH:" + donorId + ":" + message.hashCode());
            notificationDAO.save(n);
        } catch (DataAccessException ignored) {
        }
    }

    /**
     * Returns the full matching history timeline for a request showing each
     * proposed donor and their match status progression (spec 19: Matching
     * History). Each entry contains donor name, blood group, match status,
     * rank, location proximity, and timing.
     */
    public List<MatchingHistoryEntry> getMatchingHistory(long requestId)
            throws DataAccessException {
        List<MatchingHistoryEntry> history = new ArrayList<>();
        List<BloodMatch> matches = matchDAO.findByRequest(requestId);
        for (BloodMatch m : matches) {
            BloodDonor donor = null;
            if (m.getDonorId() != null) {
                donor = donorDAO.findById(m.getDonorId());
            }
            String donorName = donor == null ? "#" + m.getDonorId()
                    : donor.getFullName();
            String bloodGroup = donor == null ? "?"
                    : donor.getBloodGroup().getLabel();
            String location = donor == null ? "?" : donor.getLocation();
            String locationRank = m.isLocationMatched() ? "Nearby"
                    : LocationProximityUtil.rankLabel(0);
            history.add(new MatchingHistoryEntry(
                    m.getId(), donorName, bloodGroup, location, locationRank,
                    m.getStatus(), m.getUnitsMatched(), m.getDonorDistanceRank(),
                    m.getNotes(), m.getMatchedAt(), m.getConfirmedAt(),
                    m.getCollectedAt()));
        }
        return history;
    }

    /** Appends a lifecycle event to the request history (spec 25). */
    private void recordHistory(Long requestId, String event, String details,
            Long performedBy) {
        try {
            RequestHistory h = new RequestHistory();
            h.setRequestId(requestId);
            h.setEvent(event);
            h.setRemarks(details);
            h.setPerformedBy(performedBy);
            h.setPerformedAt(LocalDateTime.now());
            historyDAO.save(h);
        } catch (DataAccessException ignored) {
        }
    }

    /** Compact per-group donor availability summary for the panel/report. */
    public record BloodGroupSummary(BloodGroup group, int total,
                                    int available, int eligible) {
        public boolean hasShortageFor(int requiredUnits) {
            return eligible < requiredUnits;
        }
    }

    /** Aggregated blood request counts for reporting (spec 32). */
    public record RequestStats(int total, int pending, int matching,
                               int collected, int fulfilled, int cancelled,
                               int criticalOpen,
                               Map<BloodGroup, Integer> byGroup) {
    }

    /** A single entry in the matching history timeline (spec 19). */
    public record MatchingHistoryEntry(
            Long matchId, String donorName, String bloodGroup,
            String location, String locationRank, BloodMatchStatus status,
            int unitsMatched, int rank, String notes,
            LocalDateTime matchedAt, LocalDateTime confirmedAt,
            LocalDateTime collectedAt) {
    }
}
