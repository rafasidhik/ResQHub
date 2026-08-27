package com.resqhub.test;

import java.util.List;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.ReportController;
import com.resqhub.controller.ShelterController;
import com.resqhub.model.ReportFilters;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterFacility;
import com.resqhub.model.ShelterOperationalStatus;

/**
 * ShelterTest - end-to-end integration test for the Shelter Management
 * module. Drives registration, validation (incl. overcapacity
 * prevention), facilities, victim/family allocation + release, capacity
 * monitoring and the Shelter Occupancy report, all through the
 * controller layer exactly as the Swing screens do.
 */
public class ShelterTest {

    private static int passed = 0;
    private static int failed = 0;
    private static long testShelterId = -1;
    private static long testAllocationId = -1;

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            System.out.println("[FATAL] " + e);
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        System.out.println("ShelterTest: " + passed + " passed, "
                + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void run() {
        AuthController auth = new AuthController();
        check("admin logs in",
                () -> auth.login("admin", "Admin@123").isSuccess());

        ShelterController c = new ShelterController();

        // ── registration + validation ────────────────────────────────
        check("register a shelter", () -> {
            ActionResult r = c.createShelter("Test Shelter Z", "SHL-T1",
                    "Kerala", "Kochi", null, null, "10", "5", null, null,
                    "1", true, false, true, false,
                    ShelterOperationalStatus.AVAILABLE);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            testShelterId = r.<Shelter>getData().getId();
            return true;
        });

        check("blank name rejected", () -> !c.createShelter("", "SHL-T2",
                "Kerala", null, null, null, "10", "0", null, null, null,
                false, false, false, false,
                ShelterOperationalStatus.AVAILABLE).isSuccess());

        check("occupancy above capacity rejected", () ->
                !c.createShelter("Bad", "SHL-T3", "Kerala", null, null, null,
                        "5", "9", null, null, null,
                        false, false, false, false,
                        ShelterOperationalStatus.AVAILABLE).isSuccess());

        check("duplicate code rejected", () ->
                !c.createShelter("Dup", "SHL-T1", "Kerala", null, null, null,
                        "10", "0", null, null, null,
                        false, false, false, false,
                        ShelterOperationalStatus.AVAILABLE).isSuccess());

        check("shelter found in list", () -> {
            List<Shelter> all = c.getAllShelters();
            return all.stream().anyMatch(s -> s.getId().equals(testShelterId));
        });

        // ── facilities ───────────────────────────────────────────────
        check("add facility", () -> c.addFacility(testShelterId,
                "Drinking Water", true).isSuccess());
        check("duplicate facility rejected", () -> !c.addFacility(
                testShelterId, "Drinking Water", true).isSuccess());
        check("facilities listed", () -> {
            List<ShelterFacility> f = c.getFacilities(testShelterId);
            return f.stream().anyMatch(x -> "Drinking Water"
                    .equals(x.getFacilityName()));
        });

        // ── allocation + overcapacity prevention ─────────────────────
        // test shelter: max 10, occ 5 -> 5 available.
        check("allocate 3 (fits)", () -> {
            ActionResult r = c.allocate(testShelterId, null, "Family X", "3",
                    "test");
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            testAllocationId = r.<ShelterAllocation>getData().getId();
            return true;
        });

        check("allocate 2 more (fits, now full)", () ->
                c.allocate(testShelterId, null, "Family Y", "2", null)
                        .isSuccess());

        check("overcapacity allocation blocked", () -> !c.allocate(
                testShelterId, null, "Too Big", "3", null).isSuccess());

        check("allocations listed", () -> {
            List<ShelterAllocation> a = c.getAllocations(testShelterId);
            return !a.isEmpty()
                    && a.stream().anyMatch(x -> x.getId()
                            .equals(testAllocationId));
        });

        check("allocated shelter now full", () -> {
            Shelter s = c.getAllShelters().stream()
                    .filter(x -> x.getId().equals(testShelterId))
                    .findFirst().orElse(null);
            return s != null
                    && s.getOperationalStatus() == ShelterOperationalStatus.FULL
                    && s.availableCapacity() == 0;
        });

        // ── release ──────────────────────────────────────────────────
        check("release allocation", () ->
                c.release(testAllocationId).isSuccess());
        check("release is idempotent-guarded", () ->
                !c.release(testAllocationId).isSuccess());

        check("occupancy decreased after release", () -> {
            Shelter s = c.getAllShelters().stream()
                    .filter(x -> x.getId().equals(testShelterId))
                    .findFirst().orElse(null);
            return s != null && s.availableCapacity() == 3;
        });

        // ── capacity monitoring + reports integration ────────────────
        check("seeded near-capacity shelters detected", () -> {
            List<Shelter> near = c.search("")
                    .stream().filter(Shelter::isNearCapacity).toList();
            return near.stream().anyMatch(x -> "Relief Camp A"
                    .equals(x.getName()));
        });

        check("shelter occupancy report generates", () -> {
            ReportController rc = new ReportController();
            ReportResult r = rc.generateReport(ReportType.SHELTER_OCCUPANCY,
                    ReportFilters.empty()).getData();
            return r != null && r.rows() != null && !r.rows().isEmpty();
        });

        // ── cleanup ──────────────────────────────────────────────────
        check("delete test shelter (cascades children)", () -> {
            new com.resqhub.service.ShelterService()
                    .deleteShelter(testShelterId);
            return true;
        });
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

    private static void assertNotNull(Object o) {
        if (o == null) {
            throw new AssertionError("expected non-null");
        }
    }
}
