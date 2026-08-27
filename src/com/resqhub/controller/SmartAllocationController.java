package com.resqhub.controller;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.resqhub.dao.ShelterFacilityDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RankedShelter;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterAllocationStatus;
import com.resqhub.model.ShelterFacility;
import com.resqhub.model.SmartAllocationRequest;
import com.resqhub.model.SmartAllocationResult;
import com.resqhub.model.ShelterStatus;
import com.resqhub.model.Victim;
import com.resqhub.service.ShelterService;
import com.resqhub.service.SmartAllocationService;
import com.resqhub.service.VictimService;
import com.resqhub.util.InputParser;

/**
 * Smart Shelter Allocation screen controller. Turns Swing form input
 * into a {@link SmartAllocationRequest}, runs the ranking engine, and
 * exposes the allocation lifecycle (confirm / check-in / complete /
 * cancel / release), plus the management / statistics reads.
 */
public class SmartAllocationController {

    private final SmartAllocationService smartService =
            new SmartAllocationService();
    private final ShelterService shelterService = new ShelterService();
    private final VictimService victimService = new VictimService();
    private final ShelterFacilityDAO facilityDAO = new ShelterFacilityDAO();

    // ── smart allocation run ─────────────────────────────────────────

    public ActionResult allocate(String victimIdText, String familyName,
                                 String peopleText, PriorityLevel priority,
                                 String location, List<String> requiredFacilities,
                                 boolean needWheelchair, boolean needElderly,
                                 boolean needMedical, boolean needSpecial,
                                 String notes, boolean createPending) {
        try {
            SmartAllocationRequest req = buildRequest(victimIdText, familyName,
                    peopleText, priority, location, requiredFacilities,
                    needWheelchair, needElderly, needMedical, needSpecial,
                    notes, createPending);
            SmartAllocationResult result = smartService.allocate(req);
            ShelterAllocation a = result.getAllocation();
            String mode = createPending ? "reserved (PENDING)"
                    : "allocated";
            String msg = "Best match " + result.getBest().getShelter().getName()
                    + " (" + result.getBest().getShelter().getCode()
                    + ") - " + result.getBest().getShelter().getDistrict()
                    + ". Family of " + req.getPeopleCount() + " "
                    + mode + " (#" + a.getId() + ").";
            return ActionResult.successWithData(msg, result);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /** Non-mutating preview of the ranking, returned as the payload. */
    public ActionResult preview(String victimIdText, String familyName,
                                String peopleText, PriorityLevel priority,
                                String location, List<String> requiredFacilities,
                                boolean needWheelchair, boolean needElderly,
                                boolean needMedical, boolean needSpecial,
                                String notes) {
        try {
            SmartAllocationRequest req = buildRequest(victimIdText, familyName,
                    peopleText, priority, location, requiredFacilities,
                    needWheelchair, needElderly, needMedical, needSpecial,
                    notes, false);
            SmartAllocationResult result = smartService.evaluate(req);
            String msg = result.getRanked().size() + " suitable shelter(s) "
                    + "found. Best match: "
                    + result.getBest().getShelter().getName();
            return ActionResult.successWithData(msg, result);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    private SmartAllocationRequest buildRequest(String victimIdText,
            String familyName, String peopleText, PriorityLevel priority,
            String location, List<String> requiredFacilities,
            boolean needWheelchair, boolean needElderly, boolean needMedical,
            boolean needSpecial, String notes, boolean createPending) {
        SmartAllocationRequest req = new SmartAllocationRequest();
        Long victimId = parseOptionalId(victimIdText);
        req.setVictimId(victimId);
        req.setFamilyName(familyName == null ? null : familyName.trim());
        int people = peopleText == null || peopleText.trim().isEmpty()
                ? 1 : InputParser.parseInt(peopleText, "People count");
        req.setPeopleCount(people);
        req.setPriority(priority);
        req.setLocation(location == null || location.trim().isEmpty()
                ? null : location.trim());
        if (requiredFacilities != null) {
            for (String f : requiredFacilities) {
                if (f != null && !f.trim().isEmpty()) {
                    req.getRequiredFacilities().add(f.trim());
                }
            }
        }
        req.setNeedWheelchair(needWheelchair);
        req.setNeedElderly(needElderly);
        req.setNeedMedical(needMedical);
        req.setNeedSpecial(needSpecial);
        req.setNotes(notes);
        req.setCreatePending(createPending);
        return req;
    }

    // ── allocation lifecycle ─────────────────────────────────────────

    public ActionResult confirmPending(long allocationId) {
        return wrap("confirmed", () -> shelterService
                .confirmPending(allocationId));
    }

    public ActionResult checkIn(long allocationId) {
        return wrap("checked in", () -> shelterService
                .checkIn(allocationId));
    }

    public ActionResult complete(long allocationId) {
        return wrap("completed", () -> shelterService.complete(allocationId));
    }

    public ActionResult cancel(long allocationId) {
        return wrap("cancelled", () -> shelterService.cancel(allocationId));
    }

    public ActionResult release(long allocationId) {
        return wrap("released", () -> shelterService
                .releaseAllocation(allocationId));
    }

    private ActionResult wrap(String verb, Action action) {
        try {
            ShelterAllocation a = action.run();
            return ActionResult.success("Allocation #" + a.getId() + " ("
                    + (a.getFamilyName() == null ? "Victim #" + a.getVictimId()
                            : a.getFamilyName())
                    + ") is now " + a.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    private interface Action {
        ShelterAllocation run() throws Exception;
    }

    // ── reads / references ───────────────────────────────────────────

    public List<Victim> getVictims() throws DataAccessException {
        return victimService.getAllVictims();
    }

    public List<Shelter> getAcceptingShelters() throws DataAccessException {
        return shelterService.getAcceptingShelters();
    }

    public List<Shelter> getAllShelters() throws DataAccessException {
        return shelterService.getAllShelters();
    }

    /** Distinct facility names across all shelters, for the requirement
     *  checkboxes (medical support, drinking water, toilets, ...). */
    public List<String> getFacilityOptions() throws DataAccessException {
        Set<String> names = new LinkedHashSet<>();
        for (Shelter s : shelterService.getAllShelters()) {
            for (ShelterFacility f : facilityDAO.findByShelter(s.getId())) {
                if (f.getFacilityName() != null) {
                    names.add(f.getFacilityName());
                }
            }
        }
        return new ArrayList<>(names);
    }

    public List<ShelterAllocation> getAllAllocations()
            throws DataAccessException {
        return shelterService.getAllAllocations();
    }

    public List<ShelterAllocation> getByStatus(ShelterAllocationStatus status)
            throws DataAccessException {
        return shelterService.getByStatus(status);
    }

    /** Client-side filter for the management view. */
    public List<ShelterAllocation> filterAllocations(
            ShelterAllocationStatus status, Long shelterId, String keyword)
            throws DataAccessException {
        List<ShelterAllocation> out = new ArrayList<>();
        for (ShelterAllocation a : shelterService.getAllAllocations()) {
            if (status != null && a.getStatus() != status) {
                continue;
            }
            if (shelterId != null && !shelterId.equals(a.getShelterId())) {
                continue;
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String who = a.getFamilyName() == null
                        ? "Victim #" + a.getVictimId() : a.getFamilyName();
                if (!who.toLowerCase().contains(keyword.trim().toLowerCase())
                        && !String.valueOf(a.getId()).contains(keyword.trim())) {
                    continue;
                }
            }
            out.add(a);
        }
        return out;
    }

    /** Victims not currently sheltered - the accommodation wait list. */
    public List<Victim> getWaitingForShelter() throws DataAccessException {
        List<Victim> waiting = new ArrayList<>();
        for (Victim v : victimService.getAllVictims()) {
            if (v.getShelterStatus() == ShelterStatus.IN_SHELTER) {
                continue;
            }
            waiting.add(v);
        }
        return waiting;
    }

    public int countByStatus(ShelterAllocationStatus status)
            throws DataAccessException {
        return shelterService.getByStatus(status).size();
    }

    public int countWaiting() throws DataAccessException {
        return getWaitingForShelter().size();
    }

    public int countActive() throws DataAccessException {
        int total = 0;
        for (ShelterAllocation a : shelterService.getAllAllocations()) {
            if (a.getStatus().isOccupying()) {
                total++;
            }
        }
        return total;
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

    public static Object[] rankedRow(RankedShelter r) {
        Shelter s = r.getShelter();
        return new Object[]{s.getCode(), s.getName(), s.getDistrict(),
                s.availableCapacity(), r.getAvailableAfter(),
                s.getOperationalStatus().getLabel(),
                String.format("%.2f", r.getScore())};
    }

    public static String[] rankedHeaders() {
        return new String[]{"Code", "Shelter", "District", "Avail Now",
                "Avail After", "Status", "Score"};
    }

    public static Object[] allocationRow(ShelterAllocation a) {
        String who = a.getFamilyName() != null
                ? a.getFamilyName() : "Victim #" + a.getVictimId();
        String when = a.getAllocatedAt() == null ? "-"
                : a.getAllocatedAt().toLocalDate().toString();
        return new Object[]{a.getId(), a.getShelterId(), who, a.getPeopleCount(),
                a.getStatus().getLabel(), when};
    }

    public static String[] allocationHeaders() {
        return new String[]{"ID", "Shelter #", "Who", "People", "Status",
                "Allocated On"};
    }

    public static Object[] waitListRow(Victim v) {
        return new Object[]{v.getId(), v.getFullName(), v.getAge(),
                v.getEmergencyStatus().getLabel(),
                v.getCurrentLocation() == null ? "-" : v.getCurrentLocation()};
    }

    public static String[] waitListHeaders() {
        return new String[]{"ID", "Name", "Age", "Status", "Location"};
    }
}
