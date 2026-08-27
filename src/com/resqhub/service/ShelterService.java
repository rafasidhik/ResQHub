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
     * Allocates a victim / family to a shelter. Fails if the family size
     * exceeds the shelter's available capacity (overcapacity prevention).
     * Occupancy is recomputed, the victim flag is raised, and the shelter
     * status is re-derived once the group is added.
     */
    public ShelterAllocation allocateVictim(long shelterId, Long victimId,
                                            String familyName, int peopleCount,
                                            String notes)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        Shelter shelter = requireExisting(shelterId);
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

        ShelterAllocation a = new ShelterAllocation();
        a.setShelterId(shelterId);
        a.setVictimId(victimId);
        a.setFamilyName(familyName);
        a.setPeopleCount(peopleCount);
        a.setNotes(notes);
        a.setStatus(ShelterAllocationStatus.ACTIVE);
        a.setAllocatedBy(session.currentUserId());
        ShelterAllocation saved = allocationDAO.save(a);

        if (victimId != null) {
            try {
                new VictimService().markShelterStatus(victimId,
                        ShelterStatus.IN_SHELTER);
            } catch (Exception ignored) {
                // victim flag update is best-effort; allocation still stands
            }
        }
        Shelter updated = requireExisting(shelterId);
        updated.setCurrentOccupancy(updated.getCurrentOccupancy() + peopleCount);
        updated.setOperationalStatus(deriveStatus(updated));
        shelterDAO.save(updated);
        return saved;
    }

    /** Releases a family/victim from a shelter, decreasing occupancy and
     *  lowering the victim shelter flag. */
    public void releaseAllocation(long allocationId)
            throws UnauthorizedOperationException, InvalidShelterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
        ShelterAllocation a = allocationDAO.findById(allocationId);
        if (a == null) {
            throw new InvalidShelterDataException(
                    "No allocation with id " + allocationId);
        }
        if (a.getStatus() != ShelterAllocationStatus.ACTIVE) {
            throw new InvalidShelterDataException(
                    "Allocation " + allocationId + " is already released");
        }
        allocationDAO.release(allocationId);
        if (a.getVictimId() != null) {
            try {
                new VictimService().markShelterStatus(a.getVictimId(),
                        ShelterStatus.NOT_SHELTERED);
            } catch (Exception ignored) {
                // best-effort
            }
        }
        Shelter updated = requireExisting(a.getShelterId());
        int occupancy = Math.max(0,
                updated.getCurrentOccupancy() - a.getPeopleCount());
        updated.setCurrentOccupancy(occupancy);
        updated.setOperationalStatus(deriveStatus(updated));
        shelterDAO.save(updated);
    }

    public List<ShelterAllocation> getAllocations(long shelterId)
            throws DataAccessException {
        return allocationDAO.findByShelter(shelterId);
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
