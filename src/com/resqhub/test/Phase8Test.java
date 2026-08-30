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
import com.resqhub.controller.StatsController;
import com.resqhub.controller.UserController;
import com.resqhub.controller.VictimController;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.Gender;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RequestStatus;
import com.resqhub.model.RescueAssignment;
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

        AuthController signupController = new AuthController();
        check("citizen SELF-signup works without admin",
                signupController.registerCitizen("zztest_selfreg",
                        "Citizen@123", "ZZTEST Self Reg",
                        "zztest_selfreg@resqhub.local", null).isSuccess());
        check("duplicate username rejected during signup",
                !signupController.registerCitizen("zztest_selfreg",
                        "Citizen@123", "ZZTEST Dup",
                        "zztest_dup@resqhub.local", null).isSuccess());
        check("weak password rejected during signup",
                !signupController.registerCitizen("zztest_weakpw", "short",
                        "ZZTEST Weak", "zztest_weak@resqhub.local",
                        null).isSuccess());

        UserController userController = new UserController();
        check("admin registers staff account via controller",
                userController.registerUser("zztest_officer2", "Officer@123",
                        "ZZTEST Officer Two", "zztest_officer2@resqhub.local",
                        null, RoleType.RESCUE_OFFICER).isSuccess());
        auth.logout();
        check("new staff account can log in",
                auth.login("zztest_officer2", "Officer@123").isSuccess());
        check("staff (non-admin) cannot create users via controller",
                !userController.registerUser("zztest_nope", "Officer@123",
                        "ZZTEST Nope", "zztest_nope@resqhub.local", null,
                        RoleType.ADMIN).isSuccess());
        auth.logout();
        check("re-login as admin restores session",
                auth.login("admin", "Admin@123").isSuccess());

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

        check("disaster activation REPORTED -> ACTIVE works",
                disasterController.updateStatus(disaster.getId(),
                        DisasterStatus.ACTIVE).isSuccess());
        check("skipping lifecycle steps is rejected (ACTIVE -> RESOLVED)",
                !disasterController.updateStatus(disaster.getId(),
                        DisasterStatus.RESOLVED).isSuccess());
        check("backwards transition is rejected (ACTIVE -> REPORTED)",
                !disasterController.updateStatus(disaster.getId(),
                        DisasterStatus.REPORTED).isSuccess());

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

        System.out.println("--- Section G: admin deletion powers ------------------");

        check("admin deletes never-assigned cancelled request",
                requestController.deleteRequest(requestB.getId()).isSuccess());
        check("request with assignment history cannot be deleted",
                !requestController.deleteRequest(requestA.getId()).isSuccess());
        check("admin deletes victim (references auto-nulled)",
                victimController.deleteVictim(victimOne.getId()).isSuccess());
        check("disaster with dependants cannot be deleted",
                !disasterController.deleteDisaster(disaster.getId()).isSuccess());
        check("team with assignment history cannot be deleted",
                !teamController.deleteTeam(team.getId()).isSuccess());

        ActionResult disposableResult = disasterController.createDisaster(
                "ZZTEST Disposable", DisasterType.FIRE, DisasterSeverity.LOW,
                "ZZTEST Void", "1",
                LocalDateTime.now().format(InputParser.DATE_TIME_FORMAT),
                null, "delete-me");
        check("admin deletes unreferenced disaster",
                disposableResult.isSuccess()
                        && disasterController.deleteDisaster(
                                ((Disaster) disposableResult.getData())
                                        .getId()).isSuccess());

        ActionResult doomedAccount = userController.registerUser(
                "zztest_doomed", "Doomed@123", "ZZTEST Doomed",
                "zztest_doomed@resqhub.local", null, RoleType.VOLUNTEER);
        check("admin deletes a staff account",
                doomedAccount.isSuccess()
                        && userController.deleteUser(
                                ((com.resqhub.model.User)
                                        doomedAccount.getData()).getId())
                                .isSuccess());
        check("deleted account can no longer log in",
                !auth.login("zztest_doomed", "Doomed@123").isSuccess());
        long adminId = SessionManager.getInstance().getCurrentUser().getId();
        check("admin cannot delete own active account",
                !userController.deleteUser(adminId).isSuccess());

        System.out.println("--- Section H: edit, abort, history, stats ------------");

        ActionResult requestCResult = requestController.submitRequest(
                disaster.getId(), null,
                "ZZTEST Requester C", "9999944444", "ZZTEST Alley C",
                "1", "0", "0", false, false, false, "insulin needed");
        check("section H scenario request submitted",
                requestCResult.isSuccess());
        long requestCid =
                ((RescueRequest) requestCResult.getData()).getId();

        check("admin edits a disaster",
                disasterController.updateDisaster(disaster.getId(),
                        "ZZTEST Flood 2026 EDITED", DisasterType.FLOOD,
                        DisasterSeverity.LOW, "ZZTEST Zone 9", "12000",
                        LocalDateTime.now().format(InputParser.DATE_TIME_FORMAT),
                        null, "edited description").isSuccess());
        boolean disasterEdited = false;
        for (Disaster candidate : disasterController.getAllDisasters()) {
            if (candidate.getId().equals(disaster.getId())
                    && candidate.getSeverity() == DisasterSeverity.LOW
                    && candidate.getAffectedPopulation() == 12000
                    && candidate.getTitle().endsWith("EDITED")) {
                disasterEdited = true;
            }
        }
        check("edited disaster fields persisted", disasterEdited);

        Victim victimTwo = (Victim) v2Result.getData();
        check("staff edits a victim",
                victimController.updateVictim(victimTwo.getId(),
                        "ZZTEST V Two Edited", "71", Gender.MALE,
                        "9999977777", EmergencyStatus.SAFE, null, null,
                        "ZZTEST Lane 2", disaster.getId()).isSuccess());
        boolean victimEdited = false;
        for (Victim candidate : victimController.getAllVictims()) {
            if (candidate.getId().equals(victimTwo.getId())
                    && candidate.getFullName().endsWith("Edited")
                    && candidate.getEmergencyStatus()
                            == EmergencyStatus.SAFE) {
                victimEdited = true;
            }
        }
        check("edited victim fields persisted", victimEdited);

        check("staff edits a team",
                teamController.updateTeam(team.getId(), "ZZTEST Alpha Team",
                        TeamType.FIRE_RESCUE, "ZZTEST Leader", "9999900001",
                        "9", "swimming, rope, diving", "boat, cutter",
                        "ZZTEST Base").isSuccess());
        boolean teamEdited = false;
        for (RescueTeam candidate : teamController.getAllTeams()) {
            if (candidate.getId().equals(team.getId())
                    && candidate.getMemberCount() == 9) {
                teamEdited = true;
            }
        }
        check("edited team fields persisted", teamEdited);

        check("PENDING request edited with priority recompute",
                requestController.updateRequest(requestCid,
                        disaster.getId(), null,
                        "ZZTEST Requester C", "9999944444", "ZZTEST Alley C",
                        "4", "2", "2", true, true, true,
                        "situation escalated").isSuccess());
        boolean bumped = false;
        for (RescueRequest candidate
                : requestController.getByStatus(RequestStatus.PENDING)) {
            if (candidate.getId().equals(requestCid)
                    && candidate.getPriority() != PriorityLevel.LOW) {
                bumped = true;
            }
        }
        check("edited request left the LOW band", bumped);
        check("non-PENDING request edit is rejected",
                !requestController.updateRequest(requestA.getId(),
                        disaster.getId(), null, "X", "9999911111", "Y",
                        "1", "0", "0", false, false, false, "")
                        .isSuccess());

        check("team reassigned to edited request",
                requestController.assignTeam(requestCid, team.getId())
                        .isSuccess());
        long assignmentTwo =
                requestController.getLatestAssignmentId(requestCid);
        check("abort releases the team and requeues the request",
                requestController.abortAssignment(assignmentTwo,
                        "flood worsened, pulled back").isSuccess());
        boolean abortedTeamFree = false;
        for (RescueTeam candidate : teamController.getAllTeams()) {
            if (candidate.getId().equals(team.getId())
                    && candidate.getAvailabilityStatus()
                            == AvailabilityStatus.AVAILABLE) {
                abortedTeamFree = true;
            }
        }
        check("aborted team is AVAILABLE again", abortedTeamFree);
        check("aborted request returned to PENDING queue",
                indexOfId(requestController.getPendingQueue(), requestCid)
                        >= 0);
        boolean abortNotesStored = false;
        for (RescueAssignment record
                : requestController.getAssignmentHistory(requestCid)) {
            if (record.getNotes() != null
                    && record.getNotes().contains("flood worsened")) {
                abortNotesStored = true;
            }
        }
        check("abort reason stored in assignment history", abortNotesStored);
        check("assignment history readable for completed request too",
                !requestController.getAssignmentHistory(requestA.getId())
                        .isEmpty());

        ActionResult summary = new StatsController().getSummary();
        Object statsText = summary.getData();
        check("stats summary produced for staff",
                summary.isSuccess() && statsText != null
                        && statsText.toString()
                                .contains("PENDING RESCUE REQUESTS"));

        ActionResult resettableResult = userController.registerUser(
                "zztest_resetpw", "Oldpw@123", "ZZTEST Reset",
                "zztest_resetpw@resqhub.local", null, RoleType.VOLUNTEER);
        com.resqhub.model.User resettable =
                (com.resqhub.model.User) resettableResult.getData();
        check("admin updates user profile and role",
                userController.updateUser(resettable.getId(),
                        "ZZTEST Reset Edited", "zztest_resetpw@resqhub.local",
                        "9999988888", RoleType.CAMP_MANAGER).isSuccess());
        check("admin resets a user password",
                userController.resetPassword(resettable.getId(),
                        "Newpw@456").isSuccess());
        auth.logout();
        check("login works with the reset password",
                auth.login("zztest_resetpw", "Newpw@456").isSuccess());
        check("old password rejected after reset",
                !auth.login("zztest_resetpw", "Oldpw@123").isSuccess());
        check("role change persisted after update",
                SessionManager.getInstance().getCurrentUser().getRole()
                        == RoleType.CAMP_MANAGER);

        auth.logout();
        check("admin session restored after Section H",
                auth.login("admin", "Admin@123").isSuccess());

        System.out.println("--- Section I: auth & profile features ---------------");

        check("login accepts email instead of username",
                auth.login("zztest_resetpw@resqhub.local", "Newpw@456")
                        .isSuccess());
        auth.logout();
        check("login rejects wrong password via email path",
                !auth.login("zztest_resetpw@resqhub.local", "bad")
                        .isSuccess());
        check("login rejects unknown email gracefully",
                !auth.login("nobody@example.com", "X").isSuccess());

        auth.login("admin", "Admin@123");
        AuthController authCtrl = new AuthController();
        com.resqhub.model.User selfBefore = authCtrl.getCurrentUser();
        check("self-service profile update succeeds",
                authCtrl.updateOwnProfile("ZZTEST Admin Edited",
                        selfBefore.getEmail(), "9999900000").isSuccess());
        check("profile update persisted in session",
                "ZZTEST Admin Edited".equals(
                        authCtrl.getCurrentUser().getFullName()));
        check("self-service profile rejects invalid email",
                !authCtrl.updateOwnProfile("ZZTEST Admin Edited",
                        "not-an-email", "9999900000").isSuccess());

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

        check("deletion is ADMIN-only even through controllers",
                !requestController.deleteRequest(1L).isSuccess());

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
