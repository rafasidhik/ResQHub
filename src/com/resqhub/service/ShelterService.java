package com.resqhub.service;

import java.util.ArrayList;
import java.util.List;

import com.resqhub.dao.ShelterAllocationDAO;
import com.resqhub.dao.ShelterDAO;
import com.resqhub.dao.ShelterFacilityDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidShelterDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.RoleType;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterAllocationStatus;
import com.resqhub.model.ShelterFacility;
import com.resqhub.model.ShelterOperationalStatus;
import com.resqhub.model.ShelterStatus;
import com.resqhub.util.ValidationUtil;

/**
 * Shelter lifecycle management: registration, capacity monitoring,
 * facility and accessibility tracking, and victim/family allocation.
 *
 * Business rules enforced here:
 *   - capacity / occupancy must be valid (non-negative, occupancy <= capacity)
 *   - allocations cannot exceed the shelter's available capacity (overcapacity
 *     prevention)
 *   - occupancy is recomputed from ACTIVE allocations after every change
 *   - the operational status is auto-adjusted as a shelter fills up
 *   - a victim's shelter_status flag is kept in sync on allocation / release
 *
 * Writes are restricted to ADMIN, RESCUE_OFFICER and CAMP_MANAGER; reads
 * are open to every logged-in role.
 */
public class ShelterService {

    private final ShelterDAO shelterDAO = new ShelterDAO();
    private final ShelterFacilityDAO facilityDAO = new ShelterFacilityDAO();
    private final ShelterAllocationDAO allocationDAO = new ShelterAllocationDAO();
    private final SessionManager session = SessionManager.getInstance();

    // ── registration / update ────────────────────────────────────────

    public Shelter createShelter(String name, String code, String district,
                                 String city, String address,
                                 String locationDescription, int maxCapacity,
                                 int currentOccupancy, String contactNumber,
                                 String managerName, Long disasterId,
                                 boolean wheelchair, boolean elderly,
                                 boolean medical, boolean assistance,
                                 ShelterOperationalStatus status)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);

        Shelter shelter = new Shelter();
        shelter.setName(name);
        shelter.setCode(code);
        shelter.setDistrict(district);
        shelter.setCity(city);
        shelter.setAddress(address);
        shelter.setLocationDescription(locationDescription);
        shelter.setMaxCapacity(maxCapacity);
        shelter.setCurrentOccupancy(currentOccupancy);
        shelter.setContactNumber(contactNumber);
        shelter.setManagerName(managerName);
        shelter.setDisasterId(disasterId);
        shelter.setWheelchairAccessible(wheelchair);
        shelter.setElderlyFriendly(elderly);
        shelter.setMedicalAccessible(medical);
        shelter.setSpecialAssistance(assistance);
        shelter.setOperationalStatus(status == null
                ? ShelterOperationalStatus.AVAILABLE : status);
        shelter.setCreatedBy(session.currentUserId());

        validateBasic(shelter);
        return shelterDAO.save(shelter);
    }

    public Shelter updateShelter(Shelter shelter)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        if (shelter == null || shelter.getId() == null) {
            throw new InvalidShelterDataException(
                    "Cannot update an unsaved shelter");
        }
        validateBasic(shelter);
        return shelterDAO.save(shelter);
    }

    /** Manual occupancy override (e.g. walk-in headcount adjustments). */
    public Shelter updateOccupancy(long shelterId, int newOccupancy)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        Shelter shelter = requireExisting(shelterId);
        if (!ValidationUtil.isNonNegative(newOccupancy)) {
            throw new InvalidShelterDataException(
                    "Current occupancy cannot be negative");
        }
        if (newOccupancy > shelter.getMaxCapacity()) {
            throw new InvalidShelterDataException(
                    "Occupancy (" + newOccupancy + ") exceeds maximum capacity ("
                            + shelter.getMaxCapacity() + ")");
        }
        shelter.setCurrentOccupancy(newOccupancy);
        shelter.setOperationalStatus(deriveStatus(shelter));
        return shelterDAO.save(shelter);
    }

    public Shelter updateStatus(long shelterId, ShelterOperationalStatus status)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        if (status == null) {
            throw new InvalidShelterDataException("Shelter status must be selected");
        }
        Shelter shelter = requireExisting(shelterId);
        shelter.setOperationalStatus(status);
        return shelterDAO.save(shelter);
    }

    // ── facility management ──────────────────────────────────────────

    /** Adds a facility if absent, otherwise updates it. */
    public ShelterFacility addFacility(long shelterId, String name,
                                       boolean available)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        requireExisting(shelterId);
        String clean = ValidationUtil.clean(name);
        if (!ValidationUtil.requireNonBlank(clean)) {
            throw new InvalidShelterDataException(
                    "Facility name is required");
        }
        ShelterFacility f = new ShelterFacility();
        f.setShelterId(shelterId);
        f.setFacilityName(clean);
        f.setAvailable(available);
        return facilityDAO.save(f);
    }

    public void removeFacility(long shelterId, String name)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        requireExisting(shelterId);
        facilityDAO.deleteByShelterAndName(shelterId, name);
    }

    public List<ShelterFacility> getFacilities(long shelterId)
            throws DataAccessException {
        return facilityDAO.findByShelter(shelterId);
    }

    // ── allocation ───────────────────────────────────────────────────

    /**
     * Allocates a victim / family to a shelter as an immediate ACTIVE
     * placement. Fails if the family size exceeds the shelter's available
     * capacity (overcapacity prevention) or if the victim already has an
     * open allocation (duplicate prevention). Occupancy is incremented,
     * the victim's shelter flag is raised and the shelter status is
     * re-derived.
     */
    public ShelterAllocation allocateVictim(long shelterId, Long victimId,
                                            String familyName, int peopleCount,
                                            String notes)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        requireCanPlace(shelterId, victimId, peopleCount, true);

        ShelterAllocation a = new ShelterAllocation();
        a.setShelterId(shelterId);
        a.setVictimId(victimId);
        a.setFamilyName(familyName);
        a.setPeopleCount(peopleCount);
        a.setNotes(notes);
        a.setStatus(ShelterAllocationStatus.ACTIVE);
        a.setAllocatedBy(session.currentUserId());
        ShelterAllocation saved = allocationDAO.save(a);

        syncVictimIn(victimId);
        openSpace(shelterId, peopleCount);
        return saved;
    }

    /**
     * Creates a PENDING reservation for a victim / family. The family is
     * NOT yet counted toward shelter occupancy until the reservation is
     * confirmed ({@link #confirmPending}). Still rejects overcapacity and
     * duplicate active allocations.
     */
    public ShelterAllocation createPendingAllocation(long shelterId,
                                                     Long victimId,
                                                     String familyName,
                                                     int peopleCount,
                                                     String notes)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        requireCanPlace(shelterId, victimId, peopleCount, false);

        ShelterAllocation a = new ShelterAllocation();
        a.setShelterId(shelterId);
        a.setVictimId(victimId);
        a.setFamilyName(familyName);
        a.setPeopleCount(peopleCount);
        a.setNotes(notes);
        a.setStatus(ShelterAllocationStatus.PENDING);
        a.setAllocatedBy(session.currentUserId());
        return allocationDAO.save(a);
    }

    /** Converts a PENDING reservation into an ACTIVE placement, opening
     *  the occupied space and raising the victim's shelter flag. */
    public ShelterAllocation confirmPending(long allocationId)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        ShelterAllocation a = loadAllocation(allocationId);
        if (a.getStatus() != ShelterAllocationStatus.PENDING) {
            throw new InvalidShelterDataException(
                    "Only a PENDING reservation can be confirmed");
        }
        Shelter shelter = requireExisting(a.getShelterId());
        if (shelter.availableCapacity() < a.getPeopleCount()) {
            throw new InvalidShelterDataException("Cannot confirm: shelter "
                    + shelter.getName() + " now has only "
                    + shelter.availableCapacity() + " space but "
                    + a.getPeopleCount() + " people need it");
        }
        allocationDAO.updateStatus(allocationId, ShelterAllocationStatus.ACTIVE);
        a.setStatus(ShelterAllocationStatus.ACTIVE);
        syncVictimIn(a.getVictimId());
        openSpace(a.getShelterId(), a.getPeopleCount());
        return a;
    }

    /** Marks an ACTIVE allocation as CHECKED IN once the family arrives.
     *  Occupancy is unchanged - the family was already counted. */
    public ShelterAllocation checkIn(long allocationId)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        ShelterAllocation a = loadAllocation(allocationId);
        if (a.getStatus() != ShelterAllocationStatus.ACTIVE) {
            throw new InvalidShelterDataException(
                    "Only an Active allocation can be checked in");
        }
        allocationDAO.updateStatus(allocationId,
                ShelterAllocationStatus.CHECKED_IN);
        a.setStatus(ShelterAllocationStatus.CHECKED_IN);
        return a;
    }

    /** Completes an allocation (family left / accommodated), freeing the
     *  occupied space once it was actually occupying. */
    public ShelterAllocation complete(long allocationId)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        return closeAllocation(allocationId,
                ShelterAllocationStatus.COMPLETED);
    }

    /** Cancels an allocation (place never taken / vacated early), freeing
     *  occupied space once it was actually occupying. */
    public ShelterAllocation cancel(long allocationId)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        return closeAllocation(allocationId,
                ShelterAllocationStatus.CANCELLED);
    }

    /** Releases a family/victim from a shelter, decreasing occupancy and
     *  lowering the victim shelter flag. */
    public ShelterAllocation releaseAllocation(long allocationId)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        return closeAllocation(allocationId,
                ShelterAllocationStatus.RELEASED);
    }

    /** Shared terminal transition: closes the allocation and frees the
     *  occupied space (and victim flag) when the source state occupied. */
    private ShelterAllocation closeAllocation(long allocationId,
                              ShelterAllocationStatus target)
            throws InvalidShelterDataException, DataAccessException {
        ShelterAllocation a = loadAllocation(allocationId);
        if (a.getStatus() == ShelterAllocationStatus.COMPLETED
                || a.getStatus() == ShelterAllocationStatus.CANCELLED
                || a.getStatus() == ShelterAllocationStatus.RELEASED) {
            throw new InvalidShelterDataException("Allocation " + allocationId
                    + " is already " + a.getStatus().getLabel()
                    + " - it cannot be closed again");
        }
        boolean occupied = a.getStatus().isOccupying();
        allocationDAO.updateStatus(allocationId, target);
        a.setStatus(target);
        if (occupied) {
            syncVictimOut(a.getVictimId());
            freeSpace(a.getShelterId(), a.getPeopleCount());
        }
        return a;
    }

    /**
     * Validates a placement before it is created: shelter exists + is
     * accepting, positive people count, enough available capacity, and
     * (when a victim is named) no duplicate open allocation for them.
     */
    private void requireCanPlace(long shelterId, Long victimId,
                                 int peopleCount, boolean requiringOccupancy)
            throws InvalidShelterDataException, DataAccessException {
        Shelter shelter = requireExisting(shelterId);
        if (!shelter.getOperationalStatus().isAccepting()) {
            throw new InvalidShelterDataException("Shelter " + shelter.getName()
                    + " is " + shelter.getOperationalStatus().getLabel()
                    + " and cannot accept new people");
        }
        if (!ValidationUtil.isPositive(peopleCount)) {
            throw new InvalidShelterDataException(
                    "People count must be at least 1");
        }
        if (shelter.availableCapacity() < peopleCount) {
            throw new InvalidShelterDataException(
                    "Allocation not possible: shelter has only "
                            + shelter.availableCapacity()
                            + " space but " + peopleCount + " people need it");
        }
        if (victimId != null && allocationDAO
                .findActiveByVictim(victimId) != null) {
            throw new InvalidShelterDataException(
                    "Victim #" + victimId + " already has an open allocation");
        }
    }

    /** Raises the victim shelter flag after an ACTIVE placement. */
    private void syncVictimIn(Long victimId) {
        if (victimId == null) {
            return;
        }
        try {
            new VictimService().markShelterStatus(victimId,
                    ShelterStatus.IN_SHELTER);
        } catch (Exception ignored) {
            // best-effort; the allocation still stands
        }
    }

    /** Lowers the victim shelter flag after they leave a shelter. */
    private void syncVictimOut(Long victimId) {
        if (victimId == null) {
            return;
        }
        try {
            new VictimService().markShelterStatus(victimId,
                    ShelterStatus.NOT_SHELTERED);
        } catch (Exception ignored) {
            // best-effort
        }
    }

    /** Adds people to a shelter's occupancy and re-derives its status. */
    private void openSpace(long shelterId, int peopleCount)
            throws InvalidShelterDataException, DataAccessException {
        Shelter updated = requireExisting(shelterId);
        updated.setCurrentOccupancy(updated.getCurrentOccupancy() + peopleCount);
        updated.setOperationalStatus(deriveStatus(updated));
        shelterDAO.save(updated);
    }

    /** Removes people from a shelter's occupancy and re-derives status. */
    private void freeSpace(long shelterId, int peopleCount)
            throws InvalidShelterDataException, DataAccessException {
        Shelter updated = requireExisting(shelterId);
        updated.setCurrentOccupancy(Math.max(0,
                updated.getCurrentOccupancy() - peopleCount));
        updated.setOperationalStatus(deriveStatus(updated));
        shelterDAO.save(updated);
    }

    public List<ShelterAllocation> getAllocations(long shelterId)
            throws DataAccessException {
        return allocationDAO.findByShelter(shelterId);
    }

    public List<ShelterAllocation> getAllAllocations()
            throws DataAccessException {
        return allocationDAO.findAll();
    }

    public List<ShelterAllocation> getByStatus(ShelterAllocationStatus status)
            throws DataAccessException {
        return allocationDAO.findByStatus(status);
    }

    public List<Shelter> getAcceptingShelters() throws DataAccessException {
        List<Shelter> out = new ArrayList<>();
        for (Shelter s : shelterDAO.findAll()) {
            if (s.getOperationalStatus().isAccepting()
                    && s.availableCapacity() > 0) {
                out.add(s);
            }
        }
        return out;
    }

    private ShelterAllocation loadAllocation(long allocationId)
            throws InvalidShelterDataException, DataAccessException {
        ShelterAllocation a = allocationDAO.findById(allocationId);
        if (a == null) {
            throw new InvalidShelterDataException(
                    "No allocation with id " + allocationId);
        }
        return a;
    }

    // ── queries ──────────────────────────────────────────────────────

    public List<Shelter> getAllShelters() throws DataAccessException {
        return shelterDAO.findAll();
    }

    public List<Shelter> search(String keyword) throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllShelters();
        }
        return shelterDAO.search(keyword.trim());
    }

    public List<Shelter> filter(String keyword, String statusLabel,
                                ShelterOperationalStatus exactStatus,
                                Integer minAvailable, Boolean accessibleOnly)
            throws DataAccessException {
        List<Shelter> base = keyword == null || keyword.trim().isEmpty()
                ? getAllShelters()
                : shelterDAO.search(keyword.trim());
        List<Shelter> out = new ArrayList<>();
        for (Shelter s : base) {
            if (exactStatus != null
                    && s.getOperationalStatus() != exactStatus) {
                continue;
            }
            if (statusLabel != null
                    && !statusLabel.equals(s.getOperationalStatus().getLabel())) {
                continue;
            }
            if (minAvailable != null && s.availableCapacity() < minAvailable) {
                continue;
            }
            if (Boolean.TRUE.equals(accessibleOnly)
                    && !(s.isWheelchairAccessible() || s.isElderlyFriendly()
                            || s.isMedicalAccessible()
                            || s.isSpecialAssistance())) {
                continue;
            }
            out.add(s);
        }
        return out;
    }

    public List<Shelter> getNearCapacity() throws DataAccessException {
        return shelterDAO.findNearCapacity();
    }

    public Shelter requireExisting(long shelterId)
            throws InvalidShelterDataException, DataAccessException {
        Shelter shelter = shelterDAO.findById(shelterId);
        if (shelter == null) {
            throw new InvalidShelterDataException(
                    "No shelter with id " + shelterId);
        }
        return shelter;
    }

    /** ADMIN-only hard delete; removed by related child rows via FK. */
    public void deleteShelter(long shelterId)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN);
        if (!shelterDAO.deleteById(shelterId)) {
            throw new InvalidShelterDataException(
                    "No shelter with id " + shelterId);
        }
    }

    // ── capacity / status helpers ────────────────────────────────────

    /** Re-derives Active/Available/Near Capacity/Full from occupancy. */
    private ShelterOperationalStatus deriveStatus(Shelter s) {
        if (s.getMaxCapacity() <= 0) {
            return s.getOperationalStatus();
        }
        double ratio = (double) s.getCurrentOccupancy() / s.getMaxCapacity();
        if (ratio >= 1.0) {
            return ShelterOperationalStatus.FULL;
        }
        if (ratio >= 0.9) {
            return ShelterOperationalStatus.NEAR_CAPACITY;
        }
        return ShelterOperationalStatus.AVAILABLE;
    }

    /** After occupancy changes, refresh the stored status (unless a
     *  manager intentionally set INACTIVE / CLOSED). */
    private void autoStatusAfterChange(long shelterId)
            throws InvalidShelterDataException, DataAccessException {
        Shelter s = requireExisting(shelterId);
        ShelterOperationalStatus current = s.getOperationalStatus();
        if (current == ShelterOperationalStatus.INACTIVE
                || current == ShelterOperationalStatus.CLOSED) {
            return;
        }
        s.setOperationalStatus(deriveStatus(s));
        shelterDAO.save(s);
    }

    private void validateBasic(Shelter s) throws InvalidShelterDataException {
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(s.getName())) {
            errors.add("shelter name is required");
        }
        if (!ValidationUtil.requireNonBlank(s.getCode())) {
            errors.add("shelter code is required");
        }
        if (!ValidationUtil.requireNonBlank(s.getDistrict())) {
            errors.add("location (district) is required");
        }
        if (!ValidationUtil.isPositive(s.getMaxCapacity())) {
            errors.add("maximum capacity must be positive");
        }
        if (!ValidationUtil.isNonNegative(s.getCurrentOccupancy())) {
            errors.add("current occupancy cannot be negative");
        }
        if (s.getCurrentOccupancy() > s.getMaxCapacity()) {
            errors.add("current occupancy (" + s.getCurrentOccupancy()
                    + ") exceeds maximum capacity (" + s.getMaxCapacity() + ")");
        }
        if (s.getContactNumber() != null
                && !s.getContactNumber().isEmpty()
                && !ValidationUtil.isValidPhone(s.getContactNumber())) {
            errors.add("contact number must be a 10-digit mobile number");
        }
        if (!errors.isEmpty()) {
            throw new InvalidShelterDataException(String.join("; ", errors));
        }
    }
}
