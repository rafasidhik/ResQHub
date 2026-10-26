package com.resqhub.web;

import com.resqhub.model.RoleType;
import static com.resqhub.model.RoleType.*;

/**
 * Single source of truth for "which role may open which module".
 * Used by BOTH the servlets (server-side enforcement) and the sidebar
 * (what is displayed), so the menu can never promise a page the server
 * would refuse. The matrix mirrors the navigation rules of the original
 * Swing DashboardPanel.
 */
public enum Module {
    DASHBOARD("Dashboard", "bi-speedometer2", "/app/dashboard", "Overview",
            ADMIN, RESCUE_OFFICER, CAMP_MANAGER, VOLUNTEER, CITIZEN),
    DISASTERS("Disasters", "bi-tornado", "/app/disasters", "Operations",
            ADMIN, RESCUE_OFFICER),
    VICTIMS("Victims", "bi-people", "/app/victims", "Operations",
            ADMIN, RESCUE_OFFICER),
    REQUESTS("Rescue Requests", "bi-life-preserver", "/app/requests", "Operations",
            ADMIN, RESCUE_OFFICER),
    REPORT_EMERGENCY("Report Emergency", "bi-exclamation-octagon", "/app/report-emergency", "Operations",
            CITIZEN),
    TEAMS("Rescue Teams", "bi-shield-check", "/app/teams", "Operations",
            ADMIN, RESCUE_OFFICER),
    VOLUNTEERS("Volunteers", "bi-person-heart", "/app/volunteers", "People",
            ADMIN, RESCUE_OFFICER),
    MY_TASKS("My Tasks", "bi-list-check", "/app/my-tasks", "People",
            VOLUNTEER),
    SHELTERS("Shelters", "bi-house-heart", "/app/shelters", "Relief",
            ADMIN, CAMP_MANAGER),
    SMART_ALLOCATION("Smart Allocation", "bi-diagram-3", "/app/smart-allocation", "Relief",
            ADMIN, CAMP_MANAGER),
    RESOURCES("Resources", "bi-box-seam", "/app/resources", "Relief",
            ADMIN, CAMP_MANAGER),
    FOOD("Food Distribution", "bi-basket", "/app/food", "Relief",
            ADMIN, CAMP_MANAGER),
    DONATIONS("Donations", "bi-gift", "/app/donations", "Relief",
            ADMIN, CAMP_MANAGER),
    NOTIFICATIONS("Notifications", "bi-bell", "/app/notifications", "Insights",
            ADMIN, RESCUE_OFFICER, CAMP_MANAGER, VOLUNTEER, CITIZEN),
    REPORTS("Reports", "bi-graph-up", "/app/reports", "Insights",
            ADMIN, RESCUE_OFFICER, CAMP_MANAGER),
    USERS("User Management", "bi-person-gear", "/app/users", "Administration",
            ADMIN);

    private final String label;
    private final String icon;
    private final String path;
    private final String group;
    private final RoleType[] roles;

    Module(String label, String icon, String path, String group, RoleType... roles) {
        this.label = label;
        this.icon = icon;
        this.path = path;
        this.group = group;
        this.roles = roles;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getPath() { return path; }
    public String getGroup() { return group; }
    public RoleType[] roles() { return roles.clone(); }
    public String getKey() { return name(); }

    public boolean allows(RoleType role) {
        for (RoleType r : roles) {
            if (r == role) {
                return true;
            }
        }
        return false;
    }
}
