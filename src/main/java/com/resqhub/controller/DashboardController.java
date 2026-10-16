package com.resqhub.controller;

import com.resqhub.dto.dashboard.*;
import com.resqhub.model.Role;
import com.resqhub.model.User;
import com.resqhub.service.DashboardService;
import com.resqhub.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    @Autowired
    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User currentUser = getCurrentUser(authentication);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeMenu", "dashboard");

        if (isAdmin(authentication) && session != null && session.getAttribute("adminPerspective") != null) {
            String p = (String) session.getAttribute("adminPerspective");
            if ("RESCUE_OFFICER".equals(p)) return renderRescueOfficerDashboard(model);
            if ("CAMP_MANAGER".equals(p)) return renderCampManagerDashboard(model);
            if ("VOLUNTEER".equals(p)) return renderVolunteerDashboard(currentUser != null ? currentUser.getId() : null, model);
            if ("CITIZEN".equals(p)) return renderCitizenDashboard(currentUser != null ? currentUser.getId() : null, model);
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return renderAdminDashboard(model);
            } else if ("ROLE_RESCUE_OFFICER".equals(role)) {
                return renderRescueOfficerDashboard(model);
            } else if ("ROLE_CAMP_MANAGER".equals(role)) {
                return renderCampManagerDashboard(model);
            } else if ("ROLE_VOLUNTEER".equals(role)) {
                return renderVolunteerDashboard(currentUser != null ? currentUser.getId() : null, model);
            } else if ("ROLE_CITIZEN".equals(role)) {
                return renderCitizenDashboard(currentUser != null ? currentUser.getId() : null, model);
            }
        }

        // Fallback default
        return renderCitizenDashboard(currentUser != null ? currentUser.getId() : null, model);
    }

    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDashboard(Authentication authentication, Model model, HttpSession session) {
        if (session != null) {
            session.removeAttribute("adminPerspective");
        }
        model.addAttribute("currentUser", getCurrentUser(authentication));
        model.addAttribute("activeMenu", "dashboard");
        return renderAdminDashboard(model);
    }

    @GetMapping("/dashboard/rescue-officer")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String rescueOfficerDashboard(Authentication authentication, Model model, HttpSession session) {
        if (isAdmin(authentication) && session != null) {
            session.setAttribute("adminPerspective", "RESCUE_OFFICER");
        }
        model.addAttribute("currentUser", getCurrentUser(authentication));
        model.addAttribute("activeMenu", "dashboard");
        return renderRescueOfficerDashboard(model);
    }

    @GetMapping("/dashboard/camp-manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String campManagerDashboard(Authentication authentication, Model model, HttpSession session) {
        if (isAdmin(authentication) && session != null) {
            session.setAttribute("adminPerspective", "CAMP_MANAGER");
        }
        model.addAttribute("currentUser", getCurrentUser(authentication));
        model.addAttribute("activeMenu", "dashboard");
        return renderCampManagerDashboard(model);
    }

    @GetMapping("/dashboard/volunteer")
    @PreAuthorize("hasAnyRole('ADMIN', 'VOLUNTEER')")
    public String volunteerDashboard(Authentication authentication, Model model, HttpSession session) {
        if (isAdmin(authentication) && session != null) {
            session.setAttribute("adminPerspective", "VOLUNTEER");
        }
        User currentUser = getCurrentUser(authentication);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeMenu", "dashboard");
        return renderVolunteerDashboard(currentUser != null ? currentUser.getId() : null, model);
    }

    @GetMapping("/dashboard/citizen")
    @PreAuthorize("hasAnyRole('ADMIN', 'CITIZEN')")
    public String citizenDashboard(Authentication authentication, Model model, HttpSession session) {
        if (isAdmin(authentication) && session != null) {
            session.setAttribute("adminPerspective", "CITIZEN");
        }
        User currentUser = getCurrentUser(authentication);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("activeMenu", "dashboard");
        return renderCitizenDashboard(currentUser != null ? currentUser.getId() : null, model);
    }

    @GetMapping("/dashboard/exit-preview")
    public String exitPreview(HttpSession session) {
        if (session != null) {
            session.removeAttribute("adminPerspective");
        }
        return "redirect:/dashboard/admin";
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private String renderAdminDashboard(Model model) {
        AdminDashboardData data = dashboardService.getAdminDashboardData();
        model.addAttribute("stats", data);
        model.addAttribute("pageTitle", "Unified Command Dashboard — ResQHub");
        return "dashboard/admin";
    }

    private String renderRescueOfficerDashboard(Model model) {
        RescueOfficerDashboardData data = dashboardService.getRescueOfficerDashboardData();
        model.addAttribute("stats", data);
        model.addAttribute("pageTitle", "Field Rescue Command — ResQHub");
        return "dashboard/rescue-officer";
    }

    private String renderCampManagerDashboard(Model model) {
        CampManagerDashboardData data = dashboardService.getCampManagerDashboardData();
        model.addAttribute("stats", data);
        model.addAttribute("pageTitle", "Shelter & Logistics Operations — ResQHub");
        return "dashboard/camp-manager";
    }

    private String renderVolunteerDashboard(Long userId, Model model) {
        VolunteerDashboardData data = dashboardService.getVolunteerDashboardData(userId);
        model.addAttribute("stats", data);
        model.addAttribute("pageTitle", "Volunteer Mission Board — ResQHub");
        return "dashboard/volunteer";
    }

    private String renderCitizenDashboard(Long userId, Model model) {
        CitizenDashboardData data = dashboardService.getCitizenDashboardData(userId);
        model.addAttribute("stats", data);
        model.addAttribute("pageTitle", "Citizen Emergency Portal — ResQHub");
        return "dashboard/citizen";
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) return null;
        Optional<User> userOpt = userService.findByUsername(authentication.getName());
        return userOpt.orElse(null);
    }
}
