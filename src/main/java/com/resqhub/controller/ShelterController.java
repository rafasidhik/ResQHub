package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterFacility;
import com.resqhub.model.ShelterOperationalStatus;
import com.resqhub.service.ShelterService;
import com.resqhub.util.InputParser;

/** Shelter screen controller: UI input -> typed service calls. */
public class ShelterController {

    private final ShelterService shelterService = new ShelterService();

    public ActionResult createShelter(String name, String code, String district,
                                      String city, String address,
                                      String locationDescription,
                                      String maxCapacityText,
                                      String occupancyText,
                                      String contact, String manager,
                                      String disasterIdText,
                                      boolean wheelchair, boolean elderly,
                                      boolean medical, boolean assistance,
                                      ShelterOperationalStatus status) {
        try {
            int max = InputParser.parseInt(maxCapacityText, "Maximum capacity");
            int occ = InputParser.parseInt(occupancyText, "Current occupancy");
            Long disasterId = parseOptionalId(disasterIdText);
            Shelter s = shelterService.createShelter(name, code, district, city,
                    address, locationDescription, max, occ, contact, manager,
                    disasterId, wheelchair, elderly, medical, assistance, status);
            return ActionResult.successWithData(
                    "Shelter registered as " + s.getCode() + " ("
                            + s.getName() + ") with " + s.availableCapacity()
                            + " spaces available",
                    s);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateShelter(long id, String name, String code,
                                      String district, String city,
                                      String address, String locationDescription,
                                      String maxCapacityText,
                                      String occupancyText, String contact,
                                      String manager, String disasterIdText,
                                      boolean wheelchair, boolean elderly,
                                      boolean medical, boolean assistance,
                                      ShelterOperationalStatus status) {
        try {
            Shelter existing = shelterService.requireExisting(id);
            existing.setName(name);
            existing.setCode(code);
            existing.setDistrict(district);
            existing.setCity(city);
            existing.setAddress(address);
            existing.setLocationDescription(locationDescription);
            existing.setMaxCapacity(InputParser.parseInt(maxCapacityText,
                    "Maximum capacity"));
            existing.setCurrentOccupancy(InputParser.parseInt(occupancyText,
                    "Current occupancy"));
            existing.setContactNumber(contact);
            existing.setManagerName(manager);
            existing.setDisasterId(parseOptionalId(disasterIdText));
            existing.setWheelchairAccessible(wheelchair);
            existing.setElderlyFriendly(elderly);
            existing.setMedicalAccessible(medical);
            existing.setSpecialAssistance(assistance);
            existing.setOperationalStatus(status);

            Shelter saved = shelterService.updateShelter(existing);
            return ActionResult.success("Shelter " + saved.getCode()
                    + " updated (" + saved.availableCapacity()
                    + " spaces available)");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateOccupancy(long shelterId, String occupancyText) {
        try {
            int occ = InputParser.parseInt(occupancyText, "Current occupancy");
            Shelter s = shelterService.updateOccupancy(shelterId, occ);
            return ActionResult.success("Occupancy updated to " + occ
                    + " (" + s.availableCapacity() + " spaces available)");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateStatus(long shelterId,
                                     ShelterOperationalStatus status) {
        try {
            Shelter s = shelterService.updateStatus(shelterId, status);
            return ActionResult.success("Shelter " + s.getCode()
                    + " is now " + status.getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult addFacility(long shelterId, String name,
                                    boolean available) {
        try {
            ShelterFacility f = shelterService.addFacility(shelterId, name,
                    available);
            return ActionResult.success("Facility '" + f.getFacilityName()
                    + "' " + (available ? "added" : "added (marked unavailable)"));
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult removeFacility(long shelterId, String name) {
        try {
            shelterService.removeFacility(shelterId, name);
            return ActionResult.success("Facility removed");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult allocate(long shelterId, String victimIdText,
                                 String familyName, String peopleText,
                                 String notes) {
        try {
            Long victimId = parseOptionalId(victimIdText);
            int people = peopleText == null || peopleText.trim().isEmpty()
                    ? 1 : InputParser.parseInt(peopleText, "People count");
            ShelterAllocation a = shelterService.allocateVictim(shelterId,
                    victimId, familyName, people, notes);
            return ActionResult.successWithData("Allocated "
                    + a.getFamilyName() + " (" + people
                    + " people) to shelter #" + shelterId, a);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult release(long allocationId) {
        try {
            shelterService.releaseAllocation(allocationId);
            return ActionResult.success("Allocation " + allocationId
                    + " released");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ── reads ────────────────────────────────────────────────────────

    public List<Shelter> getAllShelters() throws DataAccessException {
        return shelterService.getAllShelters();
    }

    public List<Shelter> search(String keyword) throws DataAccessException {
        return shelterService.search(keyword);
    }

    public List<Shelter> filter(String keyword, String statusLabel,
                                ShelterOperationalStatus exactStatus,
                                Integer minAvailable, Boolean accessibleOnly)
            throws DataAccessException {
        return shelterService.filter(keyword, statusLabel, exactStatus,
                minAvailable, accessibleOnly);
    }

    public List<ShelterFacility> getFacilities(long shelterId)
            throws DataAccessException {
        return shelterService.getFacilities(shelterId);
    }

    public List<ShelterAllocation> getAllocations(long shelterId)
            throws DataAccessException {
        return shelterService.getAllocations(shelterId);
    }

    private Long parseOptionalId(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── row / header helpers ─────────────────────────────────────────

    public static Object[] toRow(Shelter s) {
        return new Object[]{
                s.getId(),
                s.getCode(),
                s.getName(),
                s.getDistrict(),
                s.getMaxCapacity(),
                s.getCurrentOccupancy(),
                s.availableCapacity(),
                s.getOperationalStatus().getLabel(),
                accessibilitySummary(s)
        };
    }

    public static String[] tableHeaders() {
        return new String[]{"ID", "Code", "Name", "District", "Capacity",
                "Occupancy", "Available", "Status", "Accessibility"};
    }

    private static String accessibilitySummary(Shelter s) {
        StringBuilder sb = new StringBuilder();
        if (s.isWheelchairAccessible()) {
            sb.append("Wheelchair, ");
        }
        if (s.isElderlyFriendly()) {
            sb.append("Elderly, ");
        }
        if (s.isMedicalAccessible()) {
            sb.append("Medical, ");
        }
        if (s.isSpecialAssistance()) {
            sb.append("Special, ");
        }
        return sb.length() == 0 ? "-" : sb.substring(0, sb.length() - 2);
    }

    public static Object[] facilityRow(ShelterFacility f) {
        return new Object[]{f.getId(), f.getFacilityName(),
                f.isAvailable() ? "Available" : "Unavailable"};
    }

    public static String[] facilityHeaders() {
        return new String[]{"ID", "Facility", "Status"};
    }

    public static Object[] allocationRow(ShelterAllocation a) {
        String who = a.getFamilyName() != null
                ? a.getFamilyName() : "Victim #" + a.getVictimId();
        String when = a.getAllocatedAt() == null ? "-"
                : a.getAllocatedAt().toLocalDate().toString();
        return new Object[]{a.getId(), who, a.getPeopleCount(),
                a.getStatus().getLabel(), when};
    }

    public static String[] allocationHeaders() {
        return new String[]{"ID", "Who", "People", "Status", "Allocated On"};
    }
}
