package com.resqhub.config;

import com.resqhub.service.NotificationService;
import com.resqhub.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    public GlobalModelAttributes(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @ModelAttribute("unreadNotificationCount")
    public int unreadNotificationCount(Authentication auth) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            try {
                return userService.findByUsername(auth.getName())
                        .map(u -> notificationService.getUnreadCount(u.getId(), u.getRole()))
                        .orElse(0);
            } catch (Exception ignored) {
                return 0;
            }
        }
        return 0;
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "";
    }

    @ModelAttribute("activePerspective")
    public String activePerspective(HttpServletRequest request, Authentication auth) {
        return resolvePerspective(request, auth);
    }

    @ModelAttribute("effectiveRole")
    public String effectiveRole(HttpServletRequest request, Authentication auth) {
        return resolveEffectiveRole(request, auth);
    }

    @ModelAttribute("showRescueOps")
    public boolean showRescueOps(HttpServletRequest request, Authentication auth) {
        String role = resolveEffectiveRole(request, auth);
        return "ADMIN".equals(role) || "RESCUE_OFFICER".equals(role);
    }

    @ModelAttribute("showCitizenServices")
    public boolean showCitizenServices(HttpServletRequest request, Authentication auth) {
        String role = resolveEffectiveRole(request, auth);
        return "CITIZEN".equals(role);
    }

    @ModelAttribute("showShelterOps")
    public boolean showShelterOps(HttpServletRequest request, Authentication auth) {
        String role = resolveEffectiveRole(request, auth);
        return "ADMIN".equals(role) || "CAMP_MANAGER".equals(role);
    }

    @ModelAttribute("showVolunteerOps")
    public boolean showVolunteerOps(HttpServletRequest request, Authentication auth) {
        String role = resolveEffectiveRole(request, auth);
        return "ADMIN".equals(role) || "CAMP_MANAGER".equals(role) || "VOLUNTEER".equals(role);
    }

    @ModelAttribute("showVolunteerRegistry")
    public boolean showVolunteerRegistry(HttpServletRequest request, Authentication auth) {
        String role = resolveEffectiveRole(request, auth);
        return "ADMIN".equals(role) || "CAMP_MANAGER".equals(role);
    }

    @ModelAttribute("showResourceOps")
    public boolean showResourceOps(HttpServletRequest request, Authentication auth) {
        String role = resolveEffectiveRole(request, auth);
        return "ADMIN".equals(role) || "CAMP_MANAGER".equals(role);
    }

    @ModelAttribute("showAdminOps")
    public boolean showAdminOps(HttpServletRequest request, Authentication auth) {
        String role = resolveEffectiveRole(request, auth);
        return "ADMIN".equals(role);
    }

    @ModelAttribute("dashboardUrl")
    public String dashboardUrl(HttpServletRequest request, Authentication auth) {
        String role = resolveEffectiveRole(request, auth);
        if ("CITIZEN".equals(role)) return "/dashboard/citizen";
        if ("RESCUE_OFFICER".equals(role)) return "/dashboard/rescue-officer";
        if ("CAMP_MANAGER".equals(role)) return "/dashboard/camp-manager";
        if ("VOLUNTEER".equals(role)) return "/dashboard/volunteer";
        return "/dashboard/admin";
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private String resolvePerspective(HttpServletRequest request, Authentication auth) {
        if (!isAdmin(auth)) {
            return null;
        }

        String uri = request != null ? request.getRequestURI() : "";
        jakarta.servlet.http.HttpSession session = request != null ? request.getSession(true) : null;

        // Synchronize on dashboard direct routes immediately to eliminate 1-request lag
        if ("/dashboard/admin".equals(uri) || "/dashboard/exit-preview".equals(uri)) {
            if (session != null) {
                session.removeAttribute("adminPerspective");
            }
            return null;
        } else if ("/dashboard/rescue-officer".equals(uri)) {
            if (session != null) session.setAttribute("adminPerspective", "RESCUE_OFFICER");
            return "RESCUE_OFFICER";
        } else if ("/dashboard/camp-manager".equals(uri)) {
            if (session != null) session.setAttribute("adminPerspective", "CAMP_MANAGER");
            return "CAMP_MANAGER";
        } else if ("/dashboard/volunteer".equals(uri)) {
            if (session != null) session.setAttribute("adminPerspective", "VOLUNTEER");
            return "VOLUNTEER";
        } else if ("/dashboard/citizen".equals(uri)) {
            if (session != null) session.setAttribute("adminPerspective", "CITIZEN");
            return "CITIZEN";
        }

        // On any other page, check session
        if (session != null) {
            Object p = session.getAttribute("adminPerspective");
            if (p != null) {
                return p.toString();
            }
        }
        return null;
    }

    private String resolveEffectiveRole(HttpServletRequest request, Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return "ANONYMOUS";
        }

        if (isAdmin(auth)) {
            String perspective = resolvePerspective(request, auth);
            if (perspective != null) {
                return perspective;
            }
            return "ADMIN";
        }

        // Real non-admin user logged in:
        return auth.getAuthorities().stream()
                .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("CITIZEN");
    }
}

