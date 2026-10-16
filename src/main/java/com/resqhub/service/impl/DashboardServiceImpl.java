package com.resqhub.service.impl;

import com.resqhub.dao.ActivityLogDao;
import com.resqhub.dao.RescueAssignmentDao;
import com.resqhub.dao.VolunteerAssignmentDao;
import com.resqhub.dto.dashboard.*;
import com.resqhub.model.*;
import com.resqhub.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final DisasterService disasterService;
    private final RescueService rescueService;
    private final ShelterService shelterService;
    private final ResourceService resourceService;
    private final VolunteerService volunteerService;
    private final DonationService donationService;
    private final ActivityLogDao activityLogDao;
    private final RescueAssignmentDao rescueAssignmentDao;
    private final VolunteerAssignmentDao volunteerAssignmentDao;

    @Autowired
    public DashboardServiceImpl(JdbcTemplate jdbcTemplate,
                                DisasterService disasterService,
                                RescueService rescueService,
                                ShelterService shelterService,
                                ResourceService resourceService,
                                VolunteerService volunteerService,
                                DonationService donationService,
                                ActivityLogDao activityLogDao,
                                RescueAssignmentDao rescueAssignmentDao,
                                VolunteerAssignmentDao volunteerAssignmentDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.disasterService = disasterService;
        this.rescueService = rescueService;
        this.shelterService = shelterService;
        this.resourceService = resourceService;
        this.volunteerService = volunteerService;
        this.donationService = donationService;
        this.activityLogDao = activityLogDao;
        this.rescueAssignmentDao = rescueAssignmentDao;
        this.volunteerAssignmentDao = volunteerAssignmentDao;
    }

    @Override
    public AdminDashboardData getAdminDashboardData() {
        AdminDashboardData data = new AdminDashboardData();

        // Disasters
        Integer totalDisasters = queryCount("SELECT COUNT(*) FROM disasters");
        Integer activeDisasters = queryCount("SELECT COUNT(*) FROM disasters WHERE status = 'ACTIVE'");
        Integer criticalDisasters = queryCount("SELECT COUNT(*) FROM disasters WHERE severity = 'CRITICAL' AND status = 'ACTIVE'");
        data.setTotalDisasters(totalDisasters);
        data.setActiveDisasters(activeDisasters);
        data.setCriticalDisasters(criticalDisasters);

        // Victims
        Integer totalVictims = queryCount("SELECT COUNT(*) FROM victims");
        Integer criticalVictims = queryCount("SELECT COUNT(*) FROM victims WHERE priority = 'CRITICAL'");
        Integer rescuedVictims = queryCount("SELECT COUNT(*) FROM victims WHERE rescue_status = 'RESCUED'");
        Integer awaitingVictims = queryCount("SELECT COUNT(*) FROM victims WHERE rescue_status IN ('REPORTED', 'AWAITING_RESCUE', 'IN_RESCUE')");
        data.setTotalVictims(totalVictims);
        data.setCriticalVictims(criticalVictims);
        data.setRescuedVictims(rescuedVictims);
        data.setAwaitingRescueVictims(awaitingVictims);

        // Rescue Teams
        Integer totalTeams = queryCount("SELECT COUNT(*) FROM rescue_teams");
        Integer availableTeams = queryCount("SELECT COUNT(*) FROM rescue_teams WHERE availability_status = 'AVAILABLE'");
        Integer deployedTeams = queryCount("SELECT COUNT(*) FROM rescue_teams WHERE availability_status = 'DEPLOYED'");
        data.setTotalRescueTeams(totalTeams);
        data.setAvailableRescueTeams(availableTeams);
        data.setDeployedRescueTeams(deployedTeams);

        // Shelters
        int totalShelters = (int) shelterService.getTotalShelterCount();
        int totalCapacity = shelterService.getTotalCapacity();
        int currentOccupancy = shelterService.getCurrentOccupancy();
        int occupancyRate = shelterService.getOverallOccupancyPercentage();
        data.setTotalShelters(totalShelters);
        data.setTotalShelterCapacity(totalCapacity);
        data.setCurrentShelterOccupancy(currentOccupancy);
        data.setShelterOccupancyPercentage(occupancyRate);

        // Resources
        data.setTotalInventoryItems((int) resourceService.getTotalItemCount());
        data.setLowStockCount(resourceService.getLowStockCount());

        // Volunteers
        data.setTotalVolunteers((int) volunteerService.getTotalVolunteerCount());
        data.setActiveVolunteers(volunteerService.getAvailableCount());
        data.setPendingVolunteers(volunteerService.getPendingApprovalCount());

        // Donations
        data.setTotalMonetaryFunds(donationService.getTotalMonetaryFunds());
        data.setTotalMaterialUnits(donationService.getTotalMaterialUnits());

        // Recent Activity Logs & Requests
        data.setRecentActivityLogs(activityLogDao.findRecent(8));
        data.setRecentRescueRequests(rescueService.getRequests(0, 5, null, null, null, null));

        // Chart Data: Disaster Severity breakdown
        Map<String, Integer> severityMap = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT severity, COUNT(*) as cnt FROM disasters GROUP BY severity", rs -> {
            severityMap.put(rs.getString("severity"), rs.getInt("cnt"));
        });
        data.setDisasterSeverityCounts(severityMap);

        // Chart Data: Disaster Type breakdown
        Map<String, Integer> typeMap = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT disaster_type, COUNT(*) as cnt FROM disasters GROUP BY disaster_type", rs -> {
            typeMap.put(rs.getString("disaster_type"), rs.getInt("cnt"));
        });
        data.setDisasterTypeCounts(typeMap);

        // Chart Data: Shelters occupancy comparison
        List<Map<String, Object>> shelterChart = jdbcTemplate.query(
                "SELECT name, total_capacity, current_occupancy FROM shelters ORDER BY total_capacity DESC LIMIT 6",
                (rs, rowNum) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", rs.getString("name"));
                    map.put("capacity", rs.getInt("total_capacity"));
                    map.put("currentOccupants", rs.getInt("current_occupancy"));
                    return map;
                }
        );
        data.setShelterOccupancyChartData(shelterChart);

        // Chart Data: Inventory Category items
        Map<String, Integer> catMap = new LinkedHashMap<>();
        jdbcTemplate.query("SELECT category, COUNT(*) as cnt FROM inventory_items GROUP BY category", rs -> {
            catMap.put(rs.getString("category"), rs.getInt("cnt"));
        });
        data.setInventoryCategoryCounts(catMap);

        return data;
    }

    @Override
    public RescueOfficerDashboardData getRescueOfficerDashboardData() {
        RescueOfficerDashboardData data = new RescueOfficerDashboardData();

        data.setPendingRequestsCount(queryCount("SELECT COUNT(*) FROM rescue_requests WHERE status = 'PENDING'"));
        data.setInProgressMissionsCount(queryCount("SELECT COUNT(*) FROM rescue_requests WHERE status IN ('ASSIGNED', 'IN_PROGRESS')"));
        data.setAvailableTeamsCount(queryCount("SELECT COUNT(*) FROM rescue_teams WHERE availability_status = 'AVAILABLE'"));
        data.setDeployedTeamsCount(queryCount("SELECT COUNT(*) FROM rescue_teams WHERE availability_status = 'DEPLOYED'"));
        data.setCriticalVictimsCount(queryCount("SELECT COUNT(*) FROM victims WHERE priority = 'CRITICAL' AND rescue_status != 'RESCUED'"));

        // Urgent pending requests
        data.setUrgentRequests(rescueService.getRequests(0, 8, null, null, "PENDING", null));
        // Active teams
        data.setActiveTeams(rescueService.getTeams(0, 8, null, null, null));
        // Recent assignments
        List<RescueAssignment> assignments = jdbcTemplate.query(
                "SELECT * FROM rescue_assignments ORDER BY created_at DESC LIMIT 5",
                (rs, rowNum) -> {
                    RescueAssignment a = new RescueAssignment();
                    a.setId(rs.getLong("id"));
                    a.setRequestId(rs.getLong("request_id"));
                    a.setTeamId(rs.getLong("team_id"));
                    a.setStatus(RescueAssignmentStatus.valueOf(rs.getString("status")));
                    a.setStartTime(rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toLocalDateTime() : null);
                    a.setActualCompletion(rs.getTimestamp("actual_completion") != null ? rs.getTimestamp("actual_completion").toLocalDateTime() : null);
                    a.setProgressNotes(rs.getString("progress_notes"));
                    return a;
                }
        );
        data.setRecentAssignments(assignments);

        return data;
    }

    @Override
    public CampManagerDashboardData getCampManagerDashboardData() {
        CampManagerDashboardData data = new CampManagerDashboardData();

        data.setTotalSheltersCount((int) shelterService.getTotalShelterCount());
        data.setTotalCapacity(shelterService.getTotalCapacity());
        data.setCurrentOccupancy(shelterService.getCurrentOccupancy());
        data.setOccupancyPercentage(shelterService.getOverallOccupancyPercentage());

        data.setLowStockItemsCount(resourceService.getLowStockCount());
        data.setTodayDistributionsCount(queryCount("SELECT COUNT(*) FROM food_distributions WHERE DATE(distribution_time) = CURRENT_DATE()"));
        data.setTotalRecipientsServed(resourceService.getTotalRecipientsServed());

        data.setSheltersList(shelterService.getAllShelters());
        data.setLowStockItems(resourceService.getLowStockItems());
        List<FoodDistribution> distributions = resourceService.getAllFoodDistributions();
        if (distributions.size() > 6) {
            distributions = distributions.subList(0, 6);
        }
        data.setRecentDistributions(distributions);

        return data;
    }

    @Override
    public VolunteerDashboardData getVolunteerDashboardData(Long userId) {
        VolunteerDashboardData data = new VolunteerDashboardData();

        Optional<Volunteer> volunteerOpt = volunteerService.getVolunteerByUserId(userId);
        if (volunteerOpt.isPresent()) {
            Volunteer v = volunteerOpt.get();
            data.setVolunteer(v);
            data.setApprovalStatus(v.getApprovalStatus());
            data.setAvailability(v.getAvailabilityStatus());

            List<VolunteerAssignment> myAssignments = volunteerService.getAssignmentsByVolunteer(v.getId());
            data.setMyAssignments(myAssignments);

            int assigned = 0;
            int inProgress = 0;
            int completed = 0;
            for (VolunteerAssignment a : myAssignments) {
                if (a.getStatus() == VolunteerAssignmentStatus.ACCEPTED || a.getStatus() == VolunteerAssignmentStatus.PENDING || a.getStatus() == VolunteerAssignmentStatus.OFFERED) assigned++;
                else if (a.getStatus() == VolunteerAssignmentStatus.IN_PROGRESS) inProgress++;
                else if (a.getStatus() == VolunteerAssignmentStatus.COMPLETED) completed++;
            }
            data.setAssignedTasksCount(assigned);
            data.setInProgressTasksCount(inProgress);
            data.setCompletedTasksCount(completed);
        } else {
            data.setApprovalStatus(VolunteerApprovalStatus.PENDING);
            data.setAvailability(VolunteerAvailability.UNAVAILABLE);
            data.setMyAssignments(Collections.emptyList());
        }

        // Open available tasks
        List<VolunteerAssignment> openMissions = volunteerAssignmentDao.findAll();
        if (openMissions.size() > 5) {
            openMissions = openMissions.subList(0, 5);
        }
        data.setOpenMissions(openMissions);

        return data;
    }

    @Override
    public CitizenDashboardData getCitizenDashboardData(Long userId) {
        CitizenDashboardData data = new CitizenDashboardData();

        data.setMyRequestsCount(rescueService.countRequestsByCitizen(userId));
        data.setMyRequests(rescueService.getRequestsByCitizen(userId, 0, 5));

        List<Disaster> activeDisasters = disasterService.getActiveDisasters();
        data.setActiveDisastersCount(activeDisasters.size());
        data.setActiveDisasters(activeDisasters);

        List<Shelter> openShelters = shelterService.getAvailableShelters();
        data.setOpenSheltersCount(openShelters.size());
        if (openShelters.size() > 6) {
            openShelters = openShelters.subList(0, 6);
        }
        data.setOpenShelters(openShelters);

        return data;
    }

    private int queryCount(String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
