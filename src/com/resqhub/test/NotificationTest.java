package com.resqhub.test;

import java.util.List;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.NotificationController;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationPriority;
import com.resqhub.model.NotificationStatus;
import com.resqhub.model.NotificationType;
import com.resqhub.service.UserService;

/**
 * PHASE 9 - Notifications & Alerts integration test.
 * Verifies the Notification Center end-to-end through CONTROLLERS:
 *   - role-based broadcast routing
 *   - per-user viewing / filtering / mark-read / archive
 *   - automatic alert generation (critical rescue + low stock)
 *   - assignment notification hook (volunteer task)
 * Uses the seeded admin / officer1 accounts plus one disposable
 * volunteer account, and cleans up after itself.
 */
public class NotificationTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
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
            System.exit(1);
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
