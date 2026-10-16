package com.resqhub.service;

import com.resqhub.dto.dashboard.*;

public interface DashboardService {

    AdminDashboardData getAdminDashboardData();

    RescueOfficerDashboardData getRescueOfficerDashboardData();

    CampManagerDashboardData getCampManagerDashboardData();

    VolunteerDashboardData getVolunteerDashboardData(Long userId);

    CitizenDashboardData getCitizenDashboardData(Long userId);
}
