package com.resqhub.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.resqhub.dao.FoodDistributionDAO;
import com.resqhub.dao.FoodDistributionRequestDAO;
import com.resqhub.dao.NotificationDAO;
import com.resqhub.dao.UserDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidFoodDistributionDataException;
import com.resqhub.exception.InvalidResourceDataException;
import com.resqhub.exception.InvalidShelterDataException;
import com.resqhub.exception.InvalidVolunteerDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.BeneficiaryType;
import com.resqhub.model.FoodDistribution;
import com.resqhub.model.FoodDistributionRequest;
import com.resqhub.model.FoodRequestStatus;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationPriority;
import com.resqhub.model.NotificationStatus;
import com.resqhub.model.NotificationType;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.Resource;
import com.resqhub.model.ResourceCategory;
import com.resqhub.model.RoleType;
import com.resqhub.model.Shelter;
import com.resqhub.model.VolunteerAssignment;
import com.resqhub.util.ValidationUtil;

/**
 * Food Distribution business logic (spec sections 1-21).
 *
 * Owns the food distribution request lifecycle (create -> approve ->
 * allocate -> assign -> distribute -> complete), the requirement
 * calculation from beneficiary counts, quantity tracking
 * (requested / allocated / distributed / remaining), the food-inventory
 * check that prevents negative stock, optional shelter-integration,
 * volunteer assignment and the food shortage / status notifications.
 *
 * Writes are limited to ADMIN, RESCUE_OFFICER and CAMP_MANAGER; reads
 * are open to every logged-in role.
 */
public class FoodDistributionService {

    private final FoodDistributionRequestDAO requestDAO =
            new FoodDistributionRequestDAO();
    private final FoodDistributionDAO distributionDAO =
            new FoodDistributionDAO();
    private final ResourceService resourceService = new ResourceService();
    private final VolunteerService volunteerService = new VolunteerService();
    private final ShelterService shelterService = new ShelterService();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final SessionManager session = SessionManager.getInstance();

    /** De-dup window for food shortage alerts (same as other alerts). */
    private static final long ALERT_DEDUP_SECONDS = 6 * 60 * 60;

    // ---- request creation & requirement calculation --------------------

    /**
     * Calculates the estimated food requirement for a number of
     * beneficiaries (beneficiaries x food per person). Spec section 4.
     */
    public int calculateRequirement(int beneficiaries, int mealsPerPerson) {
        return beneficiaries * mealsPerPerson;
    }

    /**
     * Creates a food distribution request. The required quantity can be
     * supplied directly, or derived via {@link #calculateRequirement}.
     */
    public FoodDistributionRequest createRequest(String requestCode,
            Long disasterId, String location, BeneficiaryType beneficiaryType,
            int beneficiaries, int requiredQuantity, PriorityLevel priority,
            String description)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException {

        requireWriteRole();
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(requestCode)) {
            errors.add("request code is required");
        }
        if (!ValidationUtil.requireNonBlank(location)) {
            errors.add("distribution location is required");
        }
        if (beneficiaryType == null) {
            errors.add("beneficiary type must be selected");
        }
        if (beneficiaries <= 0) {
            errors.add("beneficiary count must be greater than zero");
        }
        if (requiredQuantity <= 0) {
            errors.add("required food quantity must be greater than zero");
        }
        if (!errors.isEmpty()) {
            throw new InvalidFoodDistributionDataException(
                    String.join("; ", errors));
        }

        FoodDistributionRequest r = new FoodDistributionRequest();
        r.setRequestCode(requestCode);
        r.setDisasterId(disasterId);
        r.setLocation(location);
        r.setBeneficiaryType(beneficiaryType);
        r.setBeneficiaries(beneficiaries);
        r.setRequiredQuantity(requiredQuantity);
        r.setPriority(priority == null ? PriorityLevel.MEDIUM : priority);
        r.setStatus(FoodRequestStatus.PENDING);
        r.setDescription(description);
        r.setRequestedAt(LocalDateTime.now());
        r.setCreatedBy(session.currentUserId());
        FoodDistributionRequest saved = requestDAO.save(r);

        notifyStatus(saved, "High-priority food request created "
                + saved.getRequestCode() + " for " + saved.getBeneficiaries()
                + " people at " + saved.getLocation() + ".",
                saved.getPriority() != null
                        && (saved.getPriority() == PriorityLevel.CRITICAL
                                || saved.getPriority() == PriorityLevel.HIGH)
                        ? NotificationPriority.WARNING : NotificationPriority.INFO);
        return saved;
    }

    /**
     * Convenience: builds a requirement from beneficiary count then
     * creates the request (spec section 4 "Total Required" example).
     */
    public FoodDistributionRequest createRequestWithCalculation(
            String requestCode, Long disasterId, String location,
            BeneficiaryType beneficiaryType, int beneficiaries,
            int mealsPerPerson, PriorityLevel priority, String description)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException {

        return createRequest(requestCode, disasterId, location,
                beneficiaryType, beneficiaries,
                calculateRequirement(beneficiaries, mealsPerPerson), priority,
                description);
    }

    /**
     * Updates request fields when disaster conditions change (spec
     * section 17). Quantity / status changes flow through here so a
     * changing beneficiary count can raise the requirement.
     */
    public FoodDistributionRequest updateRequest(long requestId,
            String location, BeneficiaryType beneficiaryType,
            int beneficiaries, int requiredQuantity, PriorityLevel priority,
            String description)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException {

        requireWriteRole();
        FoodDistributionRequest r = requireExisting(requestId);
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(location)) {
            errors.add("distribution location is required");
        }
        if (beneficiaryType == null) {
            errors.add("beneficiary type must be selected");
        }
        if (beneficiaries <= 0) {
            errors.add("beneficiary count must be greater than zero");
        }
        if (requiredQuantity <= 0) {
            errors.add("required food quantity must be greater than zero");
        }
        if (!errors.isEmpty()) {
            throw new InvalidFoodDistributionDataException(
                    String.join("; ", errors));
        }
        r.setLocation(location);
        r.setBeneficiaryType(beneficiaryType);
        r.setBeneficiaries(beneficiaries);
        r.setRequiredQuantity(requiredQuantity);
        r.setPriority(priority == null ? PriorityLevel.MEDIUM : priority);
        r.setDescription(description);
        return requestDAO.save(r);
    }

    /** Changes the request's status directly (spec section 17). */
    public FoodDistributionRequest setStatus(long requestId,
            FoodRequestStatus status)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException {

        requireWriteRole();
        if (status == null) {
            throw new InvalidFoodDistributionDataException(
                    "a status must be selected");
        }
        FoodDistributionRequest r = requireExisting(requestId);
        r.setStatus(status);
        if (status == FoodRequestStatus.COMPLETED && r.getCompletedAt() == null) {
            r.setCompletedAt(LocalDateTime.now());
        }
        return requestDAO.save(r);
    }

    // ---- workflow: approve / allocate ---------------------------------

    /** PENDING -> APPROVED (food allocation approved). */
    public FoodDistributionRequest approveRequest(long requestId)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException {

        requireWriteRole();
        FoodDistributionRequest r = requireExisting(requestId);
        if (r.getStatus() != FoodRequestStatus.PENDING) {
            throw new InvalidFoodDistributionDataException(
                    "Only a PENDING request can be approved (was "
                            + r.getStatus().getLabel() + ")");
        }
        r.setStatus(FoodRequestStatus.APPROVED);
        FoodDistributionRequest saved = requestDAO.save(r);
        notifyStatus(saved, "Food allocation approved for "
                + saved.getRequestCode() + " (" + saved.getAllocatedQuantity()
                + " of " + saved.getRequiredQuantity() + " reserved).",
                NotificationPriority.INFO);
        return saved;
    }

    /**
     * Approves and allocates food for a request after checking that the
     * chosen resource has enough stock (spec sections 6, 7, 8). When only
     * part of the requirement can be met the request becomes
     * PARTIALLY_FULFILLED (spec section 11) - reservation-only, stock is
     * reduced on actual distribution.
     */
    public FoodDistributionRequest allocateRequest(long requestId,
            long resourceId, int quantity)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException {

        requireWriteRole();
        FoodDistributionRequest r = requireExisting(requestId);
        if (quantity <= 0) {
            throw new InvalidFoodDistributionDataException(
                    "allocation quantity must be greater than zero");
        }
        if (r.getStatus() == FoodRequestStatus.COMPLETED
                || r.getStatus() == FoodRequestStatus.CANCELLED) {
            throw new InvalidFoodDistributionDataException(
                    "A " + r.getStatus().getLabel()
                            + " request cannot be allocated");
        }
        Resource food = resourceService.getResource(resourceId);
        if (food == null) {
            throw new InvalidFoodDistributionDataException(
                    "No inventory resource with id " + resourceId);
        }
        if (food.getCategory() != ResourceCategory.FOOD) {
            throw new InvalidFoodDistributionDataException(
                    "Only a FOOD category resource can be allocated - '"
                            + food.getName() + "' is "
                            + (food.getCategory() == null ? "OTHER"
                                    : food.getCategory().getLabel()));
        }
        if (food.getAvailableQuantity() < quantity) {
            int shortage = quantity - food.getAvailableQuantity();
            throw new InvalidFoodDistributionDataException(
                    "Insufficient food stock: "
                            + food.getName() + " has only "
                            + food.getAvailableQuantity()
                            + " available, but " + quantity
                            + " were requested (short by " + shortage + ")");
        }

        r.setAllocatedQuantity(quantity);
        r.setAllocatedResourceId(resourceId);
        r.setAllocatedAt(LocalDateTime.now());
        r.setAllocatedBy(session.currentUserId());
        r.setStatus(quantity < r.getRequiredQuantity()
                ? FoodRequestStatus.PARTIALLY_FULFILLED
                : FoodRequestStatus.ALLOCATED);
        FoodDistributionRequest saved = requestDAO.save(r);
        notifyStatus(saved, "Food allocated for " + saved.getRequestCode()
                + ": " + quantity + " from " + food.getName() + ".",
                NotificationPriority.INFO);
        return saved;
    }

    // ---- assignment (spec section 13) ---------------------------------

    /** Assigns a volunteer to run the food distribution operation. */
    public FoodDistributionRequest assignVolunteer(long requestId,
            long volunteerId)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException,
            InvalidVolunteerDataException {

        requireWriteRole();
        FoodDistributionRequest r = requireExisting(requestId);
        PriorityLevel p = r.getPriority() == null ? PriorityLevel.MEDIUM
                : r.getPriority();
        VolunteerAssignment assignment = volunteerService.assignTask(
                volunteerId,
                "Food Distribution - " + r.getRequestCode(),
                "Distribute food to " + r.getBeneficiaries() + " people"
                        + (r.getDescription() == null
                                ? "" : " - " + r.getDescription()),
                r.getLocation(), p.getWeight());
        r.setAssignedVolunteerId(volunteerId);
        r.setAssignedAt(LocalDateTime.now());
        FoodDistributionRequest saved = requestDAO.save(r);
        if (saved.getStatus() == FoodRequestStatus.APPROVED
                || saved.getStatus() == FoodRequestStatus.PENDING) {
            saved.setStatus(FoodRequestStatus.ALLOCATED);
            saved = requestDAO.save(saved);
        }
        notifyStatus(saved, "Volunteer assigned to food distribution "
                + saved.getRequestCode() + " (" + assignment.getId() + ").",
                NotificationPriority.INFO);
        return saved;
    }

    // ---- distribution / completion (sections 14, 18) -----------------

    /**
     * Records an actual food distribution: validates quantity against the
     * remaining requirement AND the allocation (spec section 18), reduces
     * the food inventory (spec section 6 / 14) and updates the request's
     * status / progress.
     */
    public FoodDistribution recordDistribution(long requestId,
            long resourceId, int quantity, int beneficiariesServed,
            String location, String note)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException,
            InvalidResourceDataException {

        requireWriteRole();
        FoodDistributionRequest r = requireExisting(requestId);
        if (quantity <= 0) {
            throw new InvalidFoodDistributionDataException(
                    "distribution quantity must be greater than zero");
        }
        if (r.getStatus() == FoodRequestStatus.COMPLETED
                || r.getStatus() == FoodRequestStatus.CANCELLED) {
            throw new InvalidFoodDistributionDataException(
                    "A " + r.getStatus().getLabel()
                            + " request cannot receive more food");
        }
        int distributed = requestDAO.sumDistributed(requestId);
        int remaining = r.getRequiredQuantity() - distributed;
        if (quantity > remaining) {
            throw new InvalidFoodDistributionDataException(
                    "Distribution exceeds the remaining requirement: "
                            + "only " + remaining + " of "
                            + r.getRequiredQuantity() + " remain, but "
                            + quantity + " were requested");
        }
        if (r.getAllocatedQuantity() > 0
                && distributed + quantity > r.getAllocatedQuantity()) {
            throw new InvalidFoodDistributionDataException(
                    "Distribution exceeds the allocation: "
                            + distributed + " already distributed, "
                            + r.getAllocatedQuantity() + " allocated (only "
                            + (r.getAllocatedQuantity() - distributed)
                            + " remain from the allocation)");
        }
        Resource food = resourceService.getResource(resourceId);
        if (food == null) {
            throw new InvalidFoodDistributionDataException(
                    "No inventory resource with id " + resourceId);
        }

        int bef = food.getAvailableQuantity();
        resourceService.stockOut(resourceId, quantity, location, note,
                r.getDisasterId());

        FoodDistribution d = new FoodDistribution();
        d.setRequestId(requestId);
        d.setResourceId(resourceId);
        d.setQuantity(quantity);
        d.setBeneficiariesServed(beneficiariesServed);
        d.setDistributedTo(r.getLocation());
        d.setLocation(location);
        d.setDistributedAt(LocalDateTime.now());
        d.setDistributedBy(session.currentUserId());
        d.setNote(note);
        FoodDistribution saved = distributionDAO.save(d);

        int newTotal = distributed + quantity;
        if (newTotal >= r.getRequiredQuantity()) {
            r.setStatus(FoodRequestStatus.COMPLETED);
            r.setCompletedAt(LocalDateTime.now());
        } else {
            r.setStatus(FoodRequestStatus.IN_PROGRESS);
        }
        requestDAO.save(r);

        notifyStatus(r, "Food distribution completed for "
                + r.getRequestCode() + ": " + quantity + " units handed out "
                + "at " + location + " (" + r.getBeneficiaries() + " people).",
                NotificationPriority.INFO);
        return saved;
    }

    /** Cancels an unfinished request (spec section 10). */
    public FoodDistributionRequest cancelRequest(long requestId)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException {

        requireWriteRole();
        FoodDistributionRequest r = requireExisting(requestId);
        if (r.getStatus() == FoodRequestStatus.COMPLETED
                || r.getStatus() == FoodRequestStatus.CANCELLED) {
            throw new InvalidFoodDistributionDataException(
                    "Request is already " + r.getStatus().getLabel());
        }
        r.setStatus(FoodRequestStatus.CANCELLED);
        return requestDAO.save(r);
    }

    // ---- shelter integration (spec section 19) -------------------------

    /** Estimated requirement for a shelter's current occupancy. */
    public int requirementForShelter(long shelterId, int mealsPerPerson)
            throws DataAccessException, InvalidShelterDataException {
        Shelter s = shelterService.requireExisting(shelterId);
        return calculateRequirement(s.getCurrentOccupancy(), mealsPerPerson);
    }

    /**
     * Creates a request derived from a shelter's occupancy (spec
     * section 19). Location defaults to the shelter's name/district.
     */
    public FoodDistributionRequest createRequestForShelter(long shelterId,
            Long disasterId, int mealsPerPerson, PriorityLevel priority,
            String description)
            throws UnauthorizedOperationException,
            InvalidFoodDistributionDataException, DataAccessException,
            InvalidShelterDataException {

        Shelter s = shelterService.requireExisting(shelterId);
        int people = s.getCurrentOccupancy();
        if (people <= 0) {
            throw new InvalidFoodDistributionDataException(
                    "Shelter '" + s.getName() + "' is currently empty ("
                            + people + " occupants) - nothing to request");
        }
        String code = "FD-SHL-" + s.getId() + "-"
                + (System.currentTimeMillis() % 1000000);
        String location = s.getName() + (s.getDistrict() == null
                || s.getDistrict().isEmpty() ? ""
                        : ", " + s.getDistrict());
        return createRequest(code, disasterId, location,
                BeneficiaryType.SHELTER, people,
                calculateRequirement(people, mealsPerPerson), priority,
                description);
    }

    /**
     * When a shelter's occupancy grows, raises the requirement of any
     * still-open request bound to that shelter location (spec section 19).
     * Returns true if a request was updated.
     */
    public boolean updateRequestForShelter(long shelterId,
            int mealsPerPerson)
            throws UnauthorizedOperationException, DataAccessException,
            InvalidShelterDataException {

        requireWriteRole();
        Shelter s = shelterService.requireExisting(shelterId);
        String location = (s.getName() + (s.getDistrict() == null
                || s.getDistrict().isEmpty() ? "" : ", " + s.getDistrict()))
                .toLowerCase();
        List<FoodDistributionRequest> open = requestDAO.findOpenByLocation(
                location);
        if (open.isEmpty()) {
            return false;
        }
        for (FoodDistributionRequest r : open) {
            int required = calculateRequirement(s.getCurrentOccupancy(),
                    mealsPerPerson);
            if (r.getBeneficiaries() != s.getCurrentOccupancy()
                    || r.getRequiredQuantity() != required) {
                r.setBeneficiaries(s.getCurrentOccupancy());
                r.setRequiredQuantity(required);
                requestDAO.save(r);
            }
        }
        return true;
    }

    // ---- notifications (spec section 21) -------------------------------

    /**
     * Broadcasts de-duplicated FOOD shortage alerts for every open
     * request whose requirement exceeds the current FOOD inventory.
     * Returns the number of new alerts created.
     */
    public int generateFoodShortageAlerts()
            throws UnauthorizedOperationException, DataAccessException {

        requireWriteRole();
        int available = availableFoodStock();
        List<FoodDistributionRequest> shortages = shortageRequests();
        if (shortages.isEmpty()) {
            return 0;
        }
        Set<Long> audience = new LinkedHashSet<>();
        for (com.resqhub.model.User user
                : userDAO.findByRole(RoleType.ADMIN)) {
            audience.add(user.getId());
        }
        for (com.resqhub.model.User user
                : userDAO.findByRole(RoleType.CAMP_MANAGER)) {
            audience.add(user.getId());
        }
        if (audience.isEmpty()) {
            return 0;
        }
        int created = 0;
        for (FoodDistributionRequest r : shortages) {
            String dedup = "FOOD_SHORTAGE:" + r.getId();
            if (notificationDAO.findRecentByDedupKey(dedup,
                    ALERT_DEDUP_SECONDS) != null) {
                continue;
            }
            int shortage = r.getRequiredQuantity() - available;
            String message = "FOOD SHORTAGE ALERT: Location " + r.getLocation()
                    + " - required " + r.getRequiredQuantity()
                    + ", available " + available + ", shortage " + shortage
                    + " (request " + r.getRequestCode() + ").";
            for (Long userId : audience) {
                Notification n = new Notification();
                n.setRecipientUserId(userId);
                n.setType(NotificationType.FOOD);
                n.setPriority(NotificationPriority.WARNING);
                n.setStatus(NotificationStatus.UNREAD);
                n.setMessage(message);
                n.setRelatedModule("Food Distribution");
                n.setRelatedEventId(r.getId());
                n.setAutoGenerated(true);
                n.setDedupKey(dedup);
                notificationDAO.save(n);
                created++;
            }
        }
        return created;
    }

    /** Open requests whose requirement exceeds current food inventory. */
    public List<FoodDistributionRequest> shortageRequests()
            throws DataAccessException {
        int available = availableFoodStock();
        List<FoodDistributionRequest> out = new ArrayList<>();
        for (FoodDistributionRequest r : findOpen()) {
            if (r.remainingQuantity() > available) {
                out.add(r);
            }
        }
        return out;
    }

    /** Sum of available FOOD-category inventory (units). */
    public int availableFoodStock() throws DataAccessException {
        int total = 0;
        for (Resource r : resourceService.filter(null, ResourceCategory.FOOD,
                null)) {
            total += r.getAvailableQuantity();
        }
        return total;
    }

    // ---- reads / search / filter --------------------------------------

    public FoodDistributionRequest getRequest(long requestId)
            throws DataAccessException {
        return hydrate(requestDAO.findById(requestId));
    }

    public FoodDistributionRequest requireExisting(long requestId)
            throws InvalidFoodDistributionDataException, DataAccessException {
        FoodDistributionRequest r = requestDAO.findById(requestId);
        if (r == null) {
            throw new InvalidFoodDistributionDataException(
                    "No food distribution request with id " + requestId);
        }
        return hydrate(r);
    }

    public List<FoodDistributionRequest> getAllRequests()
            throws DataAccessException {
        return hydrateAll(requestDAO.findAll());
    }

    public List<FoodDistributionRequest> findOpen() throws DataAccessException {
        return hydrateAll(requestDAO.findOpen());
    }

    public List<FoodDistributionRequest> findByStatus(FoodRequestStatus status)
            throws DataAccessException {
        return hydrateAll(requestDAO.findByStatus(status));
    }

    public List<FoodDistributionRequest> findByPriority(PriorityLevel p)
            throws DataAccessException {
        return hydrateAll(requestDAO.findByPriority(p));
    }

    public List<FoodDistributionRequest> filter(String keyword,
            Long disasterId, String location, FoodRequestStatus status,
            PriorityLevel priority) throws DataAccessException {
        return hydrateAll(requestDAO.filter(keyword, disasterId, location,
                status, priority));
    }

    public List<FoodDistributionRequest> search(String keyword)
            throws DataAccessException {
        return hydrateAll(requestDAO.search(keyword));
    }

    public List<FoodDistribution> getDistributions(long requestId)
            throws DataAccessException {
        return distributionDAO.findByRequest(requestId);
    }

    public List<FoodDistribution> getAllDistributions()
            throws DataAccessException {
        return distributionDAO.findAll();
    }

    public List<FoodDistribution> filterDistributions(Long requestId,
            String location, Long disasterId) throws DataAccessException {
        return distributionDAO.filter(requestId, location, disasterId);
    }

    public List<Resource> getFoodResources() throws DataAccessException {
        return resourceService.filter(null, ResourceCategory.FOOD, null);
    }

    public List<Shelter> getAllShelters() throws DataAccessException {
        return shelterService.getAllShelters();
    }

    // ---- statistics (spec section 23) ---------------------------------

    public int countRequests() throws DataAccessException {
        return requestDAO.findAll().size();
    }

    public int countPending() throws DataAccessException {
        return requestDAO.findByStatus(FoodRequestStatus.PENDING).size();
    }

    public int countCompleted() throws DataAccessException {
        return requestDAO.findByStatus(FoodRequestStatus.COMPLETED).size();
    }

    public int countOpen() throws DataAccessException {
        return requestDAO.findOpen().size();
    }

    public int totalBeneficiaries() throws DataAccessException {
        int total = 0;
        for (FoodDistributionRequest r : requestDAO.findAll()) {
            total += r.getBeneficiaries();
        }
        return total;
    }

    public int totalBeneficiariesServed() throws DataAccessException {
        int total = 0;
        for (FoodDistribution d : distributionDAO.findAll()) {
            total += d.getBeneficiariesServed();
        }
        return total;
    }

    public int totalAllocated() throws DataAccessException {
        int total = 0;
        for (FoodDistributionRequest r : requestDAO.findAll()) {
            total += r.getAllocatedQuantity();
        }
        return total;
    }

    public int totalDistributed() throws DataAccessException {
        int total = 0;
        for (FoodDistribution d : distributionDAO.findAll()) {
            total += d.getQuantity();
        }
        return total;
    }

    public int totalRequired() throws DataAccessException {
        int total = 0;
        for (FoodDistributionRequest r : requestDAO.findAll()) {
            total += r.getRequiredQuantity();
        }
        return total;
    }

    public int totalRemaining() throws DataAccessException {
        int total = 0;
        for (FoodDistributionRequest r : requestDAO.findAll()) {
            total += r.remainingQuantity();
        }
        return total;
    }

    // ---- helpers -------------------------------------------------------

    private void requireWriteRole() throws UnauthorizedOperationException {
        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
    }

    private void notifyStatus(FoodDistributionRequest r,
            String message, NotificationPriority priority) {
        try {
            new NotificationService().createNotification(
                    session.currentUserId(), NotificationType.FOOD, priority,
                    message, "Food Distribution", r.getId());
        } catch (Exception ignored) {
            // a failed alert must never block the primary operation
        }
    }

    private FoodDistributionRequest hydrate(FoodDistributionRequest r)
            throws DataAccessException {
        if (r == null) {
            return null;
        }
        r.setDistributedQuantity(requestDAO.sumDistributed(r.getId()));
        return r;
    }

    private List<FoodDistributionRequest> hydrateAll(
            List<FoodDistributionRequest> list) throws DataAccessException {
        for (FoodDistributionRequest r : list) {
            hydrate(r);
        }
        return list;
    }
}
