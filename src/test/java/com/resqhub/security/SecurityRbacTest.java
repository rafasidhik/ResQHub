package com.resqhub.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Spring Security and RBAC Endpoint Authorization Tests")
class SecurityRbacTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Public endpoints are accessible without authentication")
    void testPublicEndpoints() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/volunteer-registration"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonymous access to /app/** redirects to login page")
    void testAnonymousAccessBlocked() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/app/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "citizen_user", roles = {"CITIZEN"})
    @DisplayName("CITIZEN role can access Dashboard & Report Emergency, but is denied from Admin & Staff modules (403)")
    void testCitizenRbac() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/report-emergency"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/app/disasters"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/app/teams"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/app/shelters"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "volunteer_user", roles = {"VOLUNTEER"})
    @DisplayName("VOLUNTEER role can access My Tasks, but is denied from User Management and Disasters (403)")
    void testVolunteerRbac() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/my-tasks"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/app/disasters"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/app/volunteers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("ADMIN role can access administration and operations modules, but is blocked from Citizen-only emergency form (403)")
    void testAdminRbac() throws Exception {
        mockMvc.perform(get("/app/dashboard"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/users"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/disasters"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/shelters"))
                .andExpect(status().isOk());

        // Report emergency is restricted exclusively to CITIZEN role
        mockMvc.perform(get("/app/report-emergency"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "camp_manager", roles = {"CAMP_MANAGER"})
    @DisplayName("CAMP_MANAGER role can access Shelters and Resources, but is denied from Disasters and User Management")
    void testCampManagerRbac() throws Exception {
        mockMvc.perform(get("/app/shelters"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/resources"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/food"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/app/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/app/disasters"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST requests without CSRF token are strictly rejected with 403 Forbidden")
    void testCsrfProtectionEnforced() throws Exception {
        mockMvc.perform(post("/login")
                .param("username", "admin")
                .param("password", "ResQHub@123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Security headers include Content-Security-Policy and X-Frame-Options DENY")
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
