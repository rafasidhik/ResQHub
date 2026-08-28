package com.resqhub.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.resqhub.dao.DisasterDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.Disaster;
import com.resqhub.model.Hospital;
import com.resqhub.model.HospitalCapacityLog;
import com.resqhub.model.HospitalFacility;
import com.resqhub.model.HospitalReferral;
import com.resqhub.model.HospitalReferralStatus;
import com.resqhub.model.HospitalStatus;
import com.resqhub.model.Victim;
import com.resqhub.service.HospitalService;
import com.resqhub.util.InputParser;

/** Hospital Management screen controller: UI input -> typed service calls. */
public class HospitalController {

    private final HospitalService hospitalService = new HospitalService();
    private final DisasterDAO disasterDAO = new DisasterDAO();

    // ---- registration / profile ---------------------------------------

    public ActionResult registerHospital(String name, String hospitalId,
            String district, String city, String area, String address,
            String phone, String emergencyContact, String email,
            String totalBedsText, String occupiedBedsText,
            Set<HospitalFacility> facilities, HospitalStatus status,
            String disasterIdText) {
        try {
            int totalBeds = InputParser.parseInt(totalBedsText,
                    "Total bed capacity");
            int occupiedBeds = InputParser.parseInt(occupiedBedsText,
                    "Occupied beds");
            Hospital h = hospitalService.createHospital(name, hospitalId,
                    district, city, area, address, phone, emergencyContact,
                    email, totalBeds, occupiedBeds, facilities, status,
                    parseOptionalId(disasterIdText));
            return ActionResult.successWithData("Hospital " + h.getName()
                    + " registered (" + h.availableBeds() + " beds available).",
                    h);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateHospital(String idText, String name,
            String hospitalId, String district, String city, String area,
            String address, String phone, String emergencyContact,
            String email, String totalBedsText, String occupiedBedsText,
            Set<HospitalFacility> facilities) {
        try {
            long id = InputParser.parseLong(idText, "Hospital");
            Hospital h = hospitalService.getHospital(id);
            if (h == null) {
                return ActionResult.failure("No hospital with id " + id);
            }
            h.setName(name);
            h.setHospitalId(hospitalId);
            h.setDistrict(district);
            h.setCity(city);
            h.setArea(area);
            h.setAddress(address);
            h.setPhone(phone);
            h.setEmergencyContact(emergencyContact);
            h.setEmail(email);
            h.setTotalBeds(InputParser.parseInt(totalBedsText,
                    "Total bed capacity"));
            h.setOccupiedBeds(InputParser.parseInt(occupiedBedsText,
                    "Occupied beds"));
            h.setFacilities(facilities);
            Hospital saved = hospitalService.updateHospital(h);
            return ActionResult.success("Hospital " + saved.getName()
                    + " updated (" + saved.availableBeds()
                    + " beds available).");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult setStatus(String idText, HospitalStatus status) {
        try {
            long id = InputParser.parseLong(idText, "Hospital");
            Hospital h = hospitalService.updateStatus(id, status);
            return ActionResult.success("Hospital " + h.getName() + " -> "
                    + h.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateOccupiedBeds(String idText,
            String newOccupiedText, String reason) {
        try {
            long id = InputParser.parseLong(idText, "Hospital");
            int newOccupied = InputParser.parseInt(newOccupiedText,
                    "New occupied beds");
            Hospital h = hospitalService.updateOccupiedBeds(id, newOccupied,
                    reason);
            return ActionResult.success("Hospital " + h.getName()
                    + " occupancy updated to " + h.getOccupiedBeds() + "/"
                    + h.getTotalBeds() + " (" + h.availableBeds()
                    + " available).");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult addFacility(String idText, HospitalFacility facility) {
        try {
            long id = InputParser.parseLong(idText, "Hospital");
            Hospital h = hospitalService.addFacility(id, facility);
            return ActionResult.success("Added " + facility.getLabel()
                    + " to " + h.getName());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult removeFacility(String idText, HospitalFacility facility) {
        try {
            long id = InputParser.parseLong(idText, "Hospital");
            Hospital h = hospitalService.removeFacility(id, facility);
            return ActionResult.success("Removed " + facility.getLabel()
                    + " from " + h.getName());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteHospital(String idText) {
        try {
            long id = InputParser.parseLong(idText, "Hospital");
            hospitalService.deleteHospital(id);
            return ActionResult.success("Hospital #" + id + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- referrals ----------------------------------------------------

    public ActionResult referVictim(String hospitalIdText, String victimIdText,
            String bedsText, Set<HospitalFacility> required, String reason,
            String notes, String disasterIdText) {
        try {
            long hospitalId = InputParser.parseLong(hospitalIdText,
                    "Hospital");
            Long victimId = parseOptionalId(victimIdText);
            int beds = InputParser.parseInt(bedsText, "Beds required");
            String victimName = null;
            if (victimId != null) {
                Victim v = hospitalService.getVictim(victimId);
                if (v != null) {
                    victimName = v.getFullName();
                }
            }
            HospitalReferral r = hospitalService.referVictim(hospitalId,
                    victimId, victimName, beds, required, reason, notes,
                    parseOptionalId(disasterIdText));
            return ActionResult.successWithData("Referral created for "
                    + (r.getVictimName() == null ? "victim #" + victimId
                            : r.getVictimName()) + " (" + beds
                    + " beds).", r);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult changeReferralStatus(String referralIdText,
            HospitalReferralStatus status) {
        try {
            long id = InputParser.parseLong(referralIdText, "Referral");
            HospitalReferral r = hospitalService.setReferralStatus(id, status);
            return ActionResult.success("Referral #" + id + " -> "
                    + r.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult generateCapacityAlerts() {
        try {
            int created = hospitalService.generateCapacityAlerts();
            return ActionResult.success(created == 0
                    ? "No new capacity alerts (none near capacity, or alerts "
                            + "already raised)"
                    : "Generated " + created + " hospital capacity alert(s)");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- reads --------------------------------------------------------

    public List<Hospital> getAllHospitals() throws DataAccessException {
        return hospitalService.getAllHospitals();
    }

    public List<Hospital> search(String keyword) throws DataAccessException {
        return hospitalService.search(keyword);
    }

    public List<Hospital> filter(String keyword, String district,
            HospitalStatus status, String minAvailableText,
            HospitalFacility facility) throws DataAccessException {
        Integer minAvailable = parseIntOrNull(minAvailableText);
        return hospitalService.filter(keyword, district, status, minAvailable,
                facility);
    }

    public List<Hospital> findAccepting() throws DataAccessException {
        return hospitalService.findAccepting();
    }

    public List<Hospital> findNearCapacity() throws DataAccessException {
        return hospitalService.findNearCapacity();
    }

    public List<Hospital> findFull() throws DataAccessException {
        return hospitalService.findFull();
    }

    public Hospital getHospital(long id) throws DataAccessException {
        return hospitalService.getHospital(id);
    }

    public List<HospitalReferral> getReferrals(long hospitalId)
            throws DataAccessException {
        return hospitalService.getReferrals(hospitalId);
    }

    public List<HospitalReferral> getAllReferrals()
            throws DataAccessException {
        return hospitalService.getAllReferrals();
    }

    public List<HospitalReferral> getOpenReferrals()
            throws DataAccessException {
        return hospitalService.getOpenReferrals();
    }

    public List<HospitalReferral> getReferralsByStatus(
            HospitalReferralStatus status) throws DataAccessException {
        return hospitalService.getReferralsByStatus(status);
    }

    public List<HospitalCapacityLog> getCapacityLogs(long hospitalId)
            throws DataAccessException {
        return hospitalService.getCapacityLogs(hospitalId);
    }

    public List<HospitalCapacityLog> getAllCapacityLogs()
            throws DataAccessException {
        return hospitalService.getAllCapacityLogs();
    }

    public List<Victim> getAllVictims() throws DataAccessException {
        return hospitalService.getAllVictims();
    }

    public List<Disaster> getDisasters() throws DataAccessException {
        return disasterDAO.findAll();
    }

    // ---- statistics ----------------------------------------------------

    public int countHospitals() throws DataAccessException {
        return hospitalService.countHospitals();
    }

    public int countActive() throws DataAccessException {
        return hospitalService.countActive();
    }

    public int countAccepting() throws DataAccessException {
        return hospitalService.countAccepting();
    }

    public int countNearCapacity() throws DataAccessException {
        return hospitalService.countNearCapacity();
    }

    public int countFull() throws DataAccessException {
        return hospitalService.countFull();
    }

    public int totalBeds() throws DataAccessException {
        return hospitalService.totalBeds();
    }

    public int totalOccupiedBeds() throws DataAccessException {
        return hospitalService.totalOccupiedBeds();
    }

    public int totalAvailableBeds() throws DataAccessException {
        return hospitalService.totalAvailableBeds();
    }

    // ---- row / header helpers ------------------------------------------

    public static Object[] hospitalRow(Hospital h,
            Map<Long, String> disasterNames) {
        return new Object[]{
                h.getId(),
                h.getName(),
                h.getHospitalId(),
                h.getDistrict(),
                h.getCity(),
                h.getAddress() == null ? "-" : h.getAddress(),
                h.getPhone() == null ? "-" : h.getPhone(),
                h.getEmergencyContact() == null ? "-"
                        : h.getEmergencyContact(),
                h.getTotalBeds(),
                h.getOccupiedBeds(),
                h.availableBeds(),
                h.utilisationPercent() + "%",
                h.facilitiesSummary(),
                h.getStatus() == null ? "-" : h.getStatus().getLabel(),
                h.getDisasterId() == null ? "-"
                        : disasterNames.getOrDefault(h.getDisasterId(), "?")
        };
    }

    public static String[] hospitalHeaders() {
        return new String[]{"ID", "Name", "Code", "District", "City",
                "Address", "Phone", "Emergency", "Total", "Occupied",
                "Available", "Utilisation", "Facilities", "Status",
                "Disaster"};
    }

    public static Object[] referralRow(HospitalReferral r,
            Map<Long, String> hospitalNames) {
        return new Object[]{
                r.getId(),
                r.getVictimName() == null
                        ? "victim #" + (r.getVictimId() == null ? "?"
                                : r.getVictimId()) : r.getVictimName(),
                r.getHospitalId() == null ? "-"
                        : hospitalNames.getOrDefault(r.getHospitalId(), "?"),
                r.getReason() == null ? "-" : r.getReason(),
                r.getBedsRequired(),
                r.requiredFacilitiesSummary(),
                r.getStatus() == null ? "-" : r.getStatus().getLabel(),
                r.getReferredAt() == null ? "-"
                        : r.getReferredAt().toString().replace('T', ' '),
                r.getNotes() == null ? "" : r.getNotes()
        };
    }

    public static String[] referralHeaders() {
        return new String[]{"ID", "Victim", "Hospital", "Reason", "Beds",
                "Facilities", "Status", "Referred", "Notes"};
    }

    public static Object[] capacityLogRow(HospitalCapacityLog log,
            Map<Long, String> hospitalNames) {
        return new Object[]{
                log.getId(),
                log.getHospitalId() == null ? "-"
                        : hospitalNames.getOrDefault(log.getHospitalId(),
                                "?"),
                log.getPreviousOccupied(),
                log.getUpdatedOccupied(),
                log.getAvailableBeds(),
                log.getReason() == null ? "-" : log.getReason(),
                log.getChangedAt() == null ? "-"
                        : log.getChangedAt().toString().replace('T', ' ')
        };
    }

    public static String[] capacityLogHeaders() {
        return new String[]{"ID", "Hospital", "Before", "After",
                "Available", "Reason", "Changed"};
    }

    public static Object[] victimRow(Victim v) {
        return new Object[]{
                v.getId(),
                v.getFullName(),
                v.getEmergencyStatus() == null ? "-"
                        : v.getEmergencyStatus().getLabel(),
                v.getMedicalCondition() == null ? "-"
                        : v.getMedicalCondition(),
                v.getCurrentLocation() == null ? "-"
                        : v.getCurrentLocation(),
                v.getDisasterId() == null ? "-" : v.getDisasterId()
        };
    }

    public static String[] victimHeaders() {
        return new String[]{"ID", "Name", "Status", "Medical", "Location",
                "Disaster"};
    }

    public Map<Long, String> hospitalNameMap() throws DataAccessException {
        Map<Long, String> map = new HashMap<>();
        for (Hospital h : getAllHospitals()) {
            map.put(h.getId(), h.getName());
        }
        return map;
    }

    public Map<Long, String> disasterNameMap() throws DataAccessException {
        Map<Long, String> map = new HashMap<>();
        for (Disaster d : getDisasters()) {
            map.put(d.getId(), d.getTitle());
        }
        return map;
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

    private Integer parseIntOrNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
