package com.resqhub.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.resqhub.dao.DisasterDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidShelterDataException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.BeneficiaryType;
import com.resqhub.model.Disaster;
import com.resqhub.model.FoodDistribution;
import com.resqhub.model.FoodDistributionRequest;
import com.resqhub.model.FoodRequestStatus;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.Resource;
import com.resqhub.model.Shelter;
import com.resqhub.model.Volunteer;
import com.resqhub.service.FoodDistributionService;
import com.resqhub.service.VolunteerService;
import com.resqhub.util.InputParser;

/** Food Distribution screen controller: UI input -> typed service calls. */
public class FoodDistributionController {

    private final FoodDistributionService foodService =
            new FoodDistributionService();
    private final DisasterDAO disasterDAO = new DisasterDAO();
    private final VolunteerService volunteerService = new VolunteerService();

    // ---- request creation ---------------------------------------------

    public ActionResult createRequest(String code, String disasterIdText,
            String location, BeneficiaryType beneficiaryType,
            String beneficiariesText, String requiredText,
            PriorityLevel priority, String description) {
        try {
            Long disasterId = parseOptionalId(disasterIdText);
            int beneficiaries = InputParser.parseInt(beneficiariesText,
                    "Beneficiary count");
            int required = InputParser.parseInt(requiredText,
                    "Required quantity");
            FoodDistributionRequest r = foodService.createRequest(code,
                    disasterId, location, beneficiaryType, beneficiaries,
                    required, priority, description);
            return ActionResult.successWithData(
                    "Request " + r.getRequestCode() + " created for "
                            + r.getBeneficiaries() + " people at "
                            + r.getLocation() + " ("
                            + r.getRequiredQuantity() + " required).", r);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /** Creates a request, calculating the requirement from beneficiary
     *  count x food per person (spec section 4). */
    public ActionResult createRequestWithCalculation(String code,
            String disasterIdText, String location,
            BeneficiaryType beneficiaryType, String beneficiariesText,
            String mealsPerPersonText, PriorityLevel priority,
            String description) {
        try {
            Long disasterId = parseOptionalId(disasterIdText);
            int beneficiaries = InputParser.parseInt(beneficiariesText,
                    "Beneficiary count");
            int perPerson = InputParser.parseInt(mealsPerPersonText,
                    "Food per person");
            FoodDistributionRequest r =
                    foodService.createRequestWithCalculation(code, disasterId,
                            location, beneficiaryType, beneficiaries,
                            perPerson, priority, description);
            return ActionResult.successWithData(
                    "Request " + r.getRequestCode() + " created - calculated "
                            + r.getBeneficiaries() + " people x " + perPerson
                            + " = " + r.getRequiredQuantity() + " required.",
                    r);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- update / status ----------------------------------------------

    public ActionResult updateRequest(long id, String location,
            BeneficiaryType beneficiaryType, String beneficiariesText,
            String requiredText, PriorityLevel priority,
            String description) {
        try {
            int beneficiaries = InputParser.parseInt(beneficiariesText,
                    "Beneficiary count");
            int required = InputParser.parseInt(requiredText,
                    "Required quantity");
            FoodDistributionRequest r = foodService.updateRequest(id,
                    location, beneficiaryType, beneficiaries, required,
                    priority, description);
            return ActionResult.success("Request " + r.getRequestCode()
                    + " updated (now " + r.getBeneficiaries() + " people, "
                    + r.getRequiredQuantity() + " required)");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult setStatus(long id, FoodRequestStatus status) {
        try {
            FoodDistributionRequest r = foodService.setStatus(id, status);
            return ActionResult.success("Request " + r.getRequestCode()
                    + " -> " + r.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- workflow ------------------------------------------------------

    public ActionResult approveRequest(long id) {
        try {
            FoodDistributionRequest r = foodService.approveRequest(id);
            return ActionResult.success("Request " + r.getRequestCode()
                    + " approved");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult allocateRequest(long id, String resourceIdText,
            String quantityText) {
        try {
            long resourceId = InputParser.parseLong(resourceIdText,
                    "Food resource");
            int quantity = InputParser.parseInt(quantityText, "Quantity");
            FoodDistributionRequest r = foodService.allocateRequest(id,
                    resourceId, quantity);
            return ActionResult.success("Allocated " + quantity + " to "
                    + r.getRequestCode() + " -> " + r.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult assignVolunteer(long id, String volunteerIdText) {
        try {
            long volunteerId = InputParser.parseLong(volunteerIdText,
                    "Volunteer");
            FoodDistributionRequest r = foodService.assignVolunteer(id,
                    volunteerId);
            return ActionResult.success("Volunteer #" + volunteerId
                    + " assigned to " + r.getRequestCode());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult recordDistribution(long id, String resourceIdText,
            String quantityText, String servedText, String location,
            String note) {
        try {
            long resourceId = InputParser.parseLong(resourceIdText,
                    "Food resource");
            int quantity = InputParser.parseInt(quantityText, "Quantity");
            int served = servedText == null || servedText.trim().isEmpty()
                    ? 0 : InputParser.parseInt(servedText,
                            "Beneficiaries served");
            FoodDistribution d = foodService.recordDistribution(id,
                    resourceId, quantity, served, location, note);
            return ActionResult.success("Distributed " + quantity + " units "
                    + "against request #" + id + " at " + location);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult cancelRequest(long id) {
        try {
            FoodDistributionRequest r = foodService.cancelRequest(id);
            return ActionResult.success("Request " + r.getRequestCode()
                    + " cancelled");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult generateFoodShortageAlerts() {
        try {
            int created = foodService.generateFoodShortageAlerts();
            return ActionResult.success(created == 0
                    ? "No new food shortage alerts (no open shortages, or "
                            + "alerts already raised)"
                    : "Generated " + created + " food shortage alert(s)");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- reads ---------------------------------------------------------

    public List<FoodDistributionRequest> getAllRequests()
            throws DataAccessException {
        return foodService.getAllRequests();
    }

    public List<FoodDistributionRequest> findOpen() throws DataAccessException {
        return foodService.findOpen();
    }

    public List<FoodDistributionRequest> getPending()
            throws DataAccessException {
        return foodService.findByStatus(FoodRequestStatus.PENDING);
    }

    public List<FoodDistributionRequest> getHighPriority()
            throws DataAccessException {
        return foodService.findByPriority(PriorityLevel.HIGH);
    }

    public List<FoodDistributionRequest> filter(String keyword,
            String disasterIdText, String location,
            FoodRequestStatus status, PriorityLevel priority)
            throws DataAccessException {
        Long disasterId = parseOptionalId(disasterIdText);
        return foodService.filter(keyword, disasterId, location, status,
                priority);
    }

    public List<FoodDistributionRequest> search(String keyword)
            throws DataAccessException {
        return foodService.search(keyword);
    }

    public FoodDistributionRequest getRequest(long id)
            throws DataAccessException {
        return foodService.getRequest(id);
    }

    public List<FoodDistribution> getDistributions(long requestId)
            throws DataAccessException {
        return foodService.getDistributions(requestId);
    }

    public List<FoodDistribution> getAllDistributions()
            throws DataAccessException {
        return foodService.getAllDistributions();
    }

    public List<Resource> getFoodResources() throws DataAccessException {
        return foodService.getFoodResources();
    }

    public List<Shelter> getAllShelters() throws DataAccessException {
        return foodService.getAllShelters();
    }

    public List<Volunteer> getAllVolunteers() throws DataAccessException {
        return volunteerService.getAllVolunteers();
    }

    public List<Disaster> getDisasters() throws DataAccessException {
        return disasterDAO.findAll();
    }

    public int requirementForShelter(long shelterId, int mealsPerPerson)
            throws DataAccessException, InvalidShelterDataException {
        return foodService.requirementForShelter(shelterId, mealsPerPerson);
    }

    public ActionResult createRequestFromShelter(long shelterId,
            String mealsPerPersonText, PriorityLevel priority) {
        try {
            int perPerson = InputParser.parseInt(mealsPerPersonText,
                    "Meals per person");
            FoodDistributionRequest r = foodService.createRequestForShelter(
                    shelterId, null, perPerson, priority, null);
            return ActionResult.successWithData(
                    "Request " + r.getRequestCode() + " created for shelter "
                            + "with " + r.getBeneficiaries() + " people, "
                            + r.getRequiredQuantity() + " required.", r);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- statistics ----------------------------------------------------

    public int countRequests() throws DataAccessException {
        return foodService.countRequests();
    }

    public int countPending() throws DataAccessException {
        return foodService.countPending();
    }

    public int countCompleted() throws DataAccessException {
        return foodService.countCompleted();
    }

    public int countOpen() throws DataAccessException {
        return foodService.countOpen();
    }

    public int totalBeneficiaries() throws DataAccessException {
        return foodService.totalBeneficiaries();
    }

    public int totalBeneficiariesServed() throws DataAccessException {
        return foodService.totalBeneficiariesServed();
    }

    public int totalAllocated() throws DataAccessException {
        return foodService.totalAllocated();
    }

    public int totalDistributed() throws DataAccessException {
        return foodService.totalDistributed();
    }

    public int totalRequired() throws DataAccessException {
        return foodService.totalRequired();
    }

    public int totalRemaining() throws DataAccessException {
        return foodService.totalRemaining();
    }

    // ---- row / header helpers ------------------------------------------

    public static Object[] requestRow(FoodDistributionRequest r,
            Map<Long, String> disasterNames) {
        return new Object[]{
                r.getId(),
                r.getRequestCode(),
                r.getDisasterId() == null ? "-"
                        : disasterNames.getOrDefault(r.getDisasterId(), "?"),
                r.getLocation(),
                r.getBeneficiaryType() == null ? "-"
                        : r.getBeneficiaryType().getLabel(),
                r.getBeneficiaries(),
                r.getRequiredQuantity(),
                r.getAllocatedQuantity(),
                r.getDistributedQuantity(),
                r.remainingQuantity(),
                r.getPriority() == null ? "-" : r.getPriority().getLabel(),
                r.getStatus() == null ? "-" : r.getStatus().getLabel(),
                r.getRequestedAt() == null ? "-"
                        : r.getRequestedAt().toString().replace('T', ' ')
        };
    }

    public static String[] requestHeaders() {
        return new String[]{"ID", "Code", "Disaster", "Location",
                "Beneficiary", "People", "Required", "Allocated",
                "Distributed", "Remaining", "Priority", "Status", "Date"};
    }

    public static Object[] distributionRow(FoodDistribution d,
            String requestCode, String resourceName) {
        return new Object[]{
                d.getId(),
                requestCode == null ? "#" + d.getRequestId() : requestCode,
                resourceName == null ? "-" : resourceName,
                d.getQuantity(),
                d.getBeneficiariesServed(),
                d.getDistributedTo(),
                d.getLocation(),
                d.getDistributedAt() == null ? "-"
                        : d.getDistributedAt().toString().replace('T', ' '),
                d.getNote()
        };
    }

    public static String[] distributionHeaders() {
        return new String[]{"ID", "Request", "Resource", "Qty", "Served",
                "Distributed To", "Location", "Date", "Note"};
    }

    public Map<Long, String> disasterNameMap() throws DataAccessException {
        Map<Long, String> map = new HashMap<>();
        for (Disaster d : getDisasters()) {
            map.put(d.getId(), d.getTitle());
        }
        return map;
    }

    public List<Object[]> allRequestRows() throws DataAccessException {
        Map<Long, String> disasters = disasterNameMap();
        List<Object[]> rows = new java.util.ArrayList<>();
        for (FoodDistributionRequest r : getAllRequests()) {
            rows.add(requestRow(r, disasters));
        }
        return rows;
    }

    public List<Object[]> allDistributionRows() {
        Map<Long, String> codes = new HashMap<>();
        try {
            for (FoodDistributionRequest r : getAllRequests()) {
                codes.put(r.getId(), r.getRequestCode());
            }
        } catch (DataAccessException ignored) {
            // reading for display only
        }
        Map<Long, String> names = new HashMap<>();
        try {
            for (Resource r : getFoodResources()) {
                names.put(r.getId(), r.getName());
            }
        } catch (DataAccessException ignored) {
            // reading for display only
        }
        List<Object[]> rows = new java.util.ArrayList<>();
        try {
            for (FoodDistribution d : getAllDistributions()) {
                rows.add(distributionRow(d, codes.get(d.getRequestId()),
                        names.get(d.getResourceId())));
            }
        } catch (DataAccessException ignored) {
            // reading for display only
        }
        return rows;
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
}
