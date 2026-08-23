package com.resqhub.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;

import com.resqhub.config.DatabaseConnectionManager;
import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.DisasterController;
import com.resqhub.controller.RescueRequestController;
import com.resqhub.controller.RescueTeamController;
import com.resqhub.controller.VictimController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterType;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.Gender;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RequestStatus;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RoleType;
import com.resqhub.model.ShelterStatus;
import com.resqhub.model.TeamType;
import com.resqhub.model.Victim;
import com.resqhub.service.SessionManager;
import com.resqhub.service.UserService;
import com.resqhub.util.InputParser;

/**
 * PHASE 8 - end-to-end integration test.
 * Drives the full rescue lifecycle through CONTROLLERS ONLY,
 * exactly as the Swing views do, switching between three roles.
 */
public class Phase8Test {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        try {
            preClean();
            runLifecycle();
        } catch (Exception e) {
            System.out.println("[FATAL] " + e);
            e.printStackTrace();
            failed++;
        } finally {
            cleanUp();
        }
        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------
    // Scenario
    // ------------------------------------------------------------------

    private static void runLifecycle() throws Exception {
        System.out.println("--- Section A: session bootstrap ----------------------");

        SessionManager first = SessionManager.getInstance();
        SessionManager second = SessionManager.getInstance();
        check("SessionManager is one shared instance", first == second);

        AuthController auth = new AuthController();
        ActionResult loginResult = auth.login("admin", "Admin@123");
        check("admin logs in through AuthController", loginResult.isSuccess());
        check("session now holds the admin",
                SessionManager.getInstance().isLoggedIn());

        UserService userService = new UserService();
        boolean citizenCreated = false;
        try {
            userService.registerUser("zztest_citizen", "Citizen@123",
                    "ZZTEST Citizen", "zztest_citizen@resqhub.local",
                    null, RoleType.CITIZEN);
            citizenCreated = true;
        } catch (Exception e) {
            System.out.println("   citizen setup skipped: " + e.getMessage());
        }
        check("admin created a CITIZEN account (ADMIN-only service op)",
                citizenCreated);

        System.out.println("--- Section B: reference data via controllers ---------");

        DisasterController disasterController = new DisasterController();
        String nowText = LocalDateTime.now()
                .format(InputParser.DATE_TIME_FORMAT);
        ActionResult disasterResult = disasterController.createDisaster(
                "ZZTEST Flood 2026", DisasterType.FLOOD,
                DisasterSeverity.SEVERE, "ZZTEST Zone 4", "5000",
                nowText, null, "integration scenario flood");
        check("disaster registered via controller", disasterResult.isSuccess());
        Disaster disaster = disasterResult.getData();

        VictimController victimController = new VictimController();
        ActionResult v1Result = victimController.registerVictim(
                "ZZTEST V One", "34", Gender.FEMALE, null,
                EmergencyStatus.CRITICAL, "leg injury",
                "husband missing", "ZZTEST Lane 1", disaster.getId());
        check("critical victim registered via controller", v1Result.isSuccess());
        Victim victimOne = v1Result.getData();

        ActionResult v2Result = victimController.registerVictim(
                "ZZTEST V Two", "70", Gender.MALE, null,
                EmergencyStatus.INJURED, null, null,
                "ZZTEST Lane 2", disaster.getId());
        check("second victim registered via controller", v2Result.isSuccess());

        RescueTeamController teamController = new RescueTeamController();
        ActionResult teamResult = teamController.registerTeam(
                "ZZTEST Alpha Team", TeamType.FIRE_RESCUE, "ZZTEST Leader",
                "9999900001", "6", "swimming, rope", "boat, cutter",
                "ZZTEST Base");
        check("rescue team registered via controller", teamResult.isSuccess());
        RescueTeam team = teamResult.getData();

        System.out.println("--- Section C: submission and priority queue ----------");

        RescueRequestController requestController = new RescueRequestController();
        ActionResult requestAResult = requestController.submitRequest(
                disaster.getId(), victimOne.getId(),
                "ZZTEST Requester A", "9999911111", "ZZTEST Rooftop A",
                "5", "2", "1", true, true, true,
                "family stranded on roof, rising water");
        check("critical request submitted via controller",
                requestAResult.isSuccess());
        RescueRequest requestA = requestAResult.getData();
        check("priority engine rates scenario A as CRITICAL",
                requestA.getPriority() == PriorityLevel.CRITICAL);

        ActionResult requestBResult = requestController.submitRequest(
                disaster.getId(), null,
                "ZZTEST Requester B", "9999922222", "ZZTEST Street B",
                "2", "0", "0", false, false, false,
                "food and water needed");
        check("low-risk request submitted via controller",
                requestBResult.isSuccess());
        RescueRequest requestB = requestBResult.getData();
        check("priority engine rates scenario B as LOW",
                requestB.getPriority() == PriorityLevel.LOW);

        List<RescueRequest> queue = requestController.getPendingQueue();
        int indexA = indexOfId(queue, requestA.getId());
        int indexB = indexOfId(queue, requestB.getId());
        check("pending queue contains both requests",
                indexA >= 0 && indexB >= 0);
        check("queue sorts CRITICAL above LOW", indexA < indexB);

        System.out.println("--- Section D: assignment workflow --------------------");

        ActionResult badAssign = requestController.assignTeam(
                requestA.getId(), 999999L);
        check("assigning a non-existent team fails politely",
                !badAssign.isSuccess());

        ActionResult assignResult = requestController.assignTeam(
                requestA.getId(), team.getId());
        check("team assigned to request A", assignResult.isSuccess());
        check("success message reports ASSIGNED state",
                assignResult.getMessage().contains("ASSIGNED"));

        long assignmentId =
                requestController.getLatestAssignmentId(requestA.getId());
        check("latest assignment id is resolvable", assignmentId > 0);

        List<RescueRequest> afterAssign = requestController.getPendingQueue();
        check("assigned request left the pending queue",
                indexOfId(afterAssign, requestA.getId()) < 0);

        ActionResult busyAssign = requestController.assignTeam(
                requestB.getId(), team.getId());
        check("same team cannot serve two requests at once",
                !busyAssign.isSuccess());

        check("progress ASSIGNED -> EN_ROUTE accepted",
                requestController
                        .progressAssignment(assignmentId,
                                AssignmentStatus.EN_ROUTE, "boat launched")
                        .isSuccess());
        check("progress EN_ROUTE -> ON_SITE accepted",
                requestController
                        .progressAssignment(assignmentId,
                                AssignmentStatus.ON_SITE, "arrived")
                        .isSuccess());

        ActionResult backwards = requestController.progressAssignment(
                assignmentId, AssignmentStatus.EN_ROUTE, "");
        check("moving backwards ON_SITE -> EN_ROUTE is rejected",
                !backwards.isSuccess());

        ActionResult completeResult =
                requestController.completeAssignment(assignmentId);
        check("assignment completed", completeResult.isSuccess());
        check("request A reached RESCUED status",
                statusListContains(requestController, RequestStatus.RESCUED,
                        requestA.getId()));

        ActionResult afterComplete = requestController.progressAssignment(
                assignmentId, AssignmentStatus.ON_SITE, "");
        check("progressing a completed assignment is rejected",
                !afterComplete.isSuccess());

        boolean teamReleased = false;
        for (RescueTeam candidate : teamController.getAllTeams()) {
            if (candidate.getId().equals(team.getId())
                    && candidate.getAvailabilityStatus()
                            == AvailabilityStatus.AVAILABLE) {
                teamReleased = true;
            }
        }
        check("completed team released back to AVAILABLE", teamReleased);

        ActionResult cancelResult =
                requestController.cancelRequest(requestB.getId());
        check("pending request B cancelled", cancelResult.isSuccess());
        check("request B shows as CANCELLED",
                statusListContains(requestController, RequestStatus.CANCELLED,
                        requestB.getId()));

        ActionResult explainResult =
                requestController.explainPriority(requestA.getId());
        Object breakdown = explainResult.getData();
        check("priority breakdown text produced",
                explainResult.isSuccess()
                        && breakdown != null
                        && breakdown.toString().contains("TOTAL SCORE"));

        ActionResult shelterHook = victimController.markShelterStatus(
                victimOne.getId(), ShelterStatus.IN_SHELTER);
        check("shelter-status hook works for Ameya's module",
                shelterHook.isSuccess());

        System.out.println("--- Section E: citizen role restrictions --------------");

        auth.logout();
        check("session cleared after logout",
                !SessionManager.getInstance().isLoggedIn());

        ActionResult citizenLogin = auth.login("zztest_citizen", "Citizen@123");
        if (!citizenCreated) {
            System.out.println("   (citizen account missing - using officer"
                    + " gating checks only)");
            citizenLogin = auth.login("officer1", "Rescue@123");
        }
        check("restricted-role login succeeded", citizenLogin.isSuccess());

        boolean queueBlockedForCitizen = false;
        try {
            requestController.getPendingQueue();
        } catch (DataAccessException expected) {
            queueBlockedForCitizen = true;
        }
        check("operations queue hidden from non-officers",
                queueBlockedForCitizen);

        ActionResult citizenVictim = victimController.registerVictim(
                "ZZTEST Nope", "30", Gender.OTHER, null,
                EmergencyStatus.SAFE, null, null, "nowhere", disaster.getId());
        check("victim registration blocked for restricted roles",
                !citizenVictim.isSuccess());

        ActionResult citizenTeam = teamController.registerTeam(
                "ZZTEST Rogue Team", TeamType.OTHER, "X", "9999933333",
                "1", "", "", "");
        check("team registration blocked for restricted roles",
                !citizenTeam.isSuccess());

        ActionResult citizenSubmit = requestController.submitRequest(
                disaster.getId(), null,
                "ZZTEST Requester C", "9999944444", "ZZTEST Alley C",
                "1", "0", "0", false, false, false,
                "insulin needed");
        check("emergency SUBMISSION still open to citizens",
                citizenSubmit.isSuccess());

        System.out.println("--- Section F: officer view ---------------------------");

        auth.logout();
        ActionResult officerLogin = auth.login("officer1", "Rescue@123");
        check("seeded officer login succeeded", officerLogin.isSuccess());

        List<RescueRequest> officerQueue = requestController.getPendingQueue();
        check("queue readable again for officers",
                indexOfId(officerQueue,
                        Long.parseLong(citizenSubmitSuccessId(citizenSubmit)))
                        >= 0 || !officerQueue.isEmpty());

        auth.logout();
    }

    private static String citizenSubmitSuccessId(ActionResult result) {
        Object data = result.getData();
        return data == null ? "-1"
                : String.valueOf(((RescueRequest) data).getId());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static int indexOfId(List<RescueRequest> requests, Long id) {
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean statusListContains(
            RescueRequestController controller, RequestStatus status,
            Long requestId) throws DataAccessException {
        for (RescueRequest request : controller.getByStatus(status)) {
            if (request.getId().equals(requestId)) {
                return true;
            }
        }
        return false;
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + label);
        } else {
            failed++;
            System.out.println("[FAIL] " + label);
        }
    }

    // ------------------------------------------------------------------
    // Test-data hygiene (FK-safe reverse order)
    // ------------------------------------------------------------------

    private static void preClean() throws Exception {
        cleanUp();
    }

    private static void cleanUp() {
        String[] statements = {
                "DELETE a FROM rescue_assignments a "
                        + "JOIN rescue_requests r "
                        + "ON a.rescue_request_id = r.id "
                        + "WHERE r.requester_name LIKE 'ZZTEST%'",
                "DELETE FROM rescue_requests WHERE requester_name LIKE 'ZZTEST%'",
                "DELETE FROM victims WHERE full_name LIKE 'ZZTEST%'",
                "DELETE FROM rescue_teams WHERE team_name LIKE 'ZZTEST%'",
                "DELETE FROM disasters WHERE title LIKE 'ZZTEST%'",
                "DELETE FROM users WHERE username LIKE 'zztest_%'"
        };
        try {
            Connection connection =
                    DatabaseConnectionManager.getInstance().getConnection();
            for (String sql : statements) {
                try (PreparedStatement statement =
                        connection.prepareStatement(sql)) {
                    statement.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("cleanup failed: " + e.getMessage(), e);
        }
    }
}
