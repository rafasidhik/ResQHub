package com.resqhub.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.resqhub.dao.HospitalDAO;
import com.resqhub.dao.VictimDAO;
import com.resqhub.exception.BloodUnavailableException;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
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
import com.resqhub.model.RequestHistory;
import com.resqhub.service.BloodDonorService;
import com.resqhub.service.BloodDonorService.BloodGroupSummary;
import com.resqhub.service.BloodDonorService.MatchingHistoryEntry;
import com.resqhub.service.BloodDonorService.RequestStats;
import com.resqhub.util.InputParser;
import com.resqhub.util.LocationProximityUtil;

/** Blood Donor Management screen controller: UI input -> typed service calls. */
public class BloodDonorController {

    private final BloodDonorService service = new BloodDonorService();
    private final HospitalDAO hospitalDAO = new HospitalDAO();
    private final VictimDAO victimDAO = new VictimDAO();

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ---- donor registration / profile --------------------------------

    public ActionResult registerDonor(String nameText, BloodGroup group,
            String location, String phone, String email,
            DonorAvailability availability, String lastDonationText,
            DonorEligibility eligibility, String notes) {
        try {
            LocalDate lastDonation = parseDateOrNull(lastDonationText);
            BloodDonor d = service.registerDonor(nameText, group, location,
                    phone, email, availability, lastDonation, eligibility,
                    notes);
            return ActionResult.successWithData(
                    "Blood donor registered as #" + d.getId() + " ("
                            + d.getBloodGroup().getLabel() + ")",
                    d);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateDonor(String idText, String nameText,
            BloodGroup group, String location, String phone, String email,
            DonorAvailability availability, String lastDonationText,
            DonorEligibility eligibility, String notes) {
        try {
            long id = InputParser.parseLong(idText, "Blood donor");
            BloodDonor d = service.getDonor(id);
            if (d == null) {
                return ActionResult.failure("No blood donor with id " + id);
            }
            d.setFullName(nameText);
            d.setBloodGroup(group);
            d.setLocation(location);
            d.setPhone(phone);
            d.setEmail(email);
            d.setAvailability(availability);
            d.setLastDonationDate(parseDateOrNull(lastDonationText));
            d.setEligibility(eligibility);
            d.setNotes(notes);
            BloodDonor saved = service.updateDonor(d);
            return ActionResult.success("Blood donor #" + saved.getId()
                    + " updated");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateDonorState(String idText,
            DonorAvailability availability, DonorEligibility eligibility) {
        try {
            long id = InputParser.parseLong(idText, "Blood donor");
            BloodDonor d = service.updateDonorState(id, availability,
                    eligibility);
            return ActionResult.success("Donor " + d.getFullName()
                    + " -> " + d.getAvailability().getLabel() + ", "
                    + d.getEligibility().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteDonor(String idText) {
        try {
            long id = InputParser.parseLong(idText, "Blood donor");
            service.deleteDonor(id);
            return ActionResult.success("Blood donor #" + id + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- blood requests -------------------------------------------------

    public ActionResult createBloodRequest(String codeText, BloodGroup group,
            String unitsText, String location, BloodRequestPriority priority,
            String emergencyText, String hospitalIdText, String victimIdText,
            String disasterIdText, String requiredDateText) {
        try {
            int units = InputParser.parseInt(unitsText, "Required units");
            java.time.LocalDateTime requiredDate =
                    InputParser.parseOptionalDateTime(requiredDateText);
            BloodRequest r = service.createBloodRequest(codeText, group, units,
                    location, priority, emergencyText,
                    optionalId(hospitalIdText), optionalId(victimIdText),
                    optionalId(disasterIdText), requiredDate);
            return ActionResult.successWithData(
                    "Blood request created: " + r.getRequestCode() + " ["
                            + r.getBloodGroup().getLabel() + " x"
                            + r.getUnitsRequired() + "]",
                    r);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateRequestStatus(String idText,
            BloodRequestStatus status) {
        try {
            long id = InputParser.parseLong(idText, "Blood request");
            BloodRequest r = service.setRequestStatus(id, status);
            return ActionResult.success("Blood request " + r.getRequestCode()
                    + " -> " + r.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteRequest(String idText) {
        try {
            long id = InputParser.parseLong(idText, "Blood request");
            service.deleteRequest(id);
            return ActionResult.success("Blood request #" + id + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- matching -------------------------------------------------------

    public List<BloodDonor> findSuitableDonors(String idText)
            throws ResQHubException {
        return service.findSuitableDonors(
                safeLong(idText));
    }

    public ActionResult matchRequest(String idText, String maxDonorsText) {
        try {
            long id = InputParser.parseLong(idText, "Blood request");
            int max = InputParser.parseInt(maxDonorsText, "Max donors");
            List<BloodMatch> matches = service.matchRequest(id, max);
            return ActionResult.successWithData(
                    "Matched " + matches.size() + " donor(s) to request #"
                            + id + ". Review the matches tab.", matches);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult setMatchStatus(String idText, BloodMatchStatus status) {
        try {
            long id = InputParser.parseLong(idText, "Blood match");
            BloodMatch m = service.setMatchStatus(id, status);
            return ActionResult.success("Match #" + id + " -> "
                    + m.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /** Re-match a request after donor declines (spec 15). */
    public ActionResult rematchRequest(String idText, String maxDonorsText) {
        try {
            long id = InputParser.parseLong(idText, "Blood request");
            int max = InputParser.parseInt(maxDonorsText, "Max donors");
            List<BloodMatch> matches = service.rematchRequest(id, max);
            return ActionResult.successWithData(
                    "Re-matched " + matches.size() + " new donor(s) to #"
                            + id + ".", matches);
        } catch (BloodUnavailableException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /** Returns ranked candidates with scores for display (spec 9). */
    public List<DonorRanking> findRankedDonors(String idText)
            throws ResQHubException {
        return service.findRankedDonors(safeLong(idText));
    }

    /** Full matching history for a request (spec 19). */
    public List<MatchingHistoryEntry> getMatchingHistory(String idText)
            throws DataAccessException {
        long id = safeLong(idText);
        return service.getMatchingHistory(id);
    }

    // ---- donations ------------------------------------------------------

    public ActionResult recordDonation(String donorIdText, BloodGroup group,
            String unitsText, String requestIdText, String notes) {
        try {
            long donorId = InputParser.parseLong(donorIdText, "Blood donor");
            int units = InputParser.parseInt(unitsText, "Units donated");
            Long requestId = optionalId(requestIdText);
            BloodDonation d = service.recordDonation(donorId, group, units,
                    requestId, notes);
            return ActionResult.successWithData(
                    "Donation recorded: " + d.getUnitsDonated() + " unit(s) "
                            + d.getBloodGroup().getLabel(), d);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- reads ----------------------------------------------------------

    public BloodDonor getDonor(long id) throws DataAccessException {
        return service.getDonor(id);
    }

    public List<BloodDonor> getAllDonors() throws DataAccessException {
        return service.getAllDonors();
    }

    public List<BloodDonor> searchDonors(String keyword)
            throws DataAccessException {
        return service.searchDonors(keyword);
    }

    public List<BloodDonor> filterDonors(BloodGroup group,
            DonorAvailability availability, DonorEligibility eligibility)
            throws DataAccessException {
        return service.filterDonors(group, availability, eligibility);
    }

    public List<BloodDonor> findEligibleAvailable() throws DataAccessException {
        return service.findEligibleAvailable();
    }

    public BloodRequest getRequest(long id) throws DataAccessException {
        return service.getRequest(id);
    }

    public List<BloodRequest> getAllRequests() throws DataAccessException {
        return service.getAllRequests();
    }

    public List<BloodRequest> getOpenRequests() throws DataAccessException {
        return service.getOpenRequests();
    }

    public List<BloodMatch> getMatchesForRequest(long requestId)
            throws DataAccessException {
        return service.getMatchesForRequest(requestId);
    }

    public List<BloodMatch> getMatchesForDonor(long donorId)
            throws DataAccessException {
        return service.getMatchesForDonor(donorId);
    }

    public List<BloodMatch> getAllMatches() throws DataAccessException {
        java.util.List<BloodMatch> out = new ArrayList<>();
        for (BloodRequest r : getAllRequests()) {
            out.addAll(getMatchesForRequest(r.getId()));
        }
        out.sort((a, b) -> Long.compare(
                b.getId() == null ? 0 : b.getId(),
                a.getId() == null ? 0 : a.getId()));
        return out;
    }

    public List<BloodDonation> getDonationHistory(long donorId)
            throws DataAccessException {
        return service.getDonationHistory(donorId);
    }

    public List<BloodDonation> getAllDonations() throws DataAccessException {
        return service.getAllDonations();
    }

    public BloodDonation getLastDonation(long donorId)
            throws DataAccessException {
        return service.getLastDonation(donorId);
    }

    // ---- shortage / stats ----------------------------------------------

    public List<String> findShortageMessages() throws ResQHubException {
        return service.findShortageMessages();
    }

    public int countShortages() throws DataAccessException {
        return service.countShortages();
    }

    public int countDonors() throws DataAccessException {
        return service.countDonors();
    }

    public int countEligibleAvailable() throws DataAccessException {
        return service.countEligibleAvailable();
    }

    public int countRequests() throws DataAccessException {
        return service.countRequests();
    }

    public int countCriticalOpen() throws DataAccessException {
        return service.countCriticalOpen();
    }

    public int countUnitsCollected() throws DataAccessException {
        return service.countUnitsCollected();
    }

    public List<BloodGroupSummary> bloodGroupSummaries()
            throws DataAccessException {
        return service.bloodGroupSummaries();
    }

    /** Filtered request search (spec 23). */
    public List<BloodRequest> searchRequests(BloodGroup group,
            BloodRequestPriority priority, BloodRequestStatus status,
            Long hospitalId, String keyword) throws DataAccessException {
        return service.searchRequests(group, priority, status,
                hospitalId, keyword);
    }

    /** Lifecycle history for a request (spec 25). */
    public List<RequestHistory> getRequestHistory(long requestId)
            throws DataAccessException {
        return service.getRequestHistory(requestId);
    }

    public int receivedUnits(long requestId) throws DataAccessException {
        return service.receivedUnits(requestId);
    }

    public int remainingUnits(long requestId) throws DataAccessException {
        return service.remainingUnits(requestId);
    }

    public RequestStats requestStats() throws DataAccessException {
        return service.requestStats();
    }

    // ---- display helpers -------------------------------------------------

    public static Object[] donorRow(BloodDonor d) {
        return new Object[]{
                d.getId(),
                d.getFullName(),
                d.getBloodGroup() == null ? "-" : d.getBloodGroup().getLabel(),
                d.getLocation() == null ? "-" : d.getLocation(),
                d.getPhone() == null ? "-" : d.getPhone(),
                d.getEmail() == null ? "-" : d.getEmail(),
                d.getAvailability() == null ? "-"
                        : d.getAvailability().getLabel(),
                d.getLastDonationDate() == null ? "-"
                        : d.getLastDonationDate().toString(),
                d.getEligibility() == null ? "-"
                        : d.getEligibility().getLabel()
        };
    }

    public static String[] donorHeaders() {
        return new String[]{"ID", "Name", "Blood Group", "Location",
                "Phone", "Email", "Availability", "Last Donation",
                "Eligibility"};
    }

    public Object[] requestRow(BloodRequest r) {
        return new Object[]{
                r.getId(),
                r.getRequestCode(),
                r.getBloodGroup() == null ? "-" : r.getBloodGroup().getLabel(),
                r.getUnitsRequired(),
                safeReceived(r.getId()),
                safeRemaining(r.getId()),
                r.getLocation() == null ? "-" : r.getLocation(),
                r.getPriority() == null ? "-" : r.getPriority().getLabel(),
                r.getStatus() == null ? "-" : r.getStatus().getLabel(),
                r.getEmergencyDetails() == null ? "-"
                        : shortText(r.getEmergencyDetails()),
                hospitalName(r.getHospitalId()),
                victimName(r.getVictimId()),
                r.getRequestDate() == null ? "-"
                        : r.getRequestDate().toLocalDate().toString(),
                r.getRequiredDate() == null ? "-"
                        : r.getRequiredDate().toLocalDate().toString()
        };
    }

    public static String[] requestHeaders() {
        return new String[]{"ID", "Code", "Blood Group", "Units",
                "Received", "Remaining", "Location", "Priority", "Status",
                "Emergency", "Hospital", "Victim", "Requested", "Required By"};
    }

    public Object[] historyRow(RequestHistory h) {
        return new Object[]{
                h.getEvent(),
                h.getRemarks(),
                h.getPerformedAt() == null ? "-"
                        : h.getPerformedAt().toString(),
                h.getId()
        };
    }

    public static String[] historyHeaders() {
        return new String[]{"Event", "Details", "When", "History ID"};
    }

    public Object[] requestStatsRow(String label, int value) {
        return new Object[]{label, value};
    }

    public static String[] requestStatsHeaders() {
        return new String[]{"Metric", "Count"};
    }

    public Object[] rankingRow(DonorRanking rk) {
        return new Object[]{
                rk.getDonor().getId(),
                rk.getDonor().getFullName(),
                rk.getDonor().getBloodGroup() == null ? "-"
                        : rk.getDonor().getBloodGroup().getLabel(),
                rk.isExactGroupMatch() ? "EXACT" : "COMPATIBLE",
                rk.getDonor().getLocation() == null ? "-"
                        : rk.getDonor().getLocation(),
                rk.isLocationMatch() ? "Nearby" : LocationProximityUtil
                        .rankLabel(rk.getLocationScore()),
                rk.getCompatibilityScore(),
                rk.getLocationScore(),
                rk.getUrgencyBonus(),
                rk.getTotalScore()
        };
    }

    public static String[] rankingHeaders() {
        return new String[]{"ID", "Donor", "Blood Group", "Compatibility",
                "Location", "Proximity", "Compat Score", "Loc Score",
                "Urgency Bonus", "Total Score"};
    }

    public Object[] matchingHistoryRow(MatchingHistoryEntry h) {
        return new Object[]{
                h.matchId(),
                h.donorName(),
                h.bloodGroup(),
                h.status() == null ? "-" : h.status().getLabel(),
                h.unitsMatched(),
                h.locationRank(),
                h.rank(),
                h.notes() == null ? "-" : h.notes(),
                h.matchedAt() == null ? "-"
                        : h.matchedAt().toString(),
                h.confirmedAt() == null ? "-"
                        : h.confirmedAt().toString(),
                h.collectedAt() == null ? "-"
                        : h.collectedAt().toString()
        };
    }

    public static String[] matchingHistoryHeaders() {
        return new String[]{"Match ID", "Donor", "Group", "Status",
                "Units", "Location Rank", "Rank", "Notes",
                "Matched At", "Confirmed At", "Collected At"};
    }

    public Object[] matchRow(BloodMatch m) {
        return new Object[]{
                m.getId(),
                donorName(m.getDonorId()),
                requestCode(m.getRequestId()),
                m.getStatus() == null ? "-" : m.getStatus().getLabel(),
                m.getUnitsMatched(),
                m.isLocationMatched() ? "Yes" : "No",
                m.getDonorDistanceRank(),
                m.getMatchedAt() == null ? "-"
                        : m.getMatchedAt().toLocalDate().toString()
        };
    }

    public static String[] matchHeaders() {
        return new String[]{"ID", "Donor", "Request", "Status",
                "Units", "Nearby", "Rank", "Matched"};
    }

    public Object[] donationRow(BloodDonation d) {
        return new Object[]{
                d.getId(),
                donorName(d.getDonorId()),
                d.getBloodGroup() == null ? "-" : d.getBloodGroup().getLabel(),
                d.getUnitsDonated(),
                d.getDonationDate() == null ? "-" : d.getDonationDate().toString(),
                d.getDonationStatus() == null ? "-" : d.getDonationStatus(),
                requestCode(d.getRequestId())
        };
    }

    public static String[] donationHeaders() {
        return new String[]{"ID", "Donor", "Blood Group", "Units",
                "Date", "Status", "Request"};
    }

    // ---- name maps -------------------------------------------------------

    public Map<Long, String> hospitalNameMap() throws DataAccessException {
        Map<Long, String> map = new HashMap<>();
        for (com.resqhub.model.Hospital h : hospitalDAO.findAll()) {
            map.put(h.getId(), h.getName());
        }
        return map;
    }

    public Map<Long, String> victimNameMap() throws DataAccessException {
        Map<Long, String> map = new HashMap<>();
        for (com.resqhub.model.Victim v : victimDAO.findAll()) {
            map.put(v.getId(), v.getFullName());
        }
        return map;
    }

    public Map<Long, String> donorNameMap() throws DataAccessException {
        Map<Long, String> map = new HashMap<>();
        for (BloodDonor d : getAllDonors()) {
            map.put(d.getId(), d.getFullName());
        }
        return map;
    }

    // ---- helpers ---------------------------------------------------------

    private String hospitalName(Long id) {
        if (id == null) {
            return "-";
        }
        try {
            com.resqhub.model.Hospital h = hospitalDAO.findById(id);
            return h == null ? "#" + id : h.getName();
        } catch (DataAccessException e) {
            return "#" + id;
        }
    }

    private String victimName(Long id) {
        if (id == null) {
            return "-";
        }
        try {
            com.resqhub.model.Victim v = victimDAO.findById(id);
            return v == null ? "#" + id : v.getFullName();
        } catch (DataAccessException e) {
            return "#" + id;
        }
    }

    private String donorName(Long id) {
        if (id == null) {
            return "-";
        }
        try {
            BloodDonor d = service.getDonor(id);
            return d == null ? "#" + id : d.getFullName();
        } catch (DataAccessException e) {
            return "#" + id;
        }
    }

    private String requestCode(Long id) {
        if (id == null) {
            return "-";
        }
        try {
            BloodRequest r = service.getRequest(id);
            return r == null ? "#" + id : r.getRequestCode();
        } catch (DataAccessException e) {
            return "#" + id;
        }
    }

    private String shortText(String text) {
        if (text == null) {
            return "-";
        }
        return text.length() > 30 ? text.substring(0, 30) + "..." : text;
    }

    private int safeReceived(Long requestId) {
        if (requestId == null) {
            return 0;
        }
        try {
            return service.receivedUnits(requestId);
        } catch (DataAccessException e) {
            return 0;
        }
    }

    private int safeRemaining(Long requestId) {
        if (requestId == null) {
            return 0;
        }
        try {
            return service.remainingUnits(requestId);
        } catch (DataAccessException e) {
            return 0;
        }
    }

    private Long optionalId(String text) {
        return safeLongOrNull(text);
    }

    private Long safeLongOrNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private long safeLong(String text) {
        Long v = safeLongOrNull(text);
        return v == null ? -1 : v;
    }

    private LocalDate parseDateOrNull(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(text.trim(),
                        DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException(
                        "Date must be yyyy-MM-dd, got '" + text.trim() + "'");
            }
        }
    }
}
