package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationPriority;
import com.resqhub.model.NotificationStatus;
import com.resqhub.model.NotificationType;
import com.resqhub.model.RoleType;
import com.resqhub.service.NotificationService;

/** Notification Center screen controller. */
public class NotificationController {

    private final NotificationService notificationService =
            new NotificationService();

    // ── creation / broadcast ─────────────────────────────────────────

    public ActionResult broadcast(NotificationType type,
            NotificationPriority priority, String message,
            String relatedModule, RoleType[] roles) {
        try {
            int count = notificationService.broadcast(type, priority,
                    message, relatedModule, roles);
            return ActionResult.success("Alert sent to " + count
                    + " user(s)");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    // ── viewing / filtering ──────────────────────────────────────────

    public List<Notification> getMyNotifications()
            throws DataAccessException {
        return notificationService.getMyNotifications();
    }

    public List<Notification> filterMine(NotificationType type,
            NotificationPriority priority, NotificationStatus status)
            throws DataAccessException {
        return notificationService.filterMine(type, priority, status);
    }

    public int countUnread() throws DataAccessException {
        return notificationService.countUnread();
    }

    // ── status transitions ───────────────────────────────────────────

    public ActionResult markRead(long id) {
        try {
            notificationService.markRead(id);
            return ActionResult.success("Notification marked as read");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult markAllRead() {
        try {
            notificationService.markAllRead();
            return ActionResult.success("All notifications marked as read");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult archive(long id) {
        try {
            notificationService.archive(id);
            return ActionResult.success("Notification archived");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    // ── automatic generation ─────────────────────────────────────────

    public ActionResult generateAutomaticAlerts() {
        try {
            int created = notificationService.generateAutomaticAlerts();
            return ActionResult.success(created == 0
                    ? "No new automatic alerts to generate"
                    : "Generated " + created + " automatic alert(s)");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public Notification get(long id) throws ResQHubException {
        return notificationService.get(id);
    }

    // ── display helpers ──────────────────────────────────────────────

    public static Object[] toRow(Notification n) {
        String icon = switch (n.getPriority() == null
                ? NotificationPriority.INFO : n.getPriority()) {
            case CRITICAL -> "\uD83D\uDEA8";   // 🚨
            case WARNING  -> "\u26A0";          // ⚠
            case INFO     -> "\u2139";          // ℹ
        };
        String stamp = n.getCreatedAt() == null
                ? "-" : n.getCreatedAt().format(
                        java.time.format.DateTimeFormatter
                                .ofPattern("dd-MM-yyyy HH:mm"));
        return new Object[]{
                n.getId(),
                icon + " " + (n.getPriority() == null
                        ? "-" : n.getPriority().getLabel()),
                n.getType() == null ? "-" : n.getType().getLabel(),
                n.getMessage(),
                n.getRelatedModule() == null ? "-" : n.getRelatedModule(),
                n.getStatus() == null ? "-" : n.getStatus().getLabel(),
                stamp
        };
    }

    public static String[] tableHeaders() {
        return new String[]{"ID", "Priority", "Type", "Message",
                "Module", "Status", "Received"};
    }
}
