package com.resqhub.test;

import java.util.List;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.ReportController;
import com.resqhub.controller.ResourceController;
import com.resqhub.model.DistributionDestination;
import com.resqhub.model.ReportFilters;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;
import com.resqhub.model.Resource;
import com.resqhub.model.ResourceCategory;
import com.resqhub.model.ResourceStatus;
import com.resqhub.model.ResourceDistribution;
import com.resqhub.model.StockMovement;
import com.resqhub.model.StockMovementType;

/**
 * Resource &amp; Inventory management tests - covers registration,
 * duplicate-code rejection, stock-in / stock-out, distribution with the
 * negative-inventory guard, automatic availability-status derivation,
 * low-stock alert generation, search / filter / update and the
 * RESOURCE_INVENTORY report. Runs through the controller layer (as the
 * Swing screens do) against the live DB, creating + deleting its own
 * entities.
 */
public class ResourceTest {

    private static int passed = 0;
    private static int failed = 0;

    private static final String SUFFIX = String.valueOf(System.nanoTime());
    private static long bandagesId = -1;
    private static long syringeId = -1;
    private static long gauzeId = -1;

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            System.out.println("[FATAL] " + e);
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        System.out.println("ResourceTest: " + passed + " passed, "
                + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void run() {
        AuthController auth = new AuthController();
        check("admin logs in",
                () -> auth.login("admin", "Admin@123").isSuccess());

        ResourceController c = new ResourceController();

        // ---- registration + validation ---------------------------------
        String bCode = "TEST-BAND-" + SUFFIX;
        check("register a resource (available)", () -> {
            ActionResult r = c.createResource("Test Bandages", bCode,
                    ResourceCategory.MEDICAL_SUPPLIES, "50", "10",
                    "boxes", "test bandages");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            Resource res = r.getData();
            bandagesId = res.getId();
            return res.getAvailableQuantity() == 50
                    && res.status() == ResourceStatus.AVAILABLE;
        });

        check("duplicate resource code rejected", () -> !c.createResource(
                "Dupe Bandages", bCode, ResourceCategory.MEDICAL_SUPPLIES,
                "1", "1", null, null).isSuccess());

        check("missing name rejected", () -> !c.createResource("   ",
                "TEST-X-" + SUFFIX, ResourceCategory.MEDICAL_SUPPLIES,
                "1", "1", null, null).isSuccess());

        check("negative quantity rejected", () -> !c.createResource(
                "Neg Stock", "TEST-NEG-" + SUFFIX,
                ResourceCategory.MEDICAL_SUPPLIES, "-5", "1",
                null, null).isSuccess());

        check("register a low-stock resource", () -> {
            ActionResult r = c.createResource("Test Syringe", "TEST-SYR-"
                    + SUFFIX, ResourceCategory.MEDICAL_SUPPLIES, "5", "20",
                    "pcs", null);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            syringeId = r.<Resource>getData().getId();
            return r.<Resource>getData().status()
                    == ResourceStatus.LOW_STOCK;
        });

        check("register an out-of-stock resource", () -> {
            ActionResult r = c.createResource("Test Gauze", "TEST-GAU-"
                    + SUFFIX, ResourceCategory.MEDICAL_SUPPLIES, "0", "10",
                    "rolls", null);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            gauzeId = r.<Resource>getData().getId();
            return r.<Resource>getData().status()
                    == ResourceStatus.OUT_OF_STOCK;
        });

        // ---- stock-in / stock-out with movement history -----------------
        check("stock-in increases quantity + records movement", () -> {
            ActionResult r = c.stockIn(bandagesId, "100", "Donation",
                    "test stock in", null);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            Resource res = find(c, bandagesId);
            if (res.getAvailableQuantity() != 150) {
                return false;
            }
            List<StockMovement> moves = c.getMovements(bandagesId);
            return moves.stream().anyMatch(m ->
                    m.getType() == StockMovementType.STOCK_IN
                            && m.getQuantity() == 100
                            && m.getPreviousQuantity() == 50
                            && m.getNewQuantity() == 150);
        });

        check("stock-out decreases quantity + records movement", () -> {
            ActionResult r = c.stockOut(bandagesId, "30", "Relief Camp A",
                    "test stock out", null);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            Resource res = find(c, bandagesId);
            if (res.getAvailableQuantity() != 120) {
                return false;
            }
            return c.getMovements(bandagesId).stream().anyMatch(m ->
                    m.getType() == StockMovementType.STOCK_OUT
                            && m.getQuantity() == 30
                            && m.getPreviousQuantity() == 150
                            && m.getNewQuantity() == 120);
        });

        check("stock-out beyond available refused", () -> !c.stockOut(
                bandagesId, "99999", "X", null, null).isSuccess());

        // ---- distribution (record + history + negative guard) -----------
        check("distribute reduces inventory + creates both records", () -> {
            ActionResult r = c.distribute(bandagesId, "20",
                    DistributionDestination.VICTIM, "Test Family",
                    null, null, null, "test distribution");
            check("distribution succeeds", r::isSuccess);
            Resource res = find(c, bandagesId);
            if (res.getAvailableQuantity() != 100) {
                return false;
            }
            List<ResourceDistribution> ds = c.getDistributions(bandagesId);
            boolean hasDistribution = ds.stream().anyMatch(d ->
                    d.getQuantity() == 20
                            && d.getDestination() == DistributionDestination.VICTIM
                            && "Test Family".equals(d.getDistributedTo()));
            boolean hasMovement = c.getMovements(bandagesId).stream()
                    .anyMatch(m ->
                            m.getType() == StockMovementType.STOCK_OUT
                                    && m.getQuantity() == 20
                                    && m.getNewQuantity() == 100);
            return hasDistribution && hasMovement;
        });

        check("distribute more than available refused", () -> !c.distribute(
                bandagesId, "200", DistributionDestination.SHELTER,
                "Camp B", null, "1", null, null).isSuccess());

        check("distribution requires a recipient", () -> !c.distribute(
                bandagesId, "5", DistributionDestination.SHELTER, " ",
                null, null, null, null).isSuccess());

        check("distribution requires a destination", () -> !c.distribute(
                bandagesId, "5", null, "Someone", null, null, null,
                null).isSuccess());

        // ---- low-stock / out-of-stock detection -------------------------
        check("low-stock listing contains test syringe", () ->
                c.getLowStock().stream().anyMatch(r ->
                        r.getId().equals(syringeId)));

        check("shortages listing contains test syringe + gauze", () -> {
            boolean hasSyringe = c.getShortages().stream().anyMatch(r ->
                    r.getId().equals(syringeId));
            boolean hasGauze = c.getShortages().stream().anyMatch(r ->
                    r.getId().equals(gauzeId));
            return hasSyringe && hasGauze;
        });

        // ---- search / filter / update -----------------------------------
        check("search by name finds bandages", () ->
                c.search("Bandages").stream().anyMatch(r ->
                        r.getId().equals(bandagesId)));

        check("filter by category narrows results", () -> {
            List<Resource> all = c.filter(null, ResourceCategory.MEDICAL_SUPPLIES,
                    null);
            return all.stream().allMatch(r ->
                    r.getCategory() == ResourceCategory.MEDICAL_SUPPLIES)
                    && all.stream().anyMatch(r -> r.getId().equals(bandagesId));
        });

        check("update raises minimum level and flips status", () -> {
            ActionResult r = c.updateResource(syringeId, "Test Syringe",
                    ResourceCategory.MEDICAL_SUPPLIES, "5", "pcs", null);
            check("update succeeds", r::isSuccess);
            Resource res = find(c, syringeId);
            return res.getMinimumLevel() == 5
                    && res.status() == ResourceStatus.AVAILABLE;
        });

        // ---- low-stock alert generation ---------------------------------
        check("generateLowStockAlerts runs (dedup-aware)", () ->
                c.generateLowStockAlerts().isSuccess());

        // ---- reports integration ----------------------------------------
        check("resource inventory report generates", () -> {
            ReportController rc = new ReportController();
            ReportResult r = rc.generateReport(ReportType.RESOURCE_INVENTORY,
                    ReportFilters.empty()).getData();
            return r != null
                    && "Resource & Inventory Report".equals(r.title());
        });

        // ---- cleanup -----------------------------------------------------
        check("cleanup: delete test resources", () -> {
            c.deleteResource(bandagesId);
            c.deleteResource(syringeId);
            c.deleteResource(gauzeId);
            return true;
        });
    }

    private static Resource find(ResourceController c, long id)
            throws Exception {
        for (Resource r : c.getAllResources()) {
            if (r.getId().equals(id)) {
                return r;
            }
        }
        return null;
    }

    private static void check(String name, Check c) {
        try {
            if (c.run()) {
                passed++;
                System.out.println("[PASS] " + name);
            } else {
                failed++;
                System.out.println("[FAIL] " + name);
            }
        } catch (Exception e) {
            failed++;
            System.out.println("[FAIL] " + name + " -> " + e);
        }
    }

    private interface Check {
        boolean run() throws Exception;
    }
}
