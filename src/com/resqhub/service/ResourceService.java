package com.resqhub.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.resqhub.dao.NotificationDAO;
import com.resqhub.dao.ResourceDAO;
import com.resqhub.dao.ResourceDistributionDAO;
import com.resqhub.dao.StockMovementDAO;
import com.resqhub.dao.UserDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidResourceDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.DistributionDestination;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationPriority;
import com.resqhub.model.NotificationStatus;
import com.resqhub.model.NotificationType;
import com.resqhub.model.Resource;
import com.resqhub.model.ResourceCategory;
import com.resqhub.model.ResourceDistribution;
import com.resqhub.model.ResourceStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.StockMovement;
import com.resqhub.model.StockMovementType;
import com.resqhub.model.User;
import com.resqhub.util.ValidationUtil;

/**
 * Resource &amp; Inventory business logic.
 *
 * Owns the inventory registry, stock-in / stock-out / distribution
 * transactions, the automatic availability-status computation and the
 * low-stock alert integration with the Notifications &amp; Alerts module.
 *
 * Central business rules enforced here:
 *   - stock can never fall below zero (negative-inventory prevention)
 *   - a distribution/stock-out only proceeds when enough quantity exists
 *   - every quantity change is recorded as a stock movement so history
 *     stays clear (direct quantity edits are not allowed)
 *   - availability (Available / Low Stock / Out of Stock) is recomputed
 *     automatically from quantity vs. minimum level after every change
 *   - shortages generate de-duplicated LOW_STOCK notifications (spec
 *     sections 8, 9, 21)
 *
 * Writes are limited to ADMIN, RESCUE_OFFICER and CAMP_MANAGER; reads
 * are open to every logged-in role.
 */
public class ResourceService {

    private final ResourceDAO resourceDAO = new ResourceDAO();
    private final StockMovementDAO movementDAO = new StockMovementDAO();
    private final ResourceDistributionDAO distributionDAO =
            new ResourceDistributionDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final SessionManager session = SessionManager.getInstance();

    /** De-dup window for LOW_STOCK alerts (kept in sync with Alerts). */
    private static final long ALERT_DEDUP_SECONDS = 6 * 60 * 60;

    // ── registration / update ────────────────────────────────────────

    public Resource createResource(String name, String code,
            ResourceCategory category, int availableQuantity,
            int minimumLevel, String unit, String description)
            throws UnauthorizedOperationException,
            InvalidResourceDataException, DataAccessException {

        requireWriteRole();
        List<String> errors = validateBasics(name, code, category,
                availableQuantity, minimumLevel);
        if (!errors.isEmpty()) {
            throw new InvalidResourceDataException(String.join("; ", errors));
        }

        Resource r = new Resource();
        r.setName(name);
        r.setCode(code);
        r.setCategory(category == null ? ResourceCategory.OTHER : category);
        r.setAvailableQuantity(availableQuantity);
        r.setMinimumLevel(minimumLevel);
        r.setUnit(unit);
        r.setDescription(description);
        r.setCreatedBy(session.currentUserId());
        return resourceDAO.save(r);
    }

    /**
     * Updates editable resource details (name, category, min level, unit,
     * description). The available quantity is intentionally NOT editable
     * here - it must change through stock-in / stock-out transactions so
     * history stays intact (spec sections 19, 21).
     */
    public Resource updateResource(long resourceId, String name,
            ResourceCategory category, int minimumLevel, String unit,
            String description)
            throws UnauthorizedOperationException,
            InvalidResourceDataException, DataAccessException {

        requireWriteRole();
        Resource existing = requireExisting(resourceId);
        List<String> errors = validateBasics(name, existing.getCode(),
                category, existing.getAvailableQuantity(), minimumLevel);
        if (!errors.isEmpty()) {
            throw new InvalidResourceDataException(String.join("; ", errors));
        }
        existing.setName(name);
        existing.setCategory(category);
        existing.setMinimumLevel(minimumLevel);
        existing.setUnit(unit);
        existing.setDescription(description);
        return resourceDAO.save(existing);
    }

    // ── stock transactions ───────────────────────────────────────────

    /**
     * Records resources entering the inventory and increases the quantity
     * (source: donation / government / partner / emergency procurement /
     * other relief source). A stock-in movement is always persisted.
     */
    public Resource stockIn(long resourceId, int quantity, String source,
            String reason, Long disasterId)
            throws UnauthorizedOperationException,
            InvalidResourceDataException, DataAccessException {

        requireWriteRole();
        if (!ValidationUtil.isPositive(quantity)) {
            throw new InvalidResourceDataException(
                    "stock-in quantity must be greater than zero");
        }
        Resource r = requireExisting(resourceId);
        int previous = r.getAvailableQuantity();
        r.setAvailableQuantity(previous + quantity);

        StockMovement m = new StockMovement();
        m.setResourceId(resourceId);
        m.setType(StockMovementType.STOCK_IN);
        m.setQuantity(quantity);
        m.setPreviousQuantity(previous);
        m.setNewQuantity(r.getAvailableQuantity());
        m.setSource(source);
        m.setReason(reason);
        m.setDisasterId(disasterId);
        m.setMovedAt(LocalDateTime.now());
        m.setRecordedBy(session.currentUserId());
        movementDAO.save(m);

        return persistQuantity(r);
    }

    /**
     * Records resources leaving the inventory (used for shelter support,
     * victim assistance, food distribution, medical / rescue operations)
     * and decreases the quantity. Refuses to go below zero.
     */
    public Resource stockOut(long resourceId, int quantity,
            String destination, String reason, Long disasterId)
            throws UnauthorizedOperationException,
            InvalidResourceDataException, DataAccessException {

        requireWriteRole();
        if (!ValidationUtil.isPositive(quantity)) {
            throw new InvalidResourceDataException(
                    "stock-out quantity must be greater than zero");
        }
        Resource r = requireExisting(resourceId);
        requireEnoughStock(r, quantity);
        int previous = r.getAvailableQuantity();
        r.setAvailableQuantity(previous - quantity);

        StockMovement m = new StockMovement();
        m.setResourceId(resourceId);
        m.setType(StockMovementType.STOCK_OUT);
        m.setQuantity(quantity);
        m.setPreviousQuantity(previous);
        m.setNewQuantity(r.getAvailableQuantity());
        m.setDestination(destination);
        m.setReason(reason);
        m.setDisasterId(disasterId);
        m.setMovedAt(LocalDateTime.now());
        m.setRecordedBy(session.currentUserId());
        movementDAO.save(m);

        return persistQuantity(r);
    }

    /**
     * Distributes resources to a destination (shelter / victim /
     * disaster-affected area / rescue team / hospital / food-distribution
     * operation). Validates available quantity, reduces inventory,
     * records BOTH a distribution record and a stock-out movement history
     * entry (spec sections 10, 11, 13).
     */
    public ResourceDistribution distribute(long resourceId, int quantity,
            DistributionDestination destination, String distributedTo,
            Long disasterId, Long shelterId, Long victimId, String reason)
            throws UnauthorizedOperationException,
            InvalidResourceDataException, DataAccessException {

        requireWriteRole();
        if (destination == null) {
            throw new InvalidResourceDataException(
                    "a distribution destination must be selected");
        }
        if (!ValidationUtil.requireNonBlank(distributedTo)) {
            throw new InvalidResourceDataException(
                    "distribution recipient is required");
        }
        if (!ValidationUtil.isPositive(quantity)) {
            throw new InvalidResourceDataException(
                    "distribution quantity must be greater than zero");
        }
        Resource r = requireExisting(resourceId);
        requireEnoughStock(r, quantity);
        int previous = r.getAvailableQuantity();
        r.setAvailableQuantity(previous - quantity);

        // stock-out history entry
        StockMovement m = new StockMovement();
        m.setResourceId(resourceId);
        m.setType(StockMovementType.STOCK_OUT);
        m.setQuantity(quantity);
        m.setPreviousQuantity(previous);
        m.setNewQuantity(r.getAvailableQuantity());
        m.setDestination(distributedTo);
        m.setReason(reason);
        m.setDisasterId(disasterId);
        m.setMovedAt(LocalDateTime.now());
        m.setRecordedBy(session.currentUserId());
        movementDAO.save(m);

        // distribution record
        ResourceDistribution d = new ResourceDistribution();
        d.setResourceId(resourceId);
        d.setQuantity(quantity);
        d.setDestination(destination);
        d.setDistributedTo(distributedTo);
        d.setDisasterId(disasterId);
        d.setShelterId(shelterId);
        d.setVictimId(victimId);
        d.setReason(reason);
        d.setDistributedAt(LocalDateTime.now());
        d.setDistributedBy(session.currentUserId());
        ResourceDistribution saved = distributionDAO.save(d);

        persistQuantity(r);
        return saved;
    }

    /** Persists a quantity change and recomputes the availability status. */
    private Resource persistQuantity(Resource r)
            throws InvalidResourceDataException, DataAccessException {
        Resource saved = resourceDAO.save(r);
        resourceDAO.persistStatus(saved);
        return resourceDAO.findById(saved.getId());
    }

    // ── low stock detection / alerts ─────────────────────────────────

    /** Resources currently below their minimum level (quantity < min). */
    public List<Resource> getLowStockResources() throws DataAccessException {
        return resourceDAO.findBelowMinimum();
    }

    /** Both low-stock and out-of-stock resources need attention. */
    public List<Resource> getShortages() throws DataAccessException {
        return resourceDAO.findStockShortages();
    }

    /**
     * Broadcasts de-duplicated LOW_STOCK notifications to ADMIN and
     * CAMP_MANAGER accounts for every resource currently in shortage.
     * Returns the number of new alerts created (spec section 9).
     */
    public int generateLowStockAlerts()
            throws UnauthorizedOperationException, DataAccessException {
        requireWriteRole();
        List<Resource> shortages = getShortages();
        if (shortages.isEmpty()) {
            return 0;
        }
        Set<Long> audience = new LinkedHashSet<>();
        for (User user : userDAO.findByRole(RoleType.ADMIN)) {
            audience.add(user.getId());
        }
        for (User user : userDAO.findByRole(RoleType.CAMP_MANAGER)) {
            audience.add(user.getId());
        }
        if (audience.isEmpty()) {
            return 0;
        }
        int created = 0;
        for (Resource r : shortages) {
            String dedup = "RESOURCE_LOW_STOCK:" + r.getId();
            if (notificationDAO.findRecentByDedupKey(dedup,
                    ALERT_DEDUP_SECONDS) != null) {
                continue;
            }
            String message = "LOW STOCK ALERT: " + r.getName()
                    + " has only " + r.getAvailableQuantity()
                    + " " + (r.getUnit() == null ? "unit(s)"
                            : r.getUnit())
                    + " left; minimum required is "
                    + r.getMinimumLevel() + ".";
            for (Long userId : audience) {
                Notification n = new Notification();
                n.setRecipientUserId(userId);
                n.setType(NotificationType.LOW_STOCK);
                n.setPriority(NotificationPriority.WARNING);
                n.setStatus(NotificationStatus.UNREAD);
                n.setMessage(message);
                n.setRelatedModule("Resources");
                n.setRelatedEventId(r.getId());
                n.setAutoGenerated(true);
                n.setDedupKey(dedup);
                notificationDAO.save(n);
                created++;
            }
        }
        return created;
    }

    // ── queries / search / filters ───────────────────────────────────

    public Resource requireExisting(long resourceId)
            throws InvalidResourceDataException, DataAccessException {
        Resource r = resourceDAO.findById(resourceId);
        if (r == null) {
            throw new InvalidResourceDataException(
                    "No resource with id " + resourceId);
        }
        return r;
    }

    public List<Resource> getAllResources() throws DataAccessException {
        return resourceDAO.findAll();
    }

    public Resource getResource(long id) throws DataAccessException {
        return resourceDAO.findById(id);
    }

    public List<Resource> search(String keyword) throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllResources();
        }
        return resourceDAO.search(keyword.trim());
    }

    public List<Resource> filter(String keyword, ResourceCategory category,
            ResourceStatus status) throws DataAccessException {
        List<Resource> base = keyword == null || keyword.trim().isEmpty()
                ? getAllResources()
                : resourceDAO.search(keyword.trim());
        List<Resource> out = new ArrayList<>();
        for (Resource r : base) {
            if (category != null && r.getCategory() != category) {
                continue;
            }
            if (status != null && r.status() != status) {
                continue;
            }
            out.add(r);
        }
        return out;
    }

    /** ADMIN-only hard delete of an unused resource. */
    public void deleteResource(long resourceId)
            throws UnauthorizedOperationException,
            InvalidResourceDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN);
        if (!resourceDAO.deleteById(resourceId)) {
            throw new InvalidResourceDataException(
                    "No resource with id " + resourceId);
        }
    }

    // ── history ──────────────────────────────────────────────────────

    public List<StockMovement> getMovements(long resourceId)
            throws DataAccessException {
        return movementDAO.findByResource(resourceId);
    }

    public List<StockMovement> getAllMovements() throws DataAccessException {
        return movementDAO.findAll();
    }

    public List<ResourceDistribution> getDistributions(long resourceId)
            throws DataAccessException {
        return distributionDAO.findByResource(resourceId);
    }

    public List<ResourceDistribution> getAllDistributions()
            throws DataAccessException {
        return distributionDAO.findAll();
    }

    // ── statistics ───────────────────────────────────────────────────

    public int countResources() throws DataAccessException {
        return resourceDAO.findAll().size();
    }

    public long totalUnits() throws DataAccessException {
        long total = 0;
        for (Resource r : resourceDAO.findAll()) {
            total += r.getAvailableQuantity();
        }
        return total;
    }

    public int countLowStock() throws DataAccessException {
        return resourceDAO.findBelowMinimum().size();
    }

    public int countOutOfStock() throws DataAccessException {
        return resourceDAO.findByStatus(ResourceStatus.OUT_OF_STOCK).size();
    }

    public int countTotalDistributed() throws DataAccessException {
        int total = 0;
        for (ResourceDistribution d : distributionDAO.findAll()) {
            total += d.getQuantity();
        }
        return total;
    }

    // ── helpers ──────────────────────────────────────────────────────

    private void requireWriteRole() throws UnauthorizedOperationException {
        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER);
    }

    private void requireEnoughStock(Resource r, int requested)
            throws InvalidResourceDataException {
        if (r.getAvailableQuantity() < requested) {
            throw new InvalidResourceDataException(
                    "Insufficient stock: " + r.getName() + " has only "
                            + r.getAvailableQuantity() + " "
                            + (r.getUnit() == null ? "unit(s)" : r.getUnit())
                            + " available, but " + requested
                            + " were requested");
        }
    }

    private List<String> validateBasics(String name, String code,
            ResourceCategory category, int availableQuantity, int minimumLevel) {
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(name)) {
            errors.add("resource name is required");
        }
        if (!ValidationUtil.requireNonBlank(code)) {
            errors.add("resource code is required");
        }
        if (category == null) {
            errors.add("resource category must be selected");
        }
        if (!ValidationUtil.isNonNegative(availableQuantity)) {
            errors.add("available quantity cannot be negative");
        }
        if (!ValidationUtil.isNonNegative(minimumLevel)) {
            errors.add("minimum stock level cannot be negative");
        }
        return errors;
    }
}
