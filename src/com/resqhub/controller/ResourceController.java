package com.resqhub.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.resqhub.dao.DisasterDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.Disaster;
import com.resqhub.model.DistributionDestination;
import com.resqhub.model.Resource;
import com.resqhub.model.ResourceCategory;
import com.resqhub.model.ResourceDistribution;
import com.resqhub.model.ResourceStatus;
import com.resqhub.model.StockMovement;
import com.resqhub.service.ResourceService;
import com.resqhub.util.InputParser;

/** Resource &amp; Inventory screen controller: UI input -> typed service calls. */
public class ResourceController {

    private final ResourceService resourceService = new ResourceService();
    private final DisasterDAO disasterDAO = new DisasterDAO();

    public ActionResult createResource(String name, String code,
            ResourceCategory category, String availableText,
            String minLevelText, String unit, String description) {
        try {
            int available = InputParser.parseInt(availableText,
                    "Available quantity");
            int minLevel = minLevelText == null || minLevelText.trim().isEmpty()
                    ? 0 : InputParser.parseInt(minLevelText, "Minimum level");
            Resource r = resourceService.createResource(name, code, category,
                    available, minLevel, unit, description);
            return ActionResult.successWithData(
                    "Resource registered: " + r.getName() + " (" + r.getCode()
                            + ") with " + r.getAvailableQuantity()
                            + " " + (r.getUnit() == null ? "unit(s)"
                                    : r.getUnit()) + " in stock",
                    r);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateResource(long id, String name,
            ResourceCategory category, String minLevelText, String unit,
            String description) {
        try {
            int minLevel = minLevelText == null || minLevelText.trim().isEmpty()
                    ? 0 : InputParser.parseInt(minLevelText, "Minimum level");
            Resource r = resourceService.updateResource(id, name, category,
                    minLevel, unit, description);
            return ActionResult.success("Resource " + r.getName()
                    + " updated (minimum level " + r.getMinimumLevel() + ")");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult stockIn(long resourceId, String quantityText,
            String source, String reason, String disasterIdText) {
        try {
            int qty = InputParser.parseInt(quantityText, "Quantity");
            Long disasterId = parseOptionalId(disasterIdText);
            Resource r = resourceService.stockIn(resourceId, qty, source,
                    reason, disasterId);
            return ActionResult.success("Received +" + qty + " "
                    + (r.getUnit() == null ? "unit(s)" : r.getUnit()) + " of "
                    + r.getName() + ". New stock: " + r.getAvailableQuantity());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult stockOut(long resourceId, String quantityText,
            String destination, String reason, String disasterIdText) {
        try {
            int qty = InputParser.parseInt(quantityText, "Quantity");
            Long disasterId = parseOptionalId(disasterIdText);
            Resource r = resourceService.stockOut(resourceId, qty, destination,
                    reason, disasterId);
            return ActionResult.success("Distributed -" + qty + " "
                    + (r.getUnit() == null ? "unit(s)" : r.getUnit()) + " of "
                    + r.getName() + ". New stock: " + r.getAvailableQuantity());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult distribute(long resourceId, String quantityText,
            DistributionDestination destination, String distributedTo,
            String disasterIdText, String shelterIdText, String victimIdText,
            String reason) {
        try {
            int qty = InputParser.parseInt(quantityText, "Quantity");
            Long disasterId = parseOptionalId(disasterIdText);
            Long shelterId = parseOptionalId(shelterIdText);
            Long victimId = parseOptionalId(victimIdText);
            ResourceDistribution d = resourceService.distribute(resourceId,
                    qty, destination, distributedTo, disasterId, shelterId,
                    victimId, reason);
            return ActionResult.success("Distributed " + qty + " to "
                    + d.getDistributedTo() + " ("
                    + d.getDestination().getLabel() + "). Inventory reduced.");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult generateLowStockAlerts() {
        try {
            int created = resourceService.generateLowStockAlerts();
            return ActionResult.success(created == 0
                    ? "No new low-stock alerts (stock is adequate or alerts "
                            + "already raised)"
                    : "Generated " + created + " low-stock alert(s)");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ── reads ────────────────────────────────────────────────────────

    public List<Resource> getAllResources() throws DataAccessException {
        return resourceService.getAllResources();
    }

    public List<Resource> getLowStock() throws DataAccessException {
        return resourceService.getLowStockResources();
    }

    public List<Resource> getShortages() throws DataAccessException {
        return resourceService.getShortages();
    }

    public List<Resource> search(String keyword) throws DataAccessException {
        return resourceService.search(keyword);
    }

    public List<Resource> filter(String keyword, ResourceCategory category,
            ResourceStatus status) throws DataAccessException {
        return resourceService.filter(keyword, category, status);
    }

    public List<StockMovement> getMovements(long resourceId)
            throws DataAccessException {
        return resourceService.getMovements(resourceId);
    }

    public List<StockMovement> getAllMovements() throws DataAccessException {
        return resourceService.getAllMovements();
    }

    public List<ResourceDistribution> getDistributions(long resourceId)
            throws DataAccessException {
        return resourceService.getDistributions(resourceId);
    }

    public List<ResourceDistribution> getAllDistributions()
            throws DataAccessException {
        return resourceService.getAllDistributions();
    }

    public int countResources() throws DataAccessException {
        return resourceService.countResources();
    }

    public long totalUnits() throws DataAccessException {
        return resourceService.totalUnits();
    }

    public int countLowStock() throws DataAccessException {
        return resourceService.countLowStock();
    }

    public int countDistributed() throws DataAccessException {
        return resourceService.countTotalDistributed();
    }

    public List<Disaster> getDisasters() throws DataAccessException {
        return disasterDAO.findAll();
    }

    public ActionResult deleteResource(long resourceId) {
        try {
            resourceService.deleteResource(resourceId);
            return ActionResult.success("Resource #" + resourceId
                    + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
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

    public static Object[] resourceRow(Resource r) {
        return new Object[]{
                r.getId(),
                r.getCode(),
                r.getName(),
                r.getCategory() == null ? "-" : r.getCategory().getLabel(),
                r.getAvailableQuantity(),
                r.getMinimumLevel(),
                r.getUnit() == null ? "-" : r.getUnit(),
                r.status().getLabel()
        };
    }

    public static String[] resourceHeaders() {
        return new String[]{"ID", "Code", "Name", "Category", "Available",
                "Min Level", "Unit", "Status"};
    }

    public static Object[] movementRow(StockMovement m, String resourceName) {
        return new Object[]{
                m.getId(),
                resourceName == null ? "#" + m.getResourceId() : resourceName,
                m.getType() == null ? "-" : m.getType().getLabel(),
                m.getQuantity(),
                m.getPreviousQuantity(),
                m.getNewQuantity(),
                m.getSource(),
                m.getDestination(),
                m.getReason(),
                m.getMovedAt() == null ? "-"
                        : m.getMovedAt().toString().replace('T', ' ')
        };
    }

    public static String[] movementHeaders() {
        return new String[]{"ID", "Resource", "Type", "Qty", "Before",
                "After", "Source", "Destination", "Reason", "Date"};
    }

    public static Object[] distributionRow(ResourceDistribution d,
            String resourceName) {
        return new Object[]{
                d.getId(),
                resourceName == null ? "#" + d.getResourceId() : resourceName,
                d.getQuantity(),
                d.getDestination() == null ? "-"
                        : d.getDestination().getLabel(),
                d.getDistributedTo(),
                d.getReason(),
                d.getDistributedAt() == null ? "-"
                        : d.getDistributedAt().toString().replace('T', ' ')
        };
    }

    public static String[] distributionHeaders() {
        return new String[]{"ID", "Resource", "Qty", "Destination",
                "Distributed To", "Reason", "Date"};
    }

    /** Builds a resourceId -> name lookup for history tables. */
    public Map<Long, String> resourceNameMap() throws DataAccessException {
        Map<Long, String> map = new HashMap<>();
        for (Resource r : getAllResources()) {
            map.put(r.getId(), r.getName());
        }
        return map;
    }

    /** A convenient filtered history of all movements. */
    public List<Object[]> allMovementRows() throws DataAccessException {
        Map<Long, String> names = resourceNameMap();
        List<Object[]> rows = new ArrayList<>();
        for (StockMovement m : getAllMovements()) {
            rows.add(movementRow(m, names.get(m.getResourceId())));
        }
        return rows;
    }

    public List<Object[]> allDistributionRows() throws DataAccessException {
        Map<Long, String> names = resourceNameMap();
        List<Object[]> rows = new ArrayList<>();
        for (ResourceDistribution d : getAllDistributions()) {
            rows.add(distributionRow(d, names.get(d.getResourceId())));
        }
        return rows;
    }
}
