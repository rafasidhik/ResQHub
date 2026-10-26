package com.resqhub.spring.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.NotificationController;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationPriority;
import com.resqhub.model.NotificationStatus;
import com.resqhub.model.NotificationType;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.service.SessionManager;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class NotificationWebController extends BaseWebController {

    private final NotificationController notificationController = new NotificationController();

    @GetMapping("/app/notifications")
    public String notifications(@RequestParam(value = "type", required = false) String typeStr,
                                @RequestParam(value = "priority", required = false) String priorityStr,
                                @RequestParam(value = "status", required = false) String statusStr,
                                Model model) throws Exception {

        NotificationType type = null;
        if (typeStr != null && !typeStr.isBlank()) {
            try { type = NotificationType.valueOf(typeStr); } catch (Exception ignored) {}
        }
        NotificationPriority priority = null;
        if (priorityStr != null && !priorityStr.isBlank()) {
            try { priority = NotificationPriority.valueOf(priorityStr); } catch (Exception ignored) {}
        }
        NotificationStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try { status = NotificationStatus.valueOf(statusStr); } catch (Exception ignored) {}
        }

        List<Notification> list = (type == null && priority == null && status == null)
                ? notificationController.getMyNotifications() : notificationController.filterMine(type, priority, status);

        User u = SessionManager.getInstance().getCurrentUser();
        boolean canBroadcast = u != null && (u.getRole() == RoleType.ADMIN || u.getRole() == RoleType.RESCUE_OFFICER);
        boolean canRefresh = u != null && (u.getRole() == RoleType.ADMIN || u.getRole() == RoleType.RESCUE_OFFICER || u.getRole() == RoleType.CAMP_MANAGER);

        model.addAttribute("items", list);
        model.addAttribute("headers", NotificationController.tableHeaders());
        model.addAttribute("rows", rows(list, NotificationController::toRow));
        model.addAttribute("types", WebUtil.options(NotificationType.class));
        model.addAttribute("priorities", WebUtil.options(NotificationPriority.class));
        model.addAttribute("statuses", WebUtil.options(NotificationStatus.class));
        model.addAttribute("fType", type);
        model.addAttribute("fPriority", priority);
        model.addAttribute("fStatus", status);
        model.addAttribute("canBroadcast", canBroadcast);
        model.addAttribute("canRefresh", canRefresh);
        model.addAttribute("roleOptions", WebUtil.options(RoleType.class));
        model.addAttribute("pageTitle", "Notifications & Alerts");
        return "notifications";
    }

    @PostMapping("/app/notifications")
    public String handleAction(@RequestParam("action") String action,
                               HttpServletRequest req,
                               RedirectAttributes ra) throws Exception {
        ActionResult r;
        switch (action) {
            case "read" -> r = notificationController.markRead(WebUtil.reqLong(req, "id", "Notification id"));
            case "readAll" -> r = notificationController.markAllRead();
            case "archive" -> r = notificationController.archive(WebUtil.reqLong(req, "id", "Notification id"));
            case "refresh" -> r = notificationController.generateAutomaticAlerts();
            case "broadcast" -> {
                String[] picked = req.getParameterValues("roles");
                List<RoleType> roles = new ArrayList<>();
                if (picked != null) {
                    for (String p : picked) {
                        try {
                            roles.add(RoleType.valueOf(p));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
                r = notificationController.broadcast(
                        WebUtil.reqEnum(req, "bType", NotificationType.class, "Category"),
                        WebUtil.reqEnum(req, "bPriority", NotificationPriority.class, "Priority"),
                        WebUtil.str(req, "message"),
                        WebUtil.opt(req, "relatedModule"),
                        roles.toArray(new RoleType[0])
                );
            }
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:/app/notifications";
            }
        }
        return handleResult(r, ra, "/app/notifications");
    }
}
