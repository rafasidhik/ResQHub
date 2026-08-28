package com.resqhub.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.resqhub.dao.HospitalCapacityLogDAO;
import com.resqhub.dao.HospitalDAO;
import com.resqhub.dao.HospitalReferralDAO;
import com.resqhub.dao.NotificationDAO;
import com.resqhub.dao.UserDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidHospitalDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.Hospital;
import com.resqhub.model.HospitalCapacityLog;
import com.resqhub.model.HospitalFacility;
import com.resqhub.model.HospitalReferral;
import com.resqhub.model.HospitalReferralStatus;
import com.resqhub.model.HospitalStatus;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationPriority;
import com.resqhub.model.NotificationStatus;
import com.resqhub.model.NotificationType;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.model.Victim;
import com.resqhub.util.ValidationUtil;

/**
 * Hospital Management service - registers hospitals, tracks bed capacity
 * (occupied / available), emergency facilities and status, validates and
 * records emergency victim referrals against hospital capacity and
 * generates near-capacity alerts. Provides the SQL-aggregation data behind
 * the Hospital Capacity report.
 *
 * Capacity model:
 *   availableBeds = totalBeds - occupiedBeds        (derived, spec 7)
 *   open referrals hold beds so allocations can't over-commit (spec 12)
 */
public class HospitalService {

    private static final int ALERT_DEDUP_SECONDS = 6 * 60 * 60;
    private static final double NEAR_CAPACITY_RATIO = 0.9;

    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final HospitalReferralDAO referralDAO = new HospitalReferralDAO();
    private final HospitalCapacityLogDAO logDAO = new HospitalCapacityLogDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final VictimService victimService = new VictimService();
    private final DisasterService disasterService = new DisasterService();
    private final SessionManager session = SessionManager.getInstance();

    // ---- registration & profile ---------------------------------------

    public Hospital createHospital(String name, String hospitalId,
            String district, String city, String area, String address,
            String phone, String emergencyContact, String email,
            int totalBeds, int occupiedBeds, Set<HospitalFacility> facilities,
            HospitalStatus status, Long disasterId)
            throws UnauthorizedOperationException, InvalidHospitalDataException,
            DataAccessException {

        requireWriteRole();
        Hospital h = new Hospital();
        h.setName(name);
        h.setHospitalId(hospitalId);
        h.setDistrict(district);
        h.setCity(city);
        h.setArea(area);
        h.setAddress(address);
        h.setPhone(phone);
        h.setEmergencyContact(emergencyContact);
        h.setEmail(email);
        h.setTotalBeds(totalBeds);
        h.setOccupiedBeds(occupiedBeds);
        h.setFacilities(facilities);
        h.setStatus(status == null ? HospitalStatus.AVAILABLE : status);
        h.setDisasterId(disasterId);
        h.setCreatedBy(session.currentUserId());
        validateBasic(h);
        Hospital saved = hospitalDAO.save(h);
        notifySelf("Hospital registered: " + saved.getName() + " ("
                + saved.getHospitalId() + ") with "
                + saved.availableBeds() + " beds available.",
                NotificationPriority.INFO);
        return saved;
    }

    public Hospital updateHospital(Hospital h)
            throws UnauthorizedOperationException, InvalidHospitalDataException,
            DataAccessException {

        requireWriteRole();
        if (h == null || h.getId() == null) {
            throw new InvalidHospitalDataException(
                    "a hospital to update is required");
        }
        validateBasic(h);
        Hospital saved = hospitalDAO.save(h);
        recheckCapacityStatus(saved.getId());
        return saved;
    }

    public Hospital updateStatus(long hospitalId, HospitalStatus status)
            throws UnauthorizedOperationException, InvalidHospitalDataException,
            DataAccessException {

        requireWriteRole();
        if (status == null) {
            throw new InvalidHospitalDataException(
                    "a status must be selected");
        }
        Hospital h = requireExisting(hospitalId);
        h.setStatus(status);
        Hospital saved = hospitalDAO.save(h);
        notifySelf("Hospital status updated: " + saved.getName() + " -> "
                + status.getLabel(), NotificationPriority.INFO);
        return saved;
    }

    /** Manual bed update: validates, re-derives status, logs history
     *  (specs 6, 7, 9, 10, 19, 18). */
    public Hospital updateOccupiedBeds(long hospitalId, int newOccupied,
            String reason)
            throws UnauthorizedOperationException, InvalidHospitalDataException,
            DataAccessException {

        requireWriteRole();
        Hospital h = requireExisting(hospitalId);
        if (newOccupied < 0) {
            throw new InvalidHospitalDataException(
                    "occupied beds cannot be negative");
        }
        if (newOccupied > h.getTotalBeds()) {
            throw new InvalidHospitalDataException(
                    "occupied beds (" + newOccupied + ") cannot exceed "
                            + "total capacity (" + h.getTotalBeds() + ")");
        }
        int previous = h.getOccupiedBeds();
        h.setOccupiedBeds(newOccupied);
        h.setStatus(deriveStatus(h));
        Hospital saved = hospitalDAO.save(h);
        recordCapacityLog(saved.getId(), previous, newOccupied,
                saved.availableBeds(),
                reason == null || reason.isEmpty() ? "Occupancy updated"
                        : reason);
        checkAndAlertCapacity(saved);
        return saved;
    }

    // ---- facilities (spec 8) ------------------------------------------

    /** Adds an emergency facility to a hospital. */
    public Hospital addFacility(long hospitalId, HospitalFacility facility)
            throws UnauthorizedOperationException, InvalidHospitalDataException,
            DataAccessException {

        requireWriteRole();
        if (facility == null) {
            throw new InvalidHospitalDataException(
                    "a facility must be selected");
        }
        Hospital h = requireExisting(hospitalId);
        Set<HospitalFacility> set = new LinkedHashSet<>(h.getFacilities());
        set.add(facility);
        h.setFacilities(set);
        return hospitalDAO.save(h);
    }

    /** Removes an emergency facility from a hospital. */
    public Hospital removeFacility(long hospitalId, HospitalFacility facility)
            throws UnauthorizedOperationException, InvalidHospitalDataException,
            DataAccessException {

        requireWriteRole();
        if (facility == null) {
            throw new InvalidHospitalDataException(
                    "a facility must be selected");
        }
        Hospital h = requireExisting(hospitalId);
        Set<HospitalFacility> set = new LinkedHashSet<>(h.getFacilities());
        set.remove(facility);
        h.setFacilities(set);
        return hospitalDAO.save(h);
    }

    // ---- emergency victim referral (specs 11, 12, 13, 14) -------------

    /** Refers a victim to a hospital after validating bed capacity and the
     *  required facilities. */
    public HospitalReferral referVictim(long hospitalId, Long victimId,
            String victimName, int bedsRequired,
            Set<HospitalFacility> requiredFacilities, String reason,
            String notes, Long disasterId)
            throws UnauthorizedOperationException, InvalidHospitalDataException,
            DataAccessException {

        requireWriteRole();
        Hospital h = requireExisting(hospitalId);
        List<String> errors = new ArrayList<>();
        if (bedsRequired <= 0) {
            errors.add("at least one bed is required for the referral");
        }
        if (reason == null || reason.trim().isEmpty()) {
            errors.add("a medical reason is required");
        }
        if ((victimId == null || victimId <= 0)
                && (victimName == null || victimName.trim().isEmpty())) {
            errors.add("a victim or a victim name is required");
        }
        if (!errors.isEmpty()) {
            throw new InvalidHospitalDataException(String.join("; ", errors));
        }

        // capacity + facility suitability (spec 12)
        int committed = referralDAO.sumOpenBeds(hospitalId);
        int available = h.availableBeds() - committed;
        if (bedsRequired > h.availableBeds()) {
            throw new InvalidHospitalDataException(
                    "Insufficient bed capacity: " + h.getName() + " has only "
                            + h.availableBeds() + " beds available, but "
                            + bedsRequired + " were required");
        }
        if (bedsRequired > available) {
            throw new InvalidHospitalDataException(
                    "Beds are over-committed: " + committed + " of "
                            + h.availableBeds() + " available beds are already "
                            + "held by open referrals, leaving only "
                            + available + " for this case");
        }
        List<String> missing = missingFacilities(h, requiredFacilities);
        if (!missing.isEmpty()) {
            throw new InvalidHospitalDataException(
                    h.getName() + " cannot take this case - missing "
                            + String.join(", ", missing));
        }

        HospitalReferral r = new HospitalReferral();
        r.setHospitalId(hospitalId);
        r.setVictimId(victimId);
        r.setVictimName(victimName);
        r.setBedsRequired(bedsRequired);
        r.setRequiredFacilities(requiredFacilities);
        r.setStatus(HospitalReferralStatus.PENDING);
        r.setReferredBy(session.currentUserId());
        r.setReferredAt(LocalDateTime.now());
        r.setReason(reason);
        r.setNotes(notes);
        r.setDisasterId(disasterId);
        HospitalReferral saved = referralDAO.save(r);
        notifySelf("Referral created: " + (saved.getVictimName() == null
                ? "victim #" + saved.getVictimId() : saved.getVictimName())
                + " -> " + h.getName() + " (" + bedsRequired + " beds).",
                NotificationPriority.INFO);
        return saved;
    }

    /** Moves a referral through its lifecycle, applying / releasing hospital
     *  beds on admission and discharge (specs 11, 13, 14). */
    public HospitalReferral setReferralStatus(long referralId,
            HospitalReferralStatus target)
            throws UnauthorizedOperationException, InvalidHospitalDataException,
            DataAccessException {

        requireWriteRole();
        if (target == null) {
            throw new InvalidHospitalDataException(
                    "a referral status must be selected");
        }
        HospitalReferral r = requireExistingReferral(referralId);
        Hospital h = requireExisting(r.getHospitalId());
        boolean wasAdmitted = r.getStatus() == HospitalReferralStatus.ADMITTED;

        if (target == HospitalReferralStatus.ADMITTED
                && r.getStatus() != HospitalReferralStatus.ADMITTED) {
            // apply beds: validate capacity then admit
            int committed = referralDAO.sumOpenBeds(h.getId());
            applyBeds(h, r, committed);
        } else if (target == HospitalReferralStatus.DISCHARGED && wasAdmitted) {
            releaseBeds(h, r);
        }

        r.setStatus(target);
        if (target == HospitalReferralStatus.ADMITTED) {
            r.setBedsApplied(true);
        }
        if (isClosed(target)) {
            r.setClosedAt(LocalDateTime.now());
        }
        HospitalReferral saved = referralDAO.save(r);
        checkAndAlertCapacity(h);
        notifySelf("Referral #" + referralId + " -> "
                + target.getLabel(), NotificationPriority.INFO);
        return saved;
    }

    // ---- capacity alerts (specs 17, 20) -------------------------------

    /** Generates dedup'd alerts for hospitals that are full or near capacity.
     *  Returns the number of new alerts created. */
    public int generateCapacityAlerts()
            throws UnauthorizedOperationException, DataAccessException {

        requireWriteRole();
        Set<Long> audience = adminAndOfficerIds();
        if (audience.isEmpty()) {
            return 0;
        }
        List<Hospital> critical = new ArrayList<>();
        critical.addAll(hospitalDAO.findNearCapacity());
        critical.addAll(hospitalDAO.findFull());
        if (critical.isEmpty()) {
            return 0;
        }
        int created = 0;
        for (Hospital h : critical) {
            String dedup = "HOSPITAL_CAPACITY:" + h.getId();
            if (notificationDAO.findRecentByDedupKey(dedup,
                    ALERT_DEDUP_SECONDS) != null) {
                continue;
            }
            String level = h.getStatus() == HospitalStatus.FULL
                    ? "FULL" : "NEAR FULL";
            String message = "HOSPITAL CAPACITY WARNING: " + h.getName()
                    + " - total " + h.getTotalBeds() + ", occupied "
                    + h.getOccupiedBeds() + ", available "
                    + h.availableBeds() + ", status " + level + ".";
            for (Long userId : audience) {
                Notification n = new Notification();
                n.setRecipientUserId(userId);
                n.setType(NotificationType.HOSPITAL);
                n.setPriority(NotificationPriority.WARNING);
                n.setStatus(NotificationStatus.UNREAD);
                n.setMessage(message);
                n.setRelatedModule("Hospital Management");
                n.setRelatedEventId(h.getId());
                n.setAutoGenerated(true);
                n.setDedupKey(dedup);
                notificationDAO.save(n);
                created++;
            }
        }
        return created;
    }

    // ---- reads --------------------------------------------------------

    public Hospital getHospital(long id) throws DataAccessException {
        return hospitalDAO.findById(id);
    }

    public Hospital requireExisting(long hospitalId)
            throws InvalidHospitalDataException, DataAccessException {
        Hospital h = hospitalDAO.findById(hospitalId);
        if (h == null) {
            throw new InvalidHospitalDataException(
                    "No hospital with id " + hospitalId);
        }
        return h;
    }

    public List<Hospital> getAllHospitals() throws DataAccessException {
        return hospitalDAO.findAll();
    }

    public List<Hospital> search(String keyword) throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return hospitalDAO.findAll();
        }
        return hospitalDAO.search(keyword.trim());
    }

    public List<Hospital> filter(String keyword, String district,
            HospitalStatus status, Integer minAvailable,
            HospitalFacility facility) throws DataAccessException {
        return hospitalDAO.filter(keyword, district, status, minAvailable,
                facility);
    }

    public List<Hospital> findAccepting() throws DataAccessException {
        return hospitalDAO.findAccepting();
    }

    public List<Hospital> findNearCapacity() throws DataAccessException {
        return hospitalDAO.findNearCapacity();
    }

    public List<Hospital> findFull() throws DataAccessException {
        return hospitalDAO.findFull();
    }

    public List<HospitalReferral> getReferrals(long hospitalId)
            throws DataAccessException {
        return referralDAO.findByHospital(hospitalId);
    }

    public List<HospitalReferral> getAllReferrals()
            throws DataAccessException {
        return referralDAO.findAll();
    }

    public List<HospitalReferral> getOpenReferrals()
            throws DataAccessException {
        return referralDAO.findOpen();
    }

    public List<HospitalReferral> getReferralsByStatus(
            HospitalReferralStatus status) throws DataAccessException {
        return referralDAO.findByStatus(status);
    }

    public HospitalReferral requireExistingReferral(long referralId)
            throws InvalidHospitalDataException, DataAccessException {
        HospitalReferral r = referralDAO.findById(referralId);
        if (r == null) {
            throw new InvalidHospitalDataException(
                    "No referral with id " + referralId);
        }
        return r;
    }

    public List<HospitalCapacityLog> getCapacityLogs(long hospitalId)
            throws DataAccessException {
        return logDAO.findByHospital(hospitalId);
    }

    public List<HospitalCapacityLog> getAllCapacityLogs()
            throws DataAccessException {
        return logDAO.findAll();
    }

    public List<Victim> getAllVictims() throws DataAccessException {
        return victimService.getAllVictims();
    }

    public List<com.resqhub.model.Disaster> getAllDisasters()
            throws DataAccessException {
        return disasterService.getAllDisasters();
    }

    public Victim getVictim(long victimId) throws DataAccessException {
        return victimService.getVictim(victimId);
    }

    public int availableFoodStock() throws DataAccessException {
        return hospitalDAO.sumAvailableBeds();
    }

    // ---- stats --------------------------------------------------------

    public int countHospitals() throws DataAccessException {
        return hospitalDAO.findAll().size();
    }

    public int countActive() throws DataAccessException {
        int n = 0;
        for (Hospital h : hospitalDAO.findAll()) {
            if (h.getStatus() != HospitalStatus.INACTIVE) {
                n++;
            }
        }
        return n;
    }

    public int countAccepting() throws DataAccessException {
        return hospitalDAO.findAccepting().size();
    }

    public int countNearCapacity() throws DataAccessException {
        return hospitalDAO.findNearCapacity().size();
    }

    public int countFull() throws DataAccessException {
        return hospitalDAO.findFull().size();
    }

    public int totalBeds() throws DataAccessException {
        int total = 0;
        for (Hospital h : hospitalDAO.findAll()) {
            total += h.getTotalBeds();
        }
        return total;
    }

    public int totalOccupiedBeds() throws DataAccessException {
        int total = 0;
        for (Hospital h : hospitalDAO.findAll()) {
            total += h.getOccupiedBeds();
        }
        return total;
    }

    public int totalAvailableBeds() throws DataAccessException {
        int total = 0;
        for (Hospital h : hospitalDAO.findAll()) {
            total += h.availableBeds();
        }
        return total;
    }

    public void deleteHospital(long hospitalId)
            throws UnauthorizedOperationException, InvalidHospitalDataException,
            DataAccessException {
        session.requireRole(RoleType.ADMIN);
        requireExisting(hospitalId);
        hospitalDAO.deleteById(hospitalId);
    }

    // ---- internals ----------------------------------------------------

    private void validateBasic(Hospital h) throws InvalidHospitalDataException {
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(h.getName())) {
            errors.add("hospital name is required");
        }
        if (!ValidationUtil.requireNonBlank(h.getHospitalId())) {
            errors.add("hospital id is required");
        }
        if (!ValidationUtil.requireNonBlank(h.getDistrict())) {
            errors.add("district is required");
        }
        if (!ValidationUtil.isPositive(h.getTotalBeds())) {
            errors.add("total bed capacity must be greater than zero");
        }
        if (h.getOccupiedBeds() < 0) {
            errors.add("occupied beds cannot be negative");
        }
        if (h.getOccupiedBeds() > h.getTotalBeds()) {
            errors.add("occupied beds (" + h.getOccupiedBeds()
                    + ") cannot exceed total capacity (" + h.getTotalBeds()
                    + ")");
        }
        if (!errors.isEmpty()) {
            throw new InvalidHospitalDataException(
                    String.join("; ", errors));
        }
    }

    private boolean isClosed(HospitalReferralStatus s) {
        return s == HospitalReferralStatus.DISCHARGED
                || s == HospitalReferralStatus.REJECTED
                || s == HospitalReferralStatus.CANCELLED;
    }

    private void applyBeds(Hospital h, HospitalReferral r, int committed)
            throws InvalidHospitalDataException, DataAccessException {
        int available = h.availableBeds() - committed;
        if (r.getBedsRequired() > h.availableBeds()
                || r.getBedsRequired() > available) {
            throw new InvalidHospitalDataException(
                    "Cannot admit - " + h.getName() + " has only "
                            + available + " beds free for new admissions "
                            + "(" + r.getBedsRequired() + " needed)");
        }
        int previous = h.getOccupiedBeds();
        h.setOccupiedBeds(previous + r.getBedsRequired());
        h.setStatus(deriveStatus(h));
        hospitalDAO.save(h);
        recordCapacityLog(h.getId(), previous, h.getOccupiedBeds(),
                h.availableBeds(), "Admitted referral - "
                        + (r.getVictimName() == null ? "victim #"
                                + r.getVictimId() : r.getVictimName()));
    }

    private void releaseBeds(Hospital h, HospitalReferral r)
            throws DataAccessException {
        int previous = h.getOccupiedBeds();
        int released = Math.min(r.getBedsRequired(), previous);
        h.setOccupiedBeds(previous - released);
        h.setStatus(deriveStatus(h));
        hospitalDAO.save(h);
        recordCapacityLog(h.getId(), previous, h.getOccupiedBeds(),
                h.availableBeds(), "Discharged referral - "
                        + (r.getVictimName() == null ? "victim #"
                                + r.getVictimId() : r.getVictimName()));
    }

    private void recordCapacityLog(long hospitalId, int previous,
            int updated, int available, String reason)
            throws DataAccessException {
        HospitalCapacityLog log = new HospitalCapacityLog();
        log.setHospitalId(hospitalId);
        log.setPreviousOccupied(previous);
        log.setUpdatedOccupied(updated);
        log.setAvailableBeds(available);
        log.setReason(reason);
        log.setChangedBy(session.currentUserId());
        log.setChangedAt(LocalDateTime.now());
        logDAO.save(log);
    }

    private List<String> missingFacilities(Hospital h,
            Set<HospitalFacility> required) {
        List<String> missing = new ArrayList<>();
        if (required == null || required.isEmpty()) {
            return missing;
        }
        for (HospitalFacility f : required) {
            if (!h.getFacilities().contains(f)) {
                missing.add(f.getLabel());
            }
        }
        return missing;
    }

    private void recheckCapacityStatus(long hospitalId)
            throws DataAccessException, InvalidHospitalDataException {
        Hospital h = requireExisting(hospitalId);
        HospitalStatus derived = deriveStatus(h);
        if (h.getStatus() != derived) {
            h.setStatus(derived);
            hospitalDAO.save(h);
        }
    }

    private HospitalStatus deriveStatus(Hospital h) {
        if (h.getTotalBeds() <= 0) {
            return HospitalStatus.INACTIVE;
        }
        double ratio = (double) h.getOccupiedBeds() / h.getTotalBeds();
        if (h.availableBeds() <= 0 || ratio >= 1.0) {
            return HospitalStatus.FULL;
        }
        if (ratio >= NEAR_CAPACITY_RATIO) {
            return HospitalStatus.LIMITED_CAPACITY;
        }
        return HospitalStatus.AVAILABLE;
    }

    private void checkAndAlertCapacity(Hospital h) {
        try {
            if (h.availableBeds() <= 0 || h.isNearCapacity()) {
                generateCapacityAlerts();
            }
        } catch (Exception ignored) {
            // a failed alert must never block the primary operation
        }
    }

    private Set<Long> adminAndOfficerIds() throws DataAccessException {
        Set<Long> ids = new LinkedHashSet<>();
        for (User u : userDAO.findByRole(RoleType.ADMIN)) {
            ids.add(u.getId());
        }
        for (User u : userDAO.findByRole(RoleType.RESCUE_OFFICER)) {
            ids.add(u.getId());
        }
        for (User u : userDAO.findByRole(RoleType.CAMP_MANAGER)) {
            ids.add(u.getId());
        }
        return ids;
    }

    private void notifySelf(String message, NotificationPriority priority) {
        try {
            new NotificationService().createNotification(
                    session.currentUserId(), NotificationType.HOSPITAL,
                    priority, message, "Hospital Management", null);
        } catch (Exception ignored) {
            // never block on a notification
        }
    }

    private void requireWriteRole() throws UnauthorizedOperationException {
        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
    }
}
