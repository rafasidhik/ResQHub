package com.resqhub.spring.controller;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.NotificationController;
import com.resqhub.controller.StatsController;
import com.resqhub.model.Notification;
import com.resqhub.model.RoleType;
import com.resqhub.service.SessionManager;

@Controller
public class DashboardWebController extends BaseWebController {

    private final StatsController statsController = new StatsController();
    private final NotificationController notificationController = new NotificationController();

    @GetMapping("/app/dashboard")
    public String dashboard(Model model) throws Exception {
        SessionManager session = SessionManager.getInstance();
        if (session.hasRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER, RoleType.CAMP_MANAGER)) {
            ActionResult r = statsController.getSnapshot();
            if (r.isSuccess() && r.getData() != null) {
                model.addAttribute("s", asMap(r.getData()));
            } else {
                model.addAttribute("statsError", r.getMessage());
            }
        }
        List<Notification> mine = notificationController.getMyNotifications();
        model.addAttribute("recent", mine.size() > 5 ? mine.subList(0, 5) : mine);
        model.addAttribute("pageTitle", "Dashboard");
        return "dashboard";
    }

    private static Map<String, Integer> asMap(Object snapshot) throws IllegalAccessException {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Field f : snapshot.getClass().getFields()) {
            if (f.getType() == int.class) {
                out.put(f.getName(), f.getInt(snapshot));
            }
        }
        return out;
    }
}
