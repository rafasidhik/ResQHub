package com.resqhub.test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import com.resqhub.config.DatabaseConnectionManager;
import com.resqhub.dao.DisasterDAO;
import com.resqhub.dao.RescueAssignmentDAO;
import com.resqhub.dao.RescueRequestDAO;
import com.resqhub.dao.RescueTeamDAO;
import com.resqhub.dao.RoleDAO;
import com.resqhub.dao.UserDAO;
import com.resqhub.dao.VictimDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.model.Gender;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RequestStatus;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RoleType;
import com.resqhub.model.TeamType;
import com.resqhub.model.User;
import com.resqhub.model.Victim;

/**
 * Phase 4 smoke test - live JDBC integration against the resqhub schema.
 * All rows are marked ZZTEST* and cleaned up automatically.
 * Run after compile.bat:
 *   java -cp "out;lib\*;resources" com.resqhub.test.Phase4Test
 */
public class Phase4Test {

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

    public static void main(String[] args) throws Exception {
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
                System.exit(1);
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
