package com.resqhub.test;

import java.time.LocalDateTime;
import java.util.List;

import com.resqhub.config.DatabaseConnectionManager;
import com.resqhub.dao.RescueAssignmentDAO;
import com.resqhub.dao.RescueRequestDAO;
import com.resqhub.dao.UserDAO;
import com.resqhub.exception.AuthenticationException;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidDisasterDataException;
import com.resqhub.exception.InvalidRescueRequestException;
import com.resqhub.exception.InvalidTeamDataException;
import com.resqhub.exception.InvalidUserDataException;
import com.resqhub.exception.InvalidVictimDataException;
import com.resqhub.exception.OperationNotAllowedException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterType;
import com.resqhub.model.Gender;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RequestStatus;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RoleType;
import com.resqhub.model.TeamType;
import com.resqhub.model.User;
import com.resqhub.service.AuthService;
import com.resqhub.service.DisasterService;
import com.resqhub.service.RescuePriorityEngine;
import com.resqhub.service.RescueRequestService;
import com.resqhub.service.RescueTeamService;
import com.resqhub.service.SessionManager;
import com.resqhub.service.UserService;
import com.resqhub.service.VictimService;

/**
 * Phase 5 smoke test - service layer end to end (live database).
 * Run after compile.bat:
 *   java -cp "out;lib\*;resources" com.resqhub.test.Phase5Test
 */
public class Phase5Test {

    private static int passed = 0;
    private static int failed = 0;

    private static final AuthService authService = new AuthService();
    private static final UserService userService = new UserService();
    private static final DisasterService disasterService = new DisasterService();
    private static final VictimService victimService = new VictimService();
    private static final RescueTeamService teamService = new RescueTeamService();
    private static final RescueRequestService requestService = new RescueRequestService();
    private static final UserDAO userDAO = new UserDAO();
    private static final RescueRequestDAO requestDao = new RescueRequestDAO();
    private static final RescueAssignmentDAO assignmentDao = new RescueAssignmentDAO();

    private static Long officerUserId;
    private static Long zzOfficerId;
    private static Long severeDisasterId;
    private static Long lowDisasterId;
    private static Long victimId;
    private static Long criticalRequestId;
    private static Long lowRequestId;
    private static Long teamId;

    public static void main(String[] args) throws Exception {
        preClean();
        try {
            testLockoutMechanism();
            testAdminRegistrationFlow();
            testRoleEnforcement();
            testDisasterValidation();
            testVictimValidation();
            testPriorityComputation();
            testAssignmentLifecycle();
        } finally {
            cleanUp();
            System.out.println();
            System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
            if (failed > 0) {
                System.exit(1);
            }
        }
    }

    // ------------------------------------------------------------------
    // 1. Login failures, failed-attempt counting and automatic lockout
    // ------------------------------------------------------------------
    private static void testLockoutMechanism() throws Exception {
        expectAuthenticationFailure("login with unknown username rejected",
                () -> authService.login("zztest_ghost", "whatever123"));

        for (int attempt = 1; attempt <= AuthService.MAX_FAILED_ATTEMPTS; attempt++) {
            final String label = "wrong password attempt " + attempt + " rejected";
            try {
                authService.login("officer1", "WrongPass" + attempt);
                check(label, false);
            } catch (AuthenticationException e) {
                check(label, true);
                if (attempt == AuthService.MAX_FAILED_ATTEMPTS) {
                    check("final failure message reports the lock",
                            e.getMessage().toLowerCase().contains("lock"));
                }
            }
        }

        User locked = userDAO.findByUsername("officer1");
        check("account persisted as LOCKED after "
                + AuthService.MAX_FAILED_ATTEMPTS + " failures",
                locked.getAccountStatus() == AccountStatus.LOCKED);

        expectAuthenticationFailure("correct password refused while locked",
                () -> authService.login("officer1", "Rescue@123"));
    }

    // ------------------------------------------------------------------
    // 2. Admin unlocks officer, registers a test officer account
    // ------------------------------------------------------------------
    private static void testAdminRegistrationFlow() throws Exception {
        authService.login("admin", "Admin@123");
        check("admin session active",
                SessionManager.getInstance().getCurrentUser()
                        .getRole() == RoleType.ADMIN);

        officerUserId = userDAO.findByUsername("officer1").getId();
        userService.unlockAccount(officerUserId);
        check("unlock resets status and attempts",
                userDAO.findByUsername("officer1").isActive());

        User created = userService.registerUser("zztest_officer", "Officer@123",
                "Zz Test Officer", "zztest@resqhub.org", "9800000001",
                RoleType.RESCUE_OFFICER);
        zzOfficerId = created.getId();
        check("new officer registered by admin", zzOfficerId != null);

        expectInvalidUser("duplicate username rejected",
                () -> userService.registerUser("zztest_officer", "Officer@123",
                        "Dup User", "dup@resqhub.org", null, RoleType.CITIZEN));

        expectInvalidUser("weak password rejected",
                () -> userService.registerUser("zztest_weak", "short",
                        "Weak User", "weak@resqhub.org", null, RoleType.CITIZEN));
    }

    // ------------------------------------------------------------------
    // 3. Non-admin cannot register users; role checks work
    // ------------------------------------------------------------------
    private static void testRoleEnforcement() throws Exception {
        authService.logout();
        authService.login("zztest_officer", "Officer@123");

        expectUnauthorized("non-admin cannot register users",
                () -> userService.registerUser("zztest_hack", "Hacker@123",
                        "Hack Attempt", "hack@resqhub.org", null, RoleType.ADMIN));

        check("hasRole varargs matches own role",
                SessionManager.getInstance()
                        .hasRole(RoleType.RESCUE_OFFICER, RoleType.CAMP_MANAGER));
        check("requireRole accepts permitted role",
                requireRoleSucceeds(RoleType.RESCUE_OFFICER));
        check("requireRole rejects unpermitted role",
                !requireRoleSucceeds(RoleType.BLOOD_COORDINATOR));
    }

    private static boolean requireRoleSucceeds(RoleType... roles) {
        try {
            SessionManager.getInstance().requireRole(roles);
            return true;
        } catch (UnauthorizedOperationException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 4. Disaster validation and creation
    // ------------------------------------------------------------------
    private static void testDisasterValidation() throws Exception {
        var severe = disasterService.createDisaster("ZZTEST Severe Flood",
                DisasterType.FLOOD, DisasterSeverity.SEVERE,
                "ZZTEST valley", 500,
                LocalDateTime.now().minusHours(2), null,
                "created by Phase5Test");
        severeDisasterId = severe.getId();
        check("severe disaster created", severeDisasterId != null);

        var low = disasterService.createDisaster("ZZTEST Minor Fire",
                DisasterType.FIRE, DisasterSeverity.LOW,
                "ZZTEST market", 10,
                LocalDateTime.now().minusMinutes(30), null, null);
        lowDisasterId = low.getId();
        check("low disaster created", lowDisasterId != null);

        expectInvalidDisaster("blank title rejected",
                () -> disasterService.createDisaster("   ", DisasterType.FLOOD,
                        DisasterSeverity.LOW, "somewhere", 0,
                        LocalDateTime.now(), null, null));

        expectInvalidDisaster("end before start rejected",
                () -> disasterService.createDisaster("ZZTEST Backwards Time",
                        DisasterType.CYCLONE, DisasterSeverity.MODERATE,
                        "coast", 0,
                        LocalDateTime.now(),
                        LocalDateTime.now().minusDays(1), null));
    }

    // ------------------------------------------------------------------
    // 5. Victim validation
    // ------------------------------------------------------------------
    private static void testVictimValidation() throws Exception {
        expectInvalidVictim("age out of range rejected",
                () -> victimService.registerVictim("ZZTEST Ancient", 500,
                        Gender.MALE, null, null, null, null,
                        "ZZTEST camp", severeDisasterId));

        var victim = victimService.registerVictim("ZZTEST Victim One", 8,
                Gender.FEMALE, null, null, "asthma", "mother present",
                "ZZTEST rooftop", severeDisasterId);
        victimId = victim.getId();
        check("victim registered with defaults SAFE/NOT_SHELTERED",
                victimId != null
                && victim.getEmergencyStatus().name().equals("SAFE"));
    }

    // ------------------------------------------------------------------
    // 6. The rescue priority algorithm on live data
    // ------------------------------------------------------------------
    private static void testPriorityComputation() throws Exception {
        RescueRequest critical = requestService.submitRequest(severeDisasterId,
                victimId, "ZZTEST Critical Caller", "9711100001",
                "ZZTEST submerged lane", 4, 2, 0,
                true, true, false, "boat evacuation needed");
        criticalRequestId = critical.getId();

        System.out.println(requestService.explainPriority(critical, severeDisasterId));

        check("life+medical+children+severe rates CRITICAL",
                critical.getPriority() == PriorityLevel.CRITICAL);

        RescueRequest reloaded = requestDao.findById(criticalRequestId);
        check("CRITICAL priority persisted to database",
                reloaded.getPriority() == PriorityLevel.CRITICAL);

        RescueRequest mild = requestService.submitRequest(lowDisasterId,
                null, "ZZTEST Mild Caller", "9711100002",
                "ZZTEST dry street", 1, 0, 0,
                false, false, false, "water bottles");
        lowRequestId = mild.getId();
        check("single person, no flags, low disaster rates LOW",
                mild.getPriority() == PriorityLevel.LOW);

        expectInvalidRequest("children exceeding people count rejected",
                () -> requestService.submitRequest(severeDisasterId, null,
                        "ZZTEST Bad Counts", "9711100003", "ZZTEST nowhere",
                        2, 3, 0, false, false, false, null));

        List<RescueRequest> queue = requestService.getPendingQueue();
        check("pending queue contains both test requests",
                containsId(queue, criticalRequestId)
                && containsId(queue, lowRequestId));
    }

    // ------------------------------------------------------------------
    // 7. Assignment lifecycle through the transactional service methods
    // ------------------------------------------------------------------
    private static void testAssignmentLifecycle() throws Exception {
        var team = teamService.registerTeam("ZZTEST Response Unit",
                TeamType.COMMUNITY, "Zz Leader", "9611100000", 6,
                "first aid", "stretchers", "ZZTEST depot");
        teamId = team.getId();
        check("team registered AVAILABLE", teamId != null && team.isAvailable());

        expectInvalidTeam("duplicate team name rejected",
                () -> teamService.registerTeam("ZZTEST Response Unit",
                        TeamType.POLICE, "Another Leader", "9611100099", 3,
                        null, null, null));

        long assignmentId = requestService.assignTeam(lowRequestId, teamId);
        check("assignTeam returned an assignment id", assignmentId > 0);
        check("request moved to ASSIGNED",
                requestDao.findById(lowRequestId).getStatus()
                        == RequestStatus.ASSIGNED);
        check("team moved to DEPLOYED", !teamService.getAllTeams().stream()
                .filter(t -> t.getId().equals(teamId)).findFirst().get()
                .isAvailable());

        expectOperationNotAllowed("cannot assign the same busy team again",
                () -> requestService.assignTeam(criticalRequestId, teamId));

        requestService.progressAssignment(assignmentId, AssignmentStatus.EN_ROUTE,
                "left depot");
        requestService.progressAssignment(assignmentId, AssignmentStatus.ON_SITE,
                "arrived");
        check("assignment progressed EN_ROUTE then ON_SITE",
                assignmentDao.findById(assignmentId).getAssignmentStatus()
                        == AssignmentStatus.ON_SITE);

        expectOperationNotAllowed("skipping backwards is illegal",
                () -> requestService.progressAssignment(assignmentId,
                        AssignmentStatus.EN_ROUTE, "illegal jump"));

        requestService.completeAssignment(assignmentId);
        check("completion set request RESCUED",
                requestDao.findById(lowRequestId).getStatus()
                        == RequestStatus.RESCUED);
        check("completion released team to AVAILABLE",
                teamService.getAllTeams().stream()
                        .filter(t -> t.getId().equals(teamId)).findFirst().get()
                        .isAvailable());

        expectOperationNotAllowed("completing twice is illegal",
                () -> requestService.completeAssignment(assignmentId));

        requestService.cancelRequest(criticalRequestId);
        check("critical request cancelled cleanly",
                requestDao.findById(criticalRequestId).getStatus()
                        == RequestStatus.CANCELLED);
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------
    private interface ThrowingCall {
        void run() throws Exception;
    }

    private static void expectAuthenticationFailure(String label, ThrowingCall call) {
        expectException(label, AuthenticationException.class, call);
    }

    private static void expectUnauthorized(String label, ThrowingCall call) {
        expectException(label, UnauthorizedOperationException.class, call);
    }

    private static void expectInvalidUser(String label, ThrowingCall call) {
        expectException(label, InvalidUserDataException.class, call);
    }

    private static void expectInvalidDisaster(String label, ThrowingCall call) {
        expectException(label, InvalidDisasterDataException.class, call);
    }

    private static void expectInvalidVictim(String label, ThrowingCall call) {
        expectException(label, InvalidVictimDataException.class, call);
    }

    private static void expectInvalidRequest(String label, ThrowingCall call) {
        expectException(label, InvalidRescueRequestException.class, call);
    }

    private static void expectInvalidTeam(String label, ThrowingCall call) {
        expectException(label, InvalidTeamDataException.class, call);
    }

    private static void expectOperationNotAllowed(String label, ThrowingCall call) {
        expectException(label, OperationNotAllowedException.class, call);
    }

    private static void expectException(String label,
                                        Class<? extends Exception> expected,
                                        ThrowingCall call) {
        try {
            call.run();
            check(label + " [" + expected.getSimpleName() + "]", false);
        } catch (Exception thrown) {
            check(label + " [" + expected.getSimpleName() + "]",
                  expected.isInstance(thrown));
            if (!expected.isInstance(thrown)) {
                System.out.println("   got: " + thrown.getClass().getSimpleName()
                        + ": " + thrown.getMessage());
            }
        }
    }

    private static boolean containsId(List<RescueRequest> requests, Long id) {
        for (RescueRequest r : requests) {
            if (r.getId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Resets seed accounts and removes ZZTEST leftovers from earlier runs. */
    private static void preClean() throws Exception {
        try (var con = DatabaseConnectionManager.getInstance().getConnection();
             var st = con.createStatement()) {
            st.executeUpdate("UPDATE users SET account_status='ACTIVE', "
                    + "failed_login_attempts=0 WHERE username IN ('officer1')");
            st.executeUpdate("DELETE FROM rescue_assignments WHERE rescue_request_id IN "
                    + "(SELECT id FROM rescue_requests WHERE requester_name LIKE 'ZZTEST%')");
            st.executeUpdate("DELETE FROM rescue_requests "
                    + "WHERE requester_name LIKE 'ZZTEST%'");
            st.executeUpdate("DELETE FROM victims WHERE full_name LIKE 'ZZTEST%'");
            st.executeUpdate("DELETE FROM rescue_teams WHERE team_name LIKE 'ZZTEST%'");
            st.executeUpdate("DELETE FROM disasters WHERE title LIKE 'ZZTEST%'");
            st.executeUpdate("DELETE FROM users WHERE username LIKE 'zztest_%'");
        }
    }

    private static void cleanUp() {
        silentDelete(() -> {
            if (lowRequestId != null) {
                List<com.resqhub.model.RescueAssignment> assignments =
                        assignmentDao.findByRequest(lowRequestId);
                for (int i = 0; i < assignments.size(); i++) {
                    assignmentDao.deleteById(assignments.get(i).getId());
                }
            }
        });
        silentDelete(() -> { if (lowRequestId != null) requestDao.deleteById(lowRequestId); });
        silentDelete(() -> { if (criticalRequestId != null) requestDao.deleteById(criticalRequestId); });
        silentDelete(() -> { if (victimId != null) new com.resqhub.dao.VictimDAO().deleteById(victimId); });
        silentDelete(() -> { if (teamId != null) new com.resqhub.dao.RescueTeamDAO().deleteById(teamId); });
        silentDelete(() -> { if (lowDisasterId != null) disasterService.getAllDisasters(); });
        silentDelete(() -> {
            if (lowDisasterId != null) new com.resqhub.dao.DisasterDAO().deleteById(lowDisasterId);
            if (severeDisasterId != null) new com.resqhub.dao.DisasterDAO().deleteById(severeDisasterId);
        });
        silentDelete(() -> { if (zzOfficerId != null) userDAO.deleteById(zzOfficerId); });
        silentDelete(() -> {
            try (var con = DatabaseConnectionManager.getInstance().getConnection();
                 var st = con.createStatement()) {
                st.executeUpdate("UPDATE users SET account_status='ACTIVE', "
                        + "failed_login_attempts=0 WHERE username='officer1'");
            }
        });
    }

    private interface SilentAction {
        void run() throws Exception;
    }

    private static void silentDelete(SilentAction action) {
        try {
            action.run();
        } catch (Exception ignored) {
            // cleanup best-effort
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
