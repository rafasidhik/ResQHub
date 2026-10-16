package com.resqhub.controller;

import com.resqhub.model.Notification;
import com.resqhub.model.NotificationType;
import com.resqhub.model.User;
import com.resqhub.service.NotificationService;
import com.resqhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    private Optional<User> getCurrentUser(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return userService.findByUsername(auth.getName());
        }
        return Optional.empty();
    }

    @GetMapping
    public String viewNotificationCenter(@RequestParam(value = "type", required = false) String typeFilter,
                                         Authentication auth,
                                         Model model) {
        model.addAttribute("activeMenu", "notifications");

        Optional<User> userOpt = getCurrentUser(auth);
        List<Notification> notifications = Collections.emptyList();
        int unreadCount = 0;

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            unreadCount = notificationService.getUnreadCount(user.getId(), user.getRole());
            notifications = notificationService.getUserNotifications(user.getId(), user.getRole(), 50);

            if (typeFilter != null && !typeFilter.isBlank() && !typeFilter.equalsIgnoreCase("ALL")) {
                try {
                    NotificationType type = NotificationType.valueOf(typeFilter.toUpperCase());
                    notifications = notifications.stream().filter(n -> n.getNotificationType() == type).toList();
                } catch (IllegalArgumentException ignored) {}
            }
        }

        model.addAttribute("notifications", notifications);
        model.addAttribute("unreadCount", unreadCount);
        model.addAttribute("types", NotificationType.values());
        model.addAttribute("selectedType", typeFilter != null ? typeFilter : "ALL");

        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id,
                             @RequestParam(value = "redirect", required = false) String redirectUrl) {
        notificationService.markAsRead(id);
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            return "redirect:" + redirectUrl;
        }
        return "redirect:/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(Authentication auth, RedirectAttributes redirectAttributes) {
        getCurrentUser(auth).ifPresent(user -> {
            notificationService.markAllAsRead(user.getId(), user.getRole());
            redirectAttributes.addFlashAttribute("successMessage", "All notifications marked as read.");
        });
        return "redirect:/notifications";
    }
}
