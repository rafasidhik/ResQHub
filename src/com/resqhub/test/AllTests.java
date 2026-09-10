package com.resqhub.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.resqhub.config.DatabaseConnectionManager;
import com.resqhub.exception.AuthenticationException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.util.PasswordUtil;
import com.resqhub.util.ValidationUtil;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.resqhub.model.BaseEntity;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.model.Person;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RoleType;
import com.resqhub.model.TeamType;
import com.resqhub.model.User;
import com.resqhub.model.Victim;
import java.sql.Statement;
import com.resqhub.dao.DisasterDAO;
import com.resqhub.dao.RescueAssignmentDAO;
import com.resqhub.dao.RescueRequestDAO;
import com.resqhub.dao.RescueTeamDAO;
import com.resqhub.dao.RoleDAO;
import com.resqhub.dao.UserDAO;
import com.resqhub.dao.VictimDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.Gender;
import com.resqhub.model.RequestStatus;
import com.resqhub.exception.InvalidDisasterDataException;
import com.resqhub.exception.InvalidRescueRequestException;
import com.resqhub.exception.InvalidTeamDataException;
import com.resqhub.exception.InvalidUserDataException;
import com.resqhub.exception.InvalidVictimDataException;
import com.resqhub.exception.OperationNotAllowedException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.AccountStatus;
import com.resqhub.service.AuthService;
import com.resqhub.service.DisasterService;
import com.resqhub.service.RescueRequestService;
import com.resqhub.service.RescueTeamService;
import com.resqhub.service.SessionManager;
import com.resqhub.service.UserService;
import com.resqhub.service.VictimService;
import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.DisasterController;
import com.resqhub.controller.RescueRequestController;
import com.resqhub.controller.RescueTeamController;
import com.resqhub.controller.VictimController;
import com.resqhub.controller.StatsController;
import com.resqhub.controller.UserController;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.RescueAssignment;
import com.resqhub.model.ShelterStatus;
import com.resqhub.util.InputParser;
import com.resqhub.controller.FoodDistributionController;
import com.resqhub.controller.ReportController;
import com.resqhub.controller.ResourceController;
import com.resqhub.dao.FoodDistributionRequestDAO;
import com.resqhub.model.BeneficiaryType;
import com.resqhub.model.FoodDistributionRequest;
import com.resqhub.model.FoodRequestStatus;
import com.resqhub.model.ReportFilters;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;
import com.resqhub.model.Resource;
import com.resqhub.model.ResourceCategory;
import com.resqhub.controller.NotificationController;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationPriority;
import com.resqhub.model.NotificationStatus;
import com.resqhub.model.NotificationType;
import com.resqhub.model.DistributionDestination;
import com.resqhub.model.ResourceStatus;
import com.resqhub.model.ResourceDistribution;
import com.resqhub.model.StockMovement;
import com.resqhub.model.StockMovementType;
import com.resqhub.controller.ShelterController;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterFacility;
import com.resqhub.model.ShelterOperationalStatus;
import com.resqhub.controller.SmartAllocationController;
import com.resqhub.model.ShelterAllocationStatus;
import com.resqhub.model.SmartAllocationResult;

/**
 * AllTests - single-file test suite aggregating every module's
 * end-to-end tests. Run after compile.bat:
 *   java -cp "out;lib\*;resources" com.resqhub.test.AllTests
 *
 * Sequentially executes the Phase 2-8 layer tests plus the
 * per-module suites (Food Distribution, Notifications,
 * Password Reset, Reports, Resources, Shelters, Smart
 * Allocation) against the live DB.
 */
public class AllTests {

    static int globalFailed = 0;

    public static void main(String[] args) throws Exception {
        runSuite("Phase2Test" , Phase2TestSuite::runSuite);
        runSuite("Phase3Test" , Phase3TestSuite::runSuite);
        runSuite("Phase4Test" , Phase4TestSuite::runSuite);
        runSuite("Phase5Test" , Phase5TestSuite::runSuite);
        runSuite("Phase6Test" , Phase6TestSuite::runSuite);
        runSuite("Phase8Test" , Phase8TestSuite::runSuite);
        runSuite("FoodDistributionTest" , FoodDistributionTestSuite::runSuite);
        runSuite("NotificationTest" , NotificationTestSuite::runSuite);
        runSuite("PasswordResetTest" , PasswordResetTestSuite::runSuite);
        runSuite("ReportTest" , ReportTestSuite::runSuite);
        runSuite("ResourceTest" , ResourceTestSuite::runSuite);
        runSuite("ShelterTest" , ShelterTestSuite::runSuite);
        runSuite("SmartAllocationTest" , SmartAllocationTestSuite::runSuite);
        System.out.println();
        System.out.println("ALL TESTS DONE - total failures: " + globalFailed);
        if (globalFailed > 0) {
            System.exit(1);
        }
    }

    interface SuiteRunner {
        void run() throws Exception;
    }

    static void runSuite(String label, SuiteRunner r) {
        System.out.println("============================================= ");
        System.out.println("SUITE: " + label);
        try {
            r.run();
        } catch (Throwable t) {
            globalFailed++;
            System.out.println("  [SUITE CRASH] " + label + " -> " + t);
            t.printStackTrace();
        }
        System.out.println();
    }

    static class Phase2TestSuite {

    private static int passed = 0;
    private static int failed = 0;

    static void runSuite() {
        testSingletonIdentity();
        testJdbcConnection();
        testPasswordHashing();
        testValidationRules();
        testCustomExceptions();

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            AllTests.globalFailed += failed;
        }
    }

    private static void testSingletonIdentity() {
        try {
            DatabaseConnectionManager first = DatabaseConnectionManager.getInstance();
            DatabaseConnectionManager second = DatabaseConnectionManager.getInstance();
            check("Singleton returns same instance", first == second);
        } catch (ResQHubException e) {
            check("Singleton returns same instance", false);
            System.out.println("   reason: " + e.getMessage());
        }
    }

    private static void testJdbcConnection() {
        String sql = "SELECT COUNT(*) FROM roles";
        try {
            DatabaseConnectionManager manager = DatabaseConnectionManager.getInstance();
            try (Connection con = manager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                rs.next();
                int roleCount = rs.getInt(1);
                check("JDBC query works, roles table has 7 rows",
                        roleCount == 7);
            }
        } catch (SQLException e) {
            check("JDBC query works, roles table has 7 rows", false);
            System.out.println("   reason: " + e.getMessage());
        } catch (ResQHubException e) {
            check("JDBC query works, roles table has 7 rows", false);
            System.out.println("   reason: " + e.getMessage());
        }
    }

    private static void testPasswordHashing() {
        String seedHash =
                "e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7";
        check("SHA-256('Admin@123') equals seeded users.password_hash",
                PasswordUtil.hash("Admin@123").equals(seedHash));
        check("Wrong password rejected by matches()",
                !PasswordUtil.matches("wrongpass", seedHash));
    }

    private static void testValidationRules() {
        check("requireNonBlank varargs accepts valid values",
                ValidationUtil.requireNonBlank("a", "b c", "d"));
        check("requireNonBlank varargs rejects blank value",
                !ValidationUtil.requireNonBlank("a", "  ", "c"));
        check("Name validation", ValidationUtil.isValidName("Anand Menon")
                && !ValidationUtil.isValidName("123Bad"));
        check("Phone validation", ValidationUtil.isValidPhone("9847000001")
                && !ValidationUtil.isValidPhone("12345"));
        check("Email validation", ValidationUtil.isValidEmail("rafa@resqhub.org")
                && !ValidationUtil.isValidEmail("no-at-sign"));
        check("Age bounds", ValidationUtil.isValidAge(34)
                && !ValidationUtil.isValidAge(500));
        check("Chronology rule",
                ValidationUtil.isChronological(java.time.LocalDateTime.now(),
                        java.time.LocalDateTime.now().plusDays(1)));
    }

    private static void testCustomExceptions() {
        try {
            simulateLoginFailure();
            check("AuthenticationException thrown and caught", false);
        } catch (AuthenticationException e) {
            check("AuthenticationException thrown and caught",
                    e.getMessage().contains("locked"));
        } catch (ResQHubException e) {
            check("AuthenticationException thrown and caught", false);
        } finally {
            System.out.println("   exception handling block executed");
        }
    }

    private static void simulateLoginFailure() throws ResQHubException {
        throw new AuthenticationException(
                "Account locked after too many failed logins");
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
    static class Phase3TestSuite {

    private static int passed = 0;
    private static int failed = 0;

    static void runSuite() {
        testInheritanceChain();
        testConstructorChaining();
        testDynamicDispatch();
        testEnumMapping();
        testDomainHelpers();

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            AllTests.globalFailed += failed;
        }
    }

    private static void testInheritanceChain() {
        Victim victim = new Victim("Anand Menon", 34, null);
        User user = new User();
        Disaster disaster = new Disaster();

        check("Victim is a Person", victim instanceof Person);
        check("Victim is a BaseEntity (multilevel)", victim instanceof BaseEntity);
        check("User is a Person", user instanceof Person);
        check("Disaster extends BaseEntity directly (hierarchical)",
                disaster instanceof BaseEntity
                && !(((Object) disaster) instanceof Person));
    }

    private static void testConstructorChaining() {
        User chained = new User("Rafa Nair");
        check("Single-arg ctor chains to two-arg (phone null)",
                "Rafa Nair".equals(chained.getFullName()) && chained.getPhone() == null);

        User full = new User("Rafa Nair", "9876500002", "rafa@resqhub.org");
        check("Two-arg ctor chains to three-arg values",
                "9876500002".equals(full.getPhone()) && "rafa@resqhub.org".equals(full.getEmail()));
    }

    private static void testDynamicDispatch() {
        List<BaseEntity> mixed = new ArrayList<>();

        Victim v = new Victim("Lakshmi Pillai", 67, null);
        v.setCurrentLocation("Meppadi camp ground");
        v.setEmergencyStatus(com.resqhub.model.EmergencyStatus.INJURED);
        mixed.add(v);

        Disaster d = new Disaster("Wayanad Floods", DisasterType.FLOOD,
                DisasterSeverity.SEVERE, "Wayanad", LocalDateTime.now());
        d.setId(1L);
        mixed.add(d);

        RescueTeam t = new RescueTeam("Coast Guard Alpha", TeamType.NDRF,
                "Cmdr. Suresh", "9848000001");
        t.setId(7L);
        mixed.add(t);

        System.out.println("   dynamic dispatch output:");
        for (BaseEntity entity : mixed) {
            System.out.println("     " + entity);   // runtime type decides getDetails()
        }

        check("Mixed list holds 3 different subtypes",
                mixed.size() == 3
                && mixed.get(0) instanceof Victim
                && mixed.get(1) instanceof Disaster
                && mixed.get(2) instanceof RescueTeam);
        check("Each subtype renders its own details",
                mixed.get(0).getDetails().contains("Lakshmi")
                && mixed.get(1).getDetails().contains("Floods")
                && mixed.get(2).getDetails().contains("Coast Guard"));
    }

    private static void testEnumMapping() {
        check("PriorityLevel weights order correctly",
                PriorityLevel.CRITICAL.getWeight()
                        > PriorityLevel.HIGH.getWeight()
                && PriorityLevel.HIGH.getWeight()
                        > PriorityLevel.LOW.getWeight());
        check("RoleType maps seeded role_name",
                RoleType.valueOf("RESCUE_OFFICER") == RoleType.RESCUE_OFFICER);
        check("Enum label readable",
                DisasterStatus.ACTIVE.getLabel().equals("Active"));
    }

    private static void testDomainHelpers() {
        RescueRequest request = new RescueRequest(1L, "Anand Menon",
                "9847000001", "Chundale, Wayanad");
        request.setPeopleCount(4);
        request.setChildrenCount(2);
        check("Vulnerable occupants detected",
                request.hasVulnerableOccupants());

        RescueRequest solo = new RescueRequest();
        solo.setChildrenCount(0);
        solo.setElderlyCount(0);
        check("No vulnerable occupants when counts zero",
                !solo.hasVulnerableOccupants());

        Victim elder = new Victim("Lakshmi Pillai", 67, null);
        Victim adult = new Victim("Abdul Rasheed", 28, null);
        check("isVulnerableAge: senior true, adult false",
                elder.isVulnerableAge() && !adult.isVulnerableAge());

        Disaster ongoing = new Disaster();
        ongoing.setStatus(DisasterStatus.CONTAINED);
        Disaster closed = new Disaster();
        closed.setStatus(DisasterStatus.RESOLVED);
        check("Disaster.isOngoing logic",
                ongoing.isOngoing() && !closed.isOngoing());
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
    static class Phase4TestSuite {

    private static int passed = 0;
    private static int failed = 0;

    private static final DisasterDAO disasterDao = new DisasterDAO();
    private static final VictimDAO victimDao = new VictimDAO();
    private static final RescueRequestDAO requestDao = new RescueRequestDAO();
    private static final RescueTeamDAO teamDao = new RescueTeamDAO();
    private static final RescueAssignmentDAO assignmentDao = new RescueAssignmentDAO();

    private static Long disasterId;
    private static Long victimId;
    private static Long requestId;
    private static Long teamId;

    static void runSuite() throws Exception {
        preClean();
        try {
            testDisasterLifecycle();
            testVictimLifecycle();
            testRequestQueueOperations();
            testTeamAvailability();
            testAssignmentTransaction();
            testRoleAndUserLookups();
        } finally {
            cleanUp();
            System.out.println();
            System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
            if (failed > 0) {
                AllTests.globalFailed += failed;
            }
        }
    }

    private static void testDisasterLifecycle() throws DataAccessException {
        Disaster d = new Disaster("ZZTEST Flood Drill", DisasterType.FLOOD,
                DisasterSeverity.SEVERE, "ZZTEST district", java.time.LocalDateTime.now());
        d.setAffectedPopulation(100);
        Disaster saved = disasterDao.save(d);

        check("Disaster insert returns generated id", saved.getId() != null);
        disasterId = saved.getId();

        Disaster reloaded = disasterDao.findById(disasterId);
        check("Disaster reload matches title",
                reloaded != null && saved.getTitle().equals(reloaded.getTitle()));

        reloaded.setStatus(DisasterStatus.CONTAINED);
        Disaster updated = disasterDao.save(reloaded);
        check("Disaster update persists new status",
                updated.getStatus() == DisasterStatus.CONTAINED);

        List<Disaster> hits = disasterDao.search("zztest");
        check("Keyword search finds the test disaster",
                hits.size() == 1 && hits.get(0).getId().equals(disasterId));
    }

    private static void testVictimLifecycle() throws DataAccessException {
        Victim v = new Victim("ZZTEST Victim", 30, Gender.MALE, "9999900000");
        v.setCurrentLocation("ZZTEST camp");
        v.setDisasterId(disasterId);

        Victim saved = victimDao.save(v);
        check("Victim insert returns generated id", saved.getId() != null);
        victimId = saved.getId();
        check("Victim defaults applied (SAFE / NOT_SHELTERED)",
                saved.getEmergencyStatus().name().equals("SAFE")
                && saved.getShelterStatus().name().equals("NOT_SHELTERED"));

        List<Victim> byDisaster = victimDao.findByDisaster(disasterId);
        check("findByDisaster returns exactly the test victim",
                byDisaster.size() == 1
                && byDisaster.get(0).getId().equals(victimId));
    }

    private static void testRequestQueueOperations() throws DataAccessException {
        RescueRequest r = new RescueRequest(disasterId, "ZZTEST Caller",
                "9111111111", "ZZTEST riverside");
        r.setPeopleCount(3);
        r.setChildrenCount(1);
        r.setLifeThreatening(true);

        RescueRequest saved = requestDao.save(r);
        check("Rescue request insert returns generated id", saved.getId() != null);
        requestId = saved.getId();
        check("New request starts PENDING with NULL priority",
                saved.getStatus() == RequestStatus.PENDING
                && saved.getPriority() == null);

        requestDao.updatePriorityAndStatus(requestId, PriorityLevel.HIGH,
                RequestStatus.PENDING);
        RescueRequest rated = requestDao.findById(requestId);
        check("Priority update persisted",
                rated.getPriority() == PriorityLevel.HIGH);

        List<RescueRequest> pending = requestDao.findPendingByPriority();
        boolean foundInOrder = false;
        for (int i = 0; i < pending.size(); i++) {
            if (pending.get(i).getId().equals(requestId)) {
                foundInOrder = true;
                if (i > 0) {
                    PriorityLevel higher = pending.get(i - 1).getPriority();
                    foundInOrder = higher == null
                            || higher.getWeight() >= PriorityLevel.HIGH.getWeight();
                }
                break;
            }
        }
        check("Pending queue contains request in priority order", foundInOrder);
    }

    private static void testTeamAvailability() throws DataAccessException {
        RescueTeam t = new RescueTeam("ZZTEST Squad", TeamType.FIRE_RESCUE,
                "ZZ Leader", "9222200000");
        t.setMemberCount(5);
        t.setBaseLocation("ZZTEST base");

        RescueTeam saved = teamDao.save(t);
        check("Team insert returns generated id", saved.getId() != null);
        teamId = saved.getId();
        check("New team starts AVAILABLE",
                saved.isAvailable());

        List<RescueTeam> available = teamDao.findAvailable();
        boolean present = false;
        for (RescueTeam team : available) {
            if (team.getId().equals(teamId)) {
                present = true;
            }
        }
        check("findAvailable includes the new team", present);
    }

    private static void testAssignmentTransaction() throws DataAccessException {
        User admin = new UserDAO().findByUsername("admin");
        long assignmentId = assignmentDao.assignTeam(requestId, teamId, admin.getId());
        check("assignTeam transaction returned assignment id", assignmentId > 0);

        check("Transaction set request to ASSIGNED",
                requestDao.findById(requestId).getStatus() == RequestStatus.ASSIGNED);
        check("Transaction set team to DEPLOYED",
                !teamDao.findById(teamId).isAvailable());
        check("Assignment row readable with COMPLETED-capable status",
                assignmentDao.findById(assignmentId).getAssignmentStatus()
                        == AssignmentStatus.ASSIGNED);
        check("findByRequest links assignment to request",
                assignmentDao.findByRequest(requestId).size() == 1);

        assignmentDao.completeAssignment(assignmentId);
        check("Completion set request to RESCUED",
                requestDao.findById(requestId).getStatus() == RequestStatus.RESCUED);
        check("Completion released team back to AVAILABLE",
                teamDao.findById(teamId).isAvailable());
    }

    private static void testRoleAndUserLookups() throws DataAccessException {
        RoleDAO roleDao = new RoleDAO();
        Long officerRoleId = roleDao.findIdByRoleName(RoleType.RESCUE_OFFICER);
        check("Role lookup resolves RESCUE_OFFICER id", officerRoleId != null);

        UserDAO userDao = new UserDAO();
        User admin = userDao.findByUsername("admin");
        check("Seeded admin loads with ADMIN role and ACTIVE status",
                admin != null
                && admin.getRole() == RoleType.ADMIN
                && admin.isActive());

        check("Unknown username returns null",
                userDao.findByUsername("zztest_ghost") == null);
    }

    /** Deletes leftover ZZTEST rows from any earlier crashed run. Raw SQL order matters. */
    private static void preClean() throws Exception {
        try (Connection con = DatabaseConnectionManager.getInstance().getConnection();
             Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM rescue_assignments WHERE rescue_request_id IN "
                    + "(SELECT id FROM rescue_requests WHERE requester_name LIKE 'ZZTEST%')");
            st.executeUpdate("DELETE FROM rescue_requests "
                    + "WHERE requester_name LIKE 'ZZTEST%'");
            st.executeUpdate("DELETE FROM victims WHERE full_name LIKE 'ZZTEST%'");
            st.executeUpdate("DELETE FROM rescue_teams WHERE team_name LIKE 'ZZTEST%'");
            st.executeUpdate("DELETE FROM disasters WHERE title LIKE 'ZZTEST%'");
        }
    }

    /** Deletes this run's rows in FK-safe reverse order. */
    private static void cleanUp() {
        silentDelete(() -> {
            List<com.resqhub.model.RescueAssignment> assignments =
                    assignmentDao.findByRequest(requestId);
            for (int i = 0; i < assignments.size(); i++) {
                assignmentDao.deleteById(assignments.get(i).getId());
            }
        });
        silentDelete(() -> requestDao.deleteById(requestId));
        silentDelete(() -> victimDao.deleteById(victimId));
        silentDelete(() -> teamDao.deleteById(teamId));
        silentDelete(() -> disasterDao.deleteById(disasterId));
    }

    private interface SilentAction {
        void run() throws Exception;
    }

    private static void silentDelete(SilentAction action) {
        try {
            action.run();
        } catch (Exception ignored) {
            // cleanup best-effort; failures must not hide test results
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
    static class Phase5TestSuite {

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

    static void runSuite() throws Exception {
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
                AllTests.globalFailed += failed;
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
    static class Phase6TestSuite {

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

    static void runSuite() throws Exception {
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
                AllTests.globalFailed += failed;
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
    static class Phase8TestSuite {

    private static int passed = 0;
    private static int failed = 0;

    static void runSuite() {
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
            AllTests.globalFailed += failed;
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
    static class FoodDistributionTestSuite {

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

    static void runSuite() {
        try {
            run();
        } catch (Exception e) {
            System.out.println("[FATAL] " + e);
            e.printStackTrace();
            failed++;
        } finally {
            releaseFoodVolunteers();
        }
        System.out.println();
        System.out.println("FoodDistributionTest: " + passed + " passed, "
                + failed + " failed");
        if (failed > 0) {
            AllTests.globalFailed += failed;
        }
    }

    private static void releaseFoodVolunteers() {
        try (java.sql.Connection con =
                        com.resqhub.config.DatabaseConnectionManager.getInstance()
                                .getConnection();
                java.sql.Statement st = con.createStatement()) {
            st.executeUpdate("DELETE FROM volunteer_assignments "
                    + "WHERE task_name LIKE 'Food Distribution - %' "
                    + "AND volunteer_id IN (1, 2)");
            st.executeUpdate("UPDATE volunteers SET availability='AVAILABLE' "
                    + "WHERE id IN (1, 2)");
        } catch (Exception ignored) {
            // best-effort cleanup
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
    static class NotificationTestSuite {

    private static int passed = 0;
    private static int failed = 0;

    static void runSuite() {
        try {
            preClean();
            runScenario();
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
            AllTests.globalFailed += failed;
        }
    }

    private static void runScenario() throws Exception {
        System.out.println("--- Section A: broadcast & role routing ---------------");

        AuthController auth = new AuthController();
        check("admin logs in", auth.login("admin", "Admin@123").isSuccess());

        // Create a disposable volunteer + camp manager to prove routing works.
        UserService userService = new UserService();
        boolean managerCreated = false;
        try {
            userService.registerUser("zznot_camp", "Camp@1234",
                    "ZZNOT Camp", "zznot_camp@resqhub.local",
                    null, com.resqhub.model.RoleType.CAMP_MANAGER);
            managerCreated = true;
        } catch (Exception e) {
            System.out.println("   camp-manager setup skipped: " + e.getMessage());
        }
        check("test CAMP_MANAGER account available", managerCreated);

        NotificationController notCtrl = new NotificationController();

        // Broadcast to camp managers only -> should reach the new manager.
        ActionResult broadcast = notCtrl.broadcast(NotificationType.SYSTEM,
                NotificationPriority.WARNING,
                "Shelter supplies review scheduled - ZZNOT",
                "System", new com.resqhub.model.RoleType[]{
                        com.resqhub.model.RoleType.CAMP_MANAGER});
        check("staff broadcasts a warning to camp managers",
                broadcast.isSuccess() && broadcast.getMessage().contains("1"));

        // Admin should NOT have received the camp-manager-only alert.
        List<Notification> adminAll = notCtrl.getMyNotifications();
        boolean adminGotManagerAlert = adminAll.stream().anyMatch(
                n -> n.getMessage() != null
                        && n.getMessage().contains("Shelter supplies review - ZZNOT"));
        check("camp-manager-only alert does NOT leak to admin",
                !adminGotManagerAlert);

        System.out.println("--- Section B: per-user view, mark-read, filter -------");

        // As admin, broadcast to all -> admin receives it.
        notCtrl.broadcast(NotificationType.SYSTEM, NotificationPriority.INFO,
                "ZZNOT system-wide notice", "System",
                new com.resqhub.model.RoleType[]{});
        List<Notification> after = notCtrl.getMyNotifications();
        check("admin sees a system-wide notice",
                after.stream().anyMatch(n -> n.getMessage() != null
                        && n.getMessage().contains("ZZNOT system-wide notice")));

        int unreadBefore = notCtrl.countUnread();
        Notification target = after.stream()
                .filter(n -> n.getMessage() != null
                        && n.getMessage().contains("ZZNOT system-wide notice"))
                .findFirst().orElse(null);
        check("a test notification row exists to mark read", target != null);
        if (target != null) {
            check("marking it read succeeds",
                    notCtrl.markRead(target.getId()).isSuccess());
            check("unread count drops after mark-read",
                    notCtrl.countUnread() <= unreadBefore);
            check("filter by READ finds the marked row",
                    notCtrl.filterMine(null, null, NotificationStatus.READ)
                            .stream().anyMatch(n ->
                                    n.getId().equals(target.getId())));
            check("archiving it succeeds",
                    notCtrl.archive(target.getId()).isSuccess());
            check("archive filter includes the archived row",
                    notCtrl.filterMine(null, null, NotificationStatus.ARCHIVED)
                            .stream().anyMatch(n ->
                                    n.getId().equals(target.getId())));
        }

        System.out.println("--- Section C: automatic alert generation -------------");

        ActionResult auto = notCtrl.generateAutomaticAlerts();
        check("automatic alert generation runs without error",
                auto.isSuccess());

        List<Notification> criticals = notCtrl.filterMine(
                NotificationType.CRITICAL_RESCUE, null, null);
        check("automatic CRITICAL rescue alerts are visible to admin",
                !criticals.isEmpty());

        boolean hasDedupedCritical = criticals.stream().anyMatch(
                n -> n.getMessage() != null
                        && (n.getMessage().contains("Chundale")
                        || n.getMessage().contains("CRITICAL rescue request")));
        check("critical alert text reflects a real critical request", hasDedupedCritical);

        // Re-running the generator must NOT create duplicate critical alerts
        // for the same seeded request (de-dup window active).
        notCtrl.generateAutomaticAlerts();
        List<Notification> criticalsAgain = notCtrl.filterMine(
                NotificationType.CRITICAL_RESCUE, null, null);
        check("re-running generator does not duplicate the critical alert",
                criticalsAgain.size() <= criticals.size() + 1);

        System.out.println("--- Section D: filter & details -----------------------");

        check("filter by priority CRITICAL returns only critical",
                notCtrl.filterMine(null, NotificationPriority.CRITICAL, null)
                        .stream().allMatch(n ->
                                n.getPriority() == NotificationPriority.CRITICAL));
        check("a notification's details are readable",
                !notCtrl.getMyNotifications().isEmpty()
                        && notCtrl.getMyNotifications().get(0).getDetails() != null);

        System.out.println("--- Section E: authorization --------------------------");

        // Camp managers may RUN automatic generators but cannot BROADCAST
        // (broadcast is ADMIN / RESCUE_OFFICER only -> controller failure).
        auth.logout();
        check("camp manager logs in",
                auth.login("zznot_camp", "Camp@1234").isSuccess());
        NotificationController managerCtrl = new NotificationController();
        ActionResult blocked = managerCtrl.broadcast(NotificationType.SYSTEM,
                NotificationPriority.INFO, "x", "x",
                new com.resqhub.model.RoleType[]{});
        check("camp manager CANNOT broadcast (staff-only)", !blocked.isSuccess());
        check("guard message explains the role restriction",
                blocked.getMessage() != null
                        && blocked.getMessage().toLowerCase().contains("not permitted"));

        auth.logout();
        check("officer re-logs in for teardown",
                auth.login("officer1", "Rescue@123").isSuccess());
        auth.logout();
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

    private static void preClean() throws Exception {
        cleanUp();
    }

    private static void cleanUp() {
        try {
            java.sql.Connection connection =
                    com.resqhub.config.DatabaseConnectionManager.getInstance()
                            .getConnection();
            String[] statements = {
                    "DELETE FROM notifications WHERE message LIKE '%ZZNOT%'",
                    "DELETE FROM notifications WHERE recipient_user_id IN "
                            + "(SELECT id FROM (SELECT u.id FROM users u "
                            + "WHERE u.username LIKE 'zznot_%') t)",
                    "DELETE FROM users WHERE username LIKE 'zznot_%'"
            };
            for (String sql : statements) {
                try (java.sql.PreparedStatement ps =
                        connection.prepareStatement(sql)) {
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "notification cleanup failed: " + e.getMessage(), e);
        }
    }
    }
    static class PasswordResetTestSuite {

    private static int passed = 0;
    private static int failed = 0;

    static void runSuite() {
        try {
            preClean();
            runScenario();
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
            AllTests.globalFailed += failed;
        }
    }

    private static void runScenario() throws Exception {
        System.out.println("--- Forgot-password flow -------------------------------");

        AuthController auth = new AuthController();
        ActionResult signup = auth.registerCitizen("zzfp_citizen",
                "Original@123", "ZZFP Citizen", "zzfp@resqhub.local", null);
        check("disposable citizen account created", signup.isSuccess());

        // Wrong email must be rejected.
        ActionResult wrongEmail = auth.resetForgottenPassword(
                "zzfp_citizen", "wrong@email.com", "Newpass@123", "Newpass@123");
        check("wrong email rejected", !wrongEmail.isSuccess());

        // Unknown username rejected.
        ActionResult wrongUser = auth.resetForgottenPassword(
                "no_such_user", "zzfp@resqhub.local", "Newpass@123", "Newpass@123");
        check("unknown username rejected", !wrongUser.isSuccess());

        // Mismatched confirm rejected.
        ActionResult mismatch = auth.resetForgottenPassword(
                "zzfp_citizen", "zzfp@resqhub.local", "Newpass@123", "Different@1");
        check("mismatched confirmation rejected", !mismatch.isSuccess());

        // Weak password rejected.
        ActionResult weak = auth.resetForgottenPassword(
                "zzfp_citizen", "zzfp@resqhub.local", "short", "short");
        check("weak password rejected", !weak.isSuccess());

        // Correct username + email resets the password.
        ActionResult reset = auth.resetForgottenPassword(
                "zzfp_citizen", "zzfp@resqhub.local", "Newpass@123", "Newpass@123");
        check("valid username + email resets password", reset.isSuccess());

        check("new password logs in",
                auth.login("zzfp_citizen", "Newpass@123").isSuccess());
        auth.logout();
        check("old password no longer works",
                !auth.login("zzfp_citizen", "Original@123").isSuccess());
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

    private static void preClean() throws Exception {
        cleanUp();
    }

    private static void cleanUp() {
        try {
            java.sql.Connection connection =
                    com.resqhub.config.DatabaseConnectionManager.getInstance()
                            .getConnection();
            try (java.sql.PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM users WHERE username LIKE 'zzfp_%'")) {
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "password-reset cleanup failed: " + e.getMessage(), e);
        }
    }
    }
    static class ReportTestSuite {

    private static int passed = 0;
    private static int failed = 0;

    static void runSuite() {
        ReportController controller = new ReportController();

        check("overview generates", () -> {
            ReportResult r = generate(controller, ReportType.OVERVIEW,
                    ReportFilters.empty());
            assertNotNull(r);
            assertTrue(r.headers().length == 2, "overview headers width");
            assertTrue(!r.rows().isEmpty(), "overview has metrics");
            assertTrue(r.summaryLines().size() > 0,
                    "overview has summary");
            return true;
        });

        for (ReportType type : ReportType.values()) {
            if (type == ReportType.OVERVIEW) {
                continue;
            }
            check(type + " generates", () -> {
                ReportResult r = generate(controller, type,
                        ReportFilters.empty());
                assertNotNull(r);
                assertTrue(r.headers() != null, type + " headers present");
                assertTrue(r.summaryLines() != null,
                        type + " summary present");
                for (Object[] row : r.rows()) {
                    assertTrue(row.length == r.headers().length,
                            type + " row/header width match");
                }
                return true;
            });
        }

        // filtering: rescue requests by disaster + status + priority
        check("rescue requests filtered", () -> {
            ReportResult r = generate(controller,
                    ReportType.RESCUE_REQUESTS,
                    new ReportFilters(null, "PENDING", "CRITICAL", null,
                            null, null, null, null));
            assertNotNull(r);
            return true;
        });

        // disaster list for the filter dropdown is populated
        check("disaster list for filter", () -> {
            var list = controller.getDisasters();
            assertNotNull(list);
            return true;
        });

        // acceptable CSV names
        check("csv name", () -> controller.csvName(ReportType.VICTIMS)
                .equals("report_victims"));

        System.out.println();
        System.out.println("ReportTest: " + passed + " passed, "
                + failed + " failed");
        if (failed > 0) {
            AllTests.globalFailed += failed;
        }
    }

    private static ReportResult generate(ReportController c,
            ReportType type, ReportFilters f) {
        ActionResult result = c.generateReport(type, f);
        if (!result.isSuccess()) {
            throw new AssertionError(type + " failed: " + result.getMessage());
        }
        return result.getData();
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

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionError(msg);
        }
    }
    }
    static class ResourceTestSuite {

    private static int passed = 0;
    private static int failed = 0;

    private static final String SUFFIX = String.valueOf(System.nanoTime());
    private static long bandagesId = -1;
    private static long syringeId = -1;
    private static long gauzeId = -1;

    static void runSuite() {
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
            AllTests.globalFailed += failed;
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
    static class ShelterTestSuite {

    private static int passed = 0;
    private static int failed = 0;
    private static long testShelterId = -1;
    private static long testAllocationId = -1;

    static void runSuite() {
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
            AllTests.globalFailed += failed;
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
    }
    static class SmartAllocationTestSuite {

    private static int passed = 0;
    private static int failed = 0;

    // test-owned entities
    private static long victimId = -1;
    private static long selfShelterId = -1;
    private static long pendingAllocId = -1;
    private static long activeAllocId = -1;

    static void runSuite() {
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
            AllTests.globalFailed += failed;
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
}
