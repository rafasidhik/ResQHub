package com.resqhub.service;

import com.resqhub.dto.dashboard.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DashboardServiceTest {

    @Autowired
    private DashboardService dashboardService;

    @Test
    @DisplayName("Admin dashboard data should aggregate non-null KPIs and charts")
    void testGetAdminDashboardData() {
        AdminDashboardData data = dashboardService.getAdminDashboardData();
        assertNotNull(data);
        assertTrue(data.getTotalDisasters() >= 0);
        assertTrue(data.getActiveDisasters() >= 0);
        assertTrue(data.getTotalShelterCapacity() >= 0);
        assertNotNull(data.getDisasterSeverityCounts());
        assertNotNull(data.getDisasterTypeCounts());
        assertNotNull(data.getShelterOccupancyChartData());
        assertNotNull(data.getInventoryCategoryCounts());
        assertNotNull(data.getTotalMonetaryFunds());
    }

    @Test
    @DisplayName("Rescue officer dashboard data should return operational metrics")
    void testGetRescueOfficerDashboardData() {
        RescueOfficerDashboardData data = dashboardService.getRescueOfficerDashboardData();
        assertNotNull(data);
        assertTrue(data.getPendingRequestsCount() >= 0);
        assertTrue(data.getAvailableTeamsCount() >= 0);
        assertNotNull(data.getUrgentRequests());
        assertNotNull(data.getActiveTeams());
    }

    @Test
    @DisplayName("Camp manager dashboard data should calculate capacity and inventory")
    void testGetCampManagerDashboardData() {
        CampManagerDashboardData data = dashboardService.getCampManagerDashboardData();
        assertNotNull(data);
        assertTrue(data.getTotalSheltersCount() >= 0);
        assertTrue(data.getOccupancyPercentage() >= 0 && data.getOccupancyPercentage() <= 100);
        assertNotNull(data.getSheltersList());
        assertNotNull(data.getLowStockItems());
    }

    @Test
    @DisplayName("Volunteer dashboard data should retrieve volunteer tasks or defaults")
    void testGetVolunteerDashboardData() {
        // Query for volunteer with user ID 4 (or generic user ID 1)
        VolunteerDashboardData data = dashboardService.getVolunteerDashboardData(4L);
        assertNotNull(data);
        assertNotNull(data.getApprovalStatus());
        assertNotNull(data.getAvailability());
        assertTrue(data.getAssignedTasksCount() >= 0);
        assertNotNull(data.getMyAssignments());
    }

    @Test
    @DisplayName("Citizen dashboard data should retrieve citizen requests and open shelters")
    void testGetCitizenDashboardData() {
        // Citizen user ID 5
        CitizenDashboardData data = dashboardService.getCitizenDashboardData(5L);
        assertNotNull(data);
        assertTrue(data.getMyRequestsCount() >= 0);
        assertNotNull(data.getMyRequests());
        assertNotNull(data.getOpenShelters());
        assertNotNull(data.getActiveDisasters());
    }
}
