package com.resqhub.test;

import java.util.List;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.ReportController;
import com.resqhub.controller.SmartAllocationController;
import com.resqhub.controller.ShelterController;
import com.resqhub.controller.VictimController;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.Gender;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterAllocationStatus;
import com.resqhub.model.ShelterOperationalStatus;
import com.resqhub.model.SmartAllocationResult;
import com.resqhub.model.Victim;

/**
 * SmartAllocationTest - end-to-end coverage of the Smart Shelter
 * Allocation engine and the allocation lifecycle. Runs through the
 * controller layer (as the Swing screens do) against the live DB.
 */
public class SmartAllocationTest {

    private static int passed = 0;
    private static int failed = 0;

    // test-owned entities
    private static long victimId = -1;
    private static long selfShelterId = -1;
    private static long victimAllocId = -1;
    private static long pendingAllocId = -1;
    private static long activeAllocId = -1;

    public static void main(String[] args) {
        try {
            run();
        } catch (Exception e) {
            System.out.println("[FATAL] " + e);
            e.printStackTrace();
            failed++;
        }
        System.out.println();
        System.out.println("SmartAllocationTest: " + passed + " passed, "
                + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    static void run() {
        AuthController auth = new AuthController();
        check("admin logs in",
                () -> auth.login("admin", "Admin@123").isSuccess());

        SmartAllocationController c = new SmartAllocationController();

        // ── fixture: a dedicated victim + a controlled shelter ────────
        VictimController vc = new VictimController();
        check("register a test victim", () -> {
            ActionResult r = vc.registerVictim("Smart Test Victim", "30",
                    Gender.MALE, null, EmergencyStatus.CRITICAL, "none",
                    null, "Malappuram", 1L);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            victimId = r.<Victim>getData().getId();
            return true;
        });
        check("register a controlled test shelter", () -> {
            ShelterController sc = new ShelterController();
            ActionResult r = sc.createShelter("Smart Test Camp", "SMT-1",
                    "Malappuram", null, null, null, "10", "0", null, null,
                    "1", true, true, true, true,
                    ShelterOperationalStatus.AVAILABLE);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            selfShelterId = r.<Shelter>getData().getId();
            return true;
        });

        // ── smart selection + occupancy + victim flag ────────────────
        int beforeTotal = totalOcc(c);
        check("smart allocate picks a best match and opens space", () -> {
            ActionResult r = c.allocate(String.valueOf(victimId),
                    null, "2", PriorityLevel.CRITICAL, "Malappuram",
                    List.of("Drinking Water", "Medical Support"),
                    false, false, false, false, null, false);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            SmartAllocationResult result = r.getData();
            if (result.getAllocation() == null) {
                throw new AssertionError("no allocation created");
            }
            victimAllocId = result.getAllocation().getId();
            return totalOcc(c) == beforeTotal + 2;
        });

        check("victim marked IN_SHELTER", () -> {
            for (Victim v : c.getVictims()) {
                if (v.getId().equals(victimId)) {
                    return v.getShelterStatus()
                            == com.resqhub.model.ShelterStatus.IN_SHELTER;
                }
            }
            return false;
        });

        check("duplicate active allocation prevented", () -> !c.allocate(
                String.valueOf(victimId), null, "1",
                PriorityLevel.MEDIUM, "Malappuram", null,
                false, false, false, false, null, false).isSuccess());

        // ── suitability filtering / rejection ─────────────────────────
        check("family larger than any shelter rejected", () -> !c.allocate(
                null, "Huge Family", "5000", null, null, null,
                false, false, false, false, null, false).isSuccess());

        check("unmet facility requirement rejected", () -> !c.allocate(
                null, "Pool Family", "2", null, "Malappuram",
                List.of("Swimming Pool"), false, false, false, false,
                null, false).isSuccess());

        check("accessibility requirement filters to accessible shelters", () -> {
            ActionResult r = c.preview(null, "Wheel Family", "2",
                    null, "Malappuram", null,
                    true, false, false, false, null);
            return r.isSuccess() && r.<SmartAllocationResult>getData()
                    .getRanked().stream().allMatch(x ->
                            x.getShelter().isWheelchairAccessible());
        });

        // ── lifecycle: pending -> confirm -> check in -> complete ────
        int pendBefore = totalOcc(c);
        check("pending reservation does NOT occupy space", () -> {
            ActionResult r = c.allocate(null, "Pending Family", "2",
                    PriorityLevel.MEDIUM, "Malappuram", null,
                    false, false, false, false, null, true);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            SmartAllocationResult result = r.getData();
            pendingAllocId = result.getAllocation().getId();
            check("pending status recorded", () -> result.getAllocation()
                    .getStatus() == ShelterAllocationStatus.PENDING);
            return totalOcc(c) == pendBefore;
        });

        int confirmBefore = totalOcc(c);
        check("confirm pending opens space", () -> {
            ActionResult r = c.confirmPending(pendingAllocId);
            check("confirm pending succeeds", r::isSuccess);
            return totalOcc(c) == confirmBefore + 2;
        });

        check("check-in keeps occupancy", () -> {
            ActionResult r = c.checkIn(pendingAllocId);
            check("check-in succeeds", r::isSuccess);
            return totalOcc(c) == confirmBefore + 2;
        });

        check("complete frees space", () -> {
            ActionResult r = c.complete(pendingAllocId);
            check("complete succeeds", r::isSuccess);
            return totalOcc(c) == confirmBefore;
        });

        // ── release + double-release guard ───────────────────────────
        int relBefore = totalOcc(c);
        check("smart allocate an active family + capture", () -> {
            ActionResult r = c.allocate(null, "Release Family", "2",
                    PriorityLevel.LOW, "Wayanad", null,
                    false, false, false, false, null, false);
            if (!r.isSuccess()) {
                throw new AssertionError(r.getMessage());
            }
            activeAllocId = r.<SmartAllocationResult>getData()
                    .getAllocation().getId();
            return totalOcc(c) == relBefore + 2;
        });
        check("release frees space", () -> {
            ActionResult r = c.release(activeAllocId);
            check("release succeeds", r::isSuccess);
            return totalOcc(c) == relBefore;
        });
        check("double release rejected", () ->
                !c.release(activeAllocId).isSuccess());

        // ── management reads ─────────────────────────────────────────
        check("allocations list non-empty", () ->
                !c.getAllAllocations().isEmpty());
        check("waiting list non-empty", () ->
                !c.getWaitingForShelter().isEmpty());
        check("counts consistent", () -> c.countWaiting()
                == c.getWaitingForShelter().size());

        // ── reports integration ──────────────────────────────────────
        check("allocation overview report generates", () -> {
            ReportController rc = new ReportController();
            ReportResult r = rc.generateReport(ReportType.ALLOCATION_OVERVIEW,
                    com.resqhub.model.ReportFilters.empty()).getData();
            return r != null && r.rows() != null && !r.rows().isEmpty();
        });

        // ── cleanup ──────────────────────────────────────────────────
        check("cleanup: delete test shelter + victim", () -> {
            try {
                new com.resqhub.service.ShelterService()
                        .deleteShelter(selfShelterId);
            } catch (Exception ok) {
                // shelter may already be gone
            }
            try {
                new com.resqhub.service.VictimService()
                        .deleteVictim(victimId);
            } catch (Exception ok) {
                // best-effort
            }
            return true;
        });
    }

    private static int totalOcc(SmartAllocationController c) {
        try {
            int sum = 0;
            for (Shelter s : c.getAllShelters()) {
                sum += s.getCurrentOccupancy();
            }
            return sum;
        } catch (Exception ignored) {
            return -1;
        }
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
