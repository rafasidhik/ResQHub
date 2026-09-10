package com.resqhub.test;

import java.util.List;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.FoodDistributionController;
import com.resqhub.controller.ReportController;
import com.resqhub.controller.ResourceController;
import com.resqhub.dao.FoodDistributionRequestDAO;
import com.resqhub.model.BeneficiaryType;
import com.resqhub.model.FoodDistribution;
import com.resqhub.model.FoodDistributionRequest;
import com.resqhub.model.FoodRequestStatus;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.ReportFilters;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;
import com.resqhub.model.Resource;
import com.resqhub.model.ResourceCategory;

/**
 * Food Distribution Management tests - covers request creation with both
 * explicit and calculated requirements, duplicate / missing-field rejection,
 * the approve -> allocate lifecycle with the stock-availability guard and
 * partial fulfilment, volunteer assignment, distribution completion that
 * reduces food inventory and records history, the shortage-alert generator,
 * search / filter, shelter-integration requirement calculation and the
 * FOOD_DISTRIBUTION report. Runs through the controller layer against the
 * live DB, creating + deleting its own entities.
 */
public class FoodDistributionTest {

    private static int passed = 0;
    private static int failed = 0;

    private static final String SUFFIX = String.valueOf(System.nanoTime());
    private static long foodResourceId = -1;
    private static long lowFoodResourceId = -1;
    private static long reqA = -1;
    private static long reqB = -1;
    private static long reqC = -1;
    private static long reqDup = -1;
    private static final FoodDistributionRequestDAO reqDAO =
            new FoodDistributionRequestDAO();

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            System.out.println("[FATAL] " + e);
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        System.out.println("FoodDistributionTest: " + passed + " passed, "
                + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void run() {
        AuthController auth = new AuthController();
        check("admin logs in",
                () -> auth.login("admin", "Admin@123").isSuccess());

        FoodDistributionController c = new FoodDistributionController();
        ResourceController rc = new ResourceController();

        // ---- setup: dedicated FOOD resources ---------------------------
        String foodCode = "TEST-FOOD-" + SUFFIX;
        check("register a FOOD resource with stock", () -> {
            ActionResult r = rc.createResource("Test Food Meals", foodCode,
                    ResourceCategory.FOOD, "1000", "100", "meals",
                    "test food resource");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            Resource res = r.getData();
            foodResourceId = res.getId();
            return res.getAvailableQuantity() == 1000;
        });

        String lowCode = "TEST-FOOD-LOW-" + SUFFIX;
        check("register a low-stock FOOD resource", () -> {
            ActionResult r = rc.createResource("Test Food Low", lowCode,
                    ResourceCategory.FOOD, "30", "5", "meals",
                    "test low-stock food");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            Resource low = r.getData();
            lowFoodResourceId = low.getId();
            return true;
        });

        // ---- creation + validation -------------------------------------
        String aCode = "FDA-" + SUFFIX;
        check("create request with explicit quantity", () -> {
            ActionResult r = c.createRequest(aCode, "1", "Relief Camp X",
                    BeneficiaryType.SHELTER, "250", "500",
                    PriorityLevel.HIGH, "explicit demand");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            FoodDistributionRequest fr = r.getData();
            reqA = fr.getId();
            return fr.getBeneficiaries() == 250
                    && fr.getRequiredQuantity() == 500
                    && fr.getStatus() == FoodRequestStatus.PENDING
                    && fr.getPriority() == PriorityLevel.HIGH;
        });

        String bCode = "FDB-" + SUFFIX;
        check("create request with calculated requirement (250 x 3 = 750)", () -> {
            ActionResult r = c.createRequestWithCalculation(bCode, "1",
                    "Relief Camp Y", BeneficiaryType.FAMILY, "250", "3",
                    PriorityLevel.MEDIUM, "calc demand");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            FoodDistributionRequest fr = r.getData();
            reqB = fr.getId();
            return fr.getRequiredQuantity() == 750
                    && fr.getBeneficiaries() == 250
                    && fr.getStatus() == FoodRequestStatus.PENDING;
        });

        check("duplicate request code rejected", () -> {
            ActionResult r = c.createRequest(aCode, "1", "Relief Camp Z",
                    BeneficiaryType.GROUP, "10", "20", PriorityLevel.LOW,
                    null);
            return !r.isSuccess();
        });

        check("missing location rejected", () -> !c.createRequest(
                "FDC-" + SUFFIX, "1", "   ", BeneficiaryType.GROUP,
                "10", "20", PriorityLevel.LOW, null).isSuccess());

        check("zero-beneficiary rejected", () -> !c.createRequest(
                "FDD-" + SUFFIX, "1", "Relief Camp A", BeneficiaryType.GROUP,
                "0", "20", PriorityLevel.LOW, null).isSuccess());

        // ---- approve -> allocate lifecycle -----------------------------
        check("approve PENDING request", () -> {
            ActionResult r = c.approveRequest(reqA);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            return c.getRequest(reqA).getStatus()
                    == FoodRequestStatus.APPROVED;
        });

        check("re-approving an APPROVED request is rejected", () ->
                !c.approveRequest(reqA).isSuccess());

        check("allocate FOOD resource (full 500 of 500)", () -> {
            ActionResult r = c.allocateRequest(reqA,
                    String.valueOf(foodResourceId), "500");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            FoodDistributionRequest fr = c.getRequest(reqA);
            return fr.getStatus() == FoodRequestStatus.ALLOCATED
                    && fr.getAllocatedQuantity() == 500;
        });

        check("non-FOOD resource cannot be allocated", () -> !c
                .allocateRequest(reqA, "2", "5").isSuccess());

        check("allocate partial (400 of 750) -> PARTIALLY_FULFILLED", () -> {
            ActionResult r = c.allocateRequest(reqB,
                    String.valueOf(foodResourceId), "400");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            FoodDistributionRequest fr = c.getRequest(reqB);
            return fr.getStatus()
                    == FoodRequestStatus.PARTIALLY_FULFILLED
                    && fr.getAllocatedQuantity() == 400;
        });

        check("allocate beyond available stock is refused", () -> !c
                .allocateRequest(reqB, String.valueOf(lowFoodResourceId),
                        "100").isSuccess());

        // ---- volunteer assignment --------------------------------------
        check("assign a volunteer to a fully-allocated request", () -> {
            ActionResult r = c.assignVolunteer(reqA, "1");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            FoodDistributionRequest fr = c.getRequest(reqA);
            return fr.getAssignedVolunteerId() != null
                    && fr.getStatus() == FoodRequestStatus.ALLOCATED;
        });

        check("assign a volunteer to a PARTIALLY_FULFILLED request", () -> {
            ActionResult r = c.assignVolunteer(reqB, "2");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            FoodDistributionRequest fr = c.getRequest(reqB);
            return fr.getAssignedVolunteerId() != null
                    && fr.getStatus()
                            == FoodRequestStatus.PARTIALLY_FULFILLED;
        });

        // ---- distribution + completion ---------------------------------
        check("distribute 300 to request A -> IN_PROGRESS + reduces stock", () -> {
            int before = findResource(rc, foodResourceId)
                    .getAvailableQuantity();
            ActionResult r = c.recordDistribution(reqA,
                    String.valueOf(foodResourceId), "300", "150",
                    "Relief Camp X", "batch one");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            int after = findResource(rc, foodResourceId)
                    .getAvailableQuantity();
            FoodDistributionRequest fr = c.getRequest(reqA);
            return after == before - 300
                    && fr.getStatus() == FoodRequestStatus.IN_PROGRESS;
        });

        check("distribution history recorded for request A", () ->
                c.getDistributions(reqA).size() == 1);

        check("distribute 300 to request B -> IN_PROGRESS", () -> {
            ActionResult r = c.recordDistribution(reqB,
                    String.valueOf(foodResourceId), "300", "150",
                    "Relief Camp Y", "batch one");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            return c.getRequest(reqB).getStatus()
                    == FoodRequestStatus.IN_PROGRESS;
        });

        check("distribute exceeding allocation is refused", () -> !c
                .recordDistribution(reqB, String.valueOf(foodResourceId),
                        "200", "0", "Relief Camp Y", "too much").isSuccess());

        check("distribute remaining 200 to request A -> COMPLETED", () -> {
            ActionResult r = c.recordDistribution(reqA,
                    String.valueOf(foodResourceId), "200", "100",
                    "Relief Camp X", "batch two");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            return c.getRequest(reqA).getStatus()
                    == FoodRequestStatus.COMPLETED;
        });

        check("distribute after COMPLETED is refused", () -> !c
                .recordDistribution(reqA, String.valueOf(foodResourceId),
                        "10", "0", "Relief Camp X", "late").isSuccess());

        check("status count: completed increments", () ->
                c.countCompleted() >= 1);

        // ---- cancellation ----------------------------------------------
        String cancelCode = "FDX-" + SUFFIX;
        check("create + cancel a request", () -> {
            ActionResult r = c.createRequest(cancelCode, null, "Outpost",
                    BeneficiaryType.GROUP, "50", "100", PriorityLevel.LOW,
                    null);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            FoodDistributionRequest cancelReq = r.getData();
            long id = cancelReq.getId();
            reqC = id;
            ActionResult cr = c.cancelRequest(id);
            if (!cr.isSuccess()) {
                throw new AssertionError(cr.getMessage());
            }
            return c.getRequest(id).getStatus()
                    == FoodRequestStatus.CANCELLED;
        });

        check("allocate a CANCELLED request is refused", () -> !c
                .allocateRequest(reqC, String.valueOf(foodResourceId), "5")
                .isSuccess());

        // ---- shortage alerts -------------------------------------------
        check("generateFoodShortageAlerts runs (dedup-aware)", () ->
                c.generateFoodShortageAlerts().isSuccess());

        // ---- search / filter -------------------------------------------
        check("search by location finds request B", () ->
                c.search("Relief Camp Y").stream()
                        .anyMatch(r -> r.getId().equals(reqB)));

        check("filter by status COMPLETED includes request A", () ->
                c.filter("", null, "", FoodRequestStatus.COMPLETED, null)
                        .stream().anyMatch(r -> r.getId().equals(reqA)));

        check("filter by disaster 1 returns the test requests", () ->
                c.filter("", "1", "", null, null).stream()
                        .anyMatch(r -> r.getId().equals(reqA) || r
                                .getId().equals(reqB)));

        // ---- shelter integration ---------------------------------------
        check("shelter requirement calculation", () -> {
            List<com.resqhub.model.Shelter> shelters = c.getAllShelters();
            if (shelters.isEmpty()) {
                return true; // nothing to verify against
            }
            for (com.resqhub.model.Shelter s : shelters) {
                if (s.getCurrentOccupancy() > 0) {
                    int req = c.requirementForShelter(s.getId(), 2);
                    return req == s.getCurrentOccupancy() * 2;
                }
            }
            return true;
        });

        check("createRequestFromShelter creates a request", () -> {
            List<com.resqhub.model.Shelter> shelters = c.getAllShelters();
            Shell: {
                for (com.resqhub.model.Shelter s : shelters) {
                    if (s.getCurrentOccupancy() > 0) {
                        ActionResult r = c.createRequestFromShelter(s.getId(),
                                "2", PriorityLevel.HIGH);
                        if (!r.isSuccess()) {
                            throw new AssertionError(r.getMessage());
                        }
                        FoodDistributionRequest fr = r.getData();
                        reqDup = fr.getId();
                        return fr.getRequiredQuantity()
                                == s.getCurrentOccupancy() * 2
                                && fr.getBeneficiaries()
                                        == s.getCurrentOccupancy();
                    }
                }
            }
            return true; // no shelter with occupants - nothing to assert
        });

        // ---- reports integration ---------------------------------------
        check("FOOD_DISTRIBUTION report generates", () -> {
            ReportController rc2 = new ReportController();
            ReportResult r = rc2.generateReport(ReportType.FOOD_DISTRIBUTION,
                    ReportFilters.empty()).getData();
            return r != null
                    && "Food Distribution Report".equals(r.title());
        });

        // ---- cleanup ----------------------------------------------------
        check("cleanup: delete test requests + resources", () -> {
            delete(reqA);
            delete(reqB);
            delete(reqC);
            delete(reqDup);
            rc.deleteResource(foodResourceId);
            rc.deleteResource(lowFoodResourceId);
            return true;
        });
    }

    private static void delete(long id) {
        if (id > 0) {
            try {
                reqDAO.deleteById(id);
            } catch (Exception ignored) {
                // already gone
            }
        }
    }

    private static Resource findResource(ResourceController rc, long id)
            throws Exception {
        for (Resource r : rc.getAllResources()) {
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
