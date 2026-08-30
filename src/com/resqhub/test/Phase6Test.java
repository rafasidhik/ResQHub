package com.resqhub.test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import com.resqhub.config.DatabaseConnectionManager;
import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.DisasterController;
import com.resqhub.controller.RescueRequestController;
import com.resqhub.controller.RescueTeamController;
import com.resqhub.controller.VictimController;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterType;
import com.resqhub.model.Gender;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.TeamType;
import com.resqhub.model.User;

/**
 * Phase 6 smoke test - controllers exercised exactly as Swing views will
 * call them (strings in, ActionResult out). Run after compile.bat:
 *   java -cp "out;lib\*;resources" com.resqhub.test.Phase6Test
 */
public class Phase6Test {

    private static int passed = 0;
    private static int failed = 0;

    private static final AuthController authController = new AuthController();
    private static final DisasterController disasterController = new DisasterController();
    private static final VictimController victimController = new VictimController();
    private static final RescueTeamController teamController = new RescueTeamController();
    private static final RescueRequestController requestController =
            new RescueRequestController();

    private static Long severeDisasterId;
    private static Long victimId;
    private static Long requestId;
    private static Long teamId;
    private static Long assignmentId;

    public static void main(String[] args) throws Exception {
        preClean();
        try {
            testLoginViaController();
            testDisasterScreenFlow();
            testVictimScreenFlow();
            testRequestScreenFlow();
            testAssignmentScreenFlow();
        } finally {
            authController.logout();
            cleanUp();
            System.out.println();
            System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
            if (failed > 0) {
                System.exit(1);
            }
        }
    }

    private static void testLoginViaController() {
        ActionResult badLogin = authController.login("zztest_ghost", "nope1234");
        check("bad login returns failure result", !badLogin.isSuccess());

        ActionResult login = authController.login("admin", "Admin@123");
        check("good login returns success result", login.isSuccess());
        check("login payload is a User",
                login.<User>getData() instanceof User);
        check("success message names the role",
                login.getMessage().contains("Administrator"));
    }

    private static void testDisasterScreenFlow() throws Exception {
        ActionResult badNumber = disasterController.createDisaster(
                "ZZTEST Controller Disaster", DisasterType.FLOOD,
                DisasterSeverity.SEVERE, "ZZTEST city", "many",
                "2026-08-20 08:00", "", null);
        check("non-numeric population rejected with message",
                !badNumber.isSuccess() && badNumber.getMessage().contains("whole"));

        ActionResult badDate = disasterController.createDisaster(
                "ZZTEST Date Test", DisasterType.FLOOD, DisasterSeverity.LOW,
                "ZZTEST town", "5", "yesterday", "", null);
        check("malformed date rejected with hint",
                !badDate.isSuccess()
                && badDate.getMessage().contains("yyyy-MM-dd"));

        ActionResult created = disasterController.createDisaster(
                "ZZTEST Severe Quake", DisasterType.EARTHQUAKE,
                DisasterSeverity.SEVERE, "ZZTEST hills", "1200",
                "2026-08-21 06:30", "", "controller-created");
        check("valid disaster created via controller", created.isSuccess());
        severeDisasterId = created.<com.resqhub.model.Disaster>getData().getId();

        List<com.resqhub.model.Disaster> all =
                disasterController.search("zztest");
        check("search finds controller-created disaster", all.size() == 1);
    }

    private static void testVictimScreenFlow() throws Exception {
        ActionResult badAge = victimController.registerVictim("ZZTEST Old",
                "abc", Gender.MALE, null, null, null, null,
                "ZZTEST street", severeDisasterId);
        check("non-numeric age rejected", !badAge.isSuccess());

        ActionResult created = victimController.registerVictim(
                "ZZTEST Controlled Victim", "45", Gender.FEMALE, "",
                null, "none", "son nearby", "ZZTEST shelter ground",
                severeDisasterId);
        check("victim registered via controller", created.isSuccess());
        victimId = created.<com.resqhub.model.Victim>getData().getId();

        ActionResult statusUpdate = victimController.updateEmergencyStatus(
                victimId, com.resqhub.model.EmergencyStatus.INJURED);
        check("emergency status update succeeds", statusUpdate.isSuccess());
    }

    private static void testRequestScreenFlow() throws Exception {
        ActionResult badCounts = requestController.submitRequest(
                severeDisasterId, null, "ZZTEST Count Caller", "9700000001",
                "ZZTEST lane", "2", "5", "", true, false, false, null);
        check("children > people rejected through controller",
                !badCounts.isSuccess());

        ActionResult submitted = requestController.submitRequest(
                severeDisasterId, victimId, "ZZTEST Ctrl Caller", "9700000002",
                "ZZTEST collapsed block", "3", "1", "1",
                true, true, false, "urgent extraction");
        check("critical request submitted via controller", submitted.isSuccess());
        RescueRequest saved = submitted.getData();
        requestId = saved.getId();
        check("priority computed as CRITICAL",
                saved.getPriority() == PriorityLevel.CRITICAL);

        ActionResult explanation = requestController.explainPriority(requestId);
        String breakdown = explanation.getData();
        check("priority breakdown text produced",
                explanation.isSuccess() && breakdown.contains("TOTAL SCORE"));
        System.out.println("   " + breakdown.replace("\n", "\n   "));
    }

    private static void testAssignmentScreenFlow() throws Exception {
        ActionResult teamCreated = teamController.registerTeam(
                "ZZTEST Ctrl Unit", TeamType.NDRF, "Ctrl Leader",
                "9600000001", "8", "heavy rescue", "cranes", "ZZTEST base");
        check("team registered via controller", teamCreated.isSuccess());
        teamId = teamCreated.<com.resqhub.model.RescueTeam>getData().getId();

        ActionResult assigned = requestController.assignTeam(requestId, teamId);
        check("assignment made via controller", assigned.isSuccess());
        check("message reports ASSIGNED state",
                assigned.getMessage().contains("ASSIGNED"));

        List<RescueRequest> pending = requestController.getPendingQueue();
        boolean stillListed = false;
        for (RescueRequest r : pending) {
            if (r.getId().equals(requestId)) {
                stillListed = true;
            }
        }
        check("assigned request left the pending queue", !stillListed);

        assignmentId = findLatestAssignmentId(requestId);
        ActionResult enRoute = requestController.progressAssignment(assignmentId,
                AssignmentStatus.EN_ROUTE, "rolling out");
        ActionResult onSite = requestController.progressAssignment(assignmentId,
                AssignmentStatus.ON_SITE, "");
        check("progression buttons work", enRoute.isSuccess() && onSite.isSuccess());

        ActionResult completed = requestController.completeAssignment(assignmentId);
        check("completion works from the controller",
                completed.isSuccess());

        ActionResult cancelled = requestController.cancelRequest(requestId);
        check("cancelling a RESCUED request fails politely",
                !cancelled.isSuccess());
    }

    private static long findLatestAssignmentId(long reqId) throws Exception {
        var dao = new com.resqhub.dao.RescueAssignmentDAO();
        var list = dao.findByRequest(reqId);
        return list.isEmpty() ? -1 : list.get(0).getId();
    }

    private static void preClean() throws Exception {
        try (Connection con = DatabaseConnectionManager.getInstance()
                .getConnection(); Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM rescue_assignments WHERE rescue_request_id IN "
                    + "(SELECT id FROM rescue_requests WHERE requester_name LIKE 'ZZTEST%')");
            st.executeUpdate("DELETE FROM rescue_requests "
                    + "WHERE requester_name LIKE 'ZZTEST%' OR requester_name LIKE 'ZZTEST %'");
            st.executeUpdate("DELETE FROM victims WHERE full_name LIKE 'ZZTEST%'");
            st.executeUpdate("DELETE FROM rescue_teams WHERE team_name LIKE 'ZZTEST%'");
            st.executeUpdate("DELETE FROM disasters WHERE title LIKE 'ZZTEST%'");
        }
    }

    private static void cleanUp() {
        silentDelete(() -> {
            if (assignmentId != null && assignmentId > 0) {
                new com.resqhub.dao.RescueAssignmentDAO().deleteById(assignmentId);
            }
        });
        silentDelete(() -> { if (requestId != null) new com.resqhub.dao.RescueRequestDAO().deleteById(requestId); });
        silentDelete(() -> { if (victimId != null) new com.resqhub.dao.VictimDAO().deleteById(victimId); });
        silentDelete(() -> { if (teamId != null) new com.resqhub.dao.RescueTeamDAO().deleteById(teamId); });
        silentDelete(() -> { if (severeDisasterId != null) new com.resqhub.dao.DisasterDAO().deleteById(severeDisasterId); });
    }

    private interface SilentAction {
        void run() throws Exception;
    }

    private static void silentDelete(SilentAction action) {
        try {
            action.run();
        } catch (Exception ignored) {
            // best-effort cleanup
        }
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + label);
        } else {
            failed++;
            System.out.println("[FAIL] " + label);
        }
    }
}
