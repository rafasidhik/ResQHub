package com.resqhub.service.impl;

import com.resqhub.dao.*;
import com.resqhub.dto.report.IncidentReportDto;
import com.resqhub.model.*;
import com.resqhub.service.DisasterService;
import com.resqhub.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final DisasterService disasterService;
    private final VictimDao victimDao;
    private final RescueRequestDao rescueRequestDao;
    private final ShelterResidentDao shelterResidentDao;
    private final VolunteerAssignmentDao volunteerAssignmentDao;
    private final InventoryDao inventoryDao;
    private final DonationDao donationDao;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    public ReportServiceImpl(DisasterService disasterService,
                             VictimDao victimDao,
                             RescueRequestDao rescueRequestDao,
                             ShelterResidentDao shelterResidentDao,
                             VolunteerAssignmentDao volunteerAssignmentDao,
                             InventoryDao inventoryDao,
                             DonationDao donationDao) {
        this.disasterService = disasterService;
        this.victimDao = victimDao;
        this.rescueRequestDao = rescueRequestDao;
        this.shelterResidentDao = shelterResidentDao;
        this.volunteerAssignmentDao = volunteerAssignmentDao;
        this.inventoryDao = inventoryDao;
        this.donationDao = donationDao;
    }

    @Override
    public IncidentReportDto generateIncidentReport(Long disasterId) {
        IncidentReportDto dto = new IncidentReportDto();

        Disaster disaster = disasterService.getDisasterById(disasterId)
                .orElseThrow(() -> new IllegalArgumentException("Disaster not found with ID: " + disasterId));
        dto.setDisaster(disaster);

        // Victims
        List<Victim> victims = victimDao.findAll(0, 500, null, disasterId, null, null);
        dto.setVictims(victims);
        dto.setTotalVictimsCount(victims.size());
        dto.setRescuedVictimsCount((int) victims.stream().filter(v -> v.getRescueStatus() == RescueStatus.RESCUED).count());
        dto.setCriticalVictimsCount((int) victims.stream().filter(v -> v.getPriority() == VictimPriority.CRITICAL).count());

        // Rescue Requests
        List<RescueRequest> requests = rescueRequestDao.findAll(0, 500, null, disasterId, null, null);
        dto.setRescueRequests(requests);
        dto.setTotalRescueRequestsCount(requests.size());
        dto.setCompletedRescueRequestsCount((int) requests.stream().filter(r -> r.getStatus() == RescueRequestStatus.COMPLETED).count());

        // Shelter Residents
        List<ShelterResident> residents = shelterResidentDao.findByDisasterId(disasterId);
        dto.setShelterResidents(residents);
        dto.setActiveShelterResidentsCount((int) residents.stream().filter(r -> r.getStatus() == ResidentStatus.ACTIVE).count());

        // Volunteer Assignments
        List<VolunteerAssignment> volunteerAssignments = volunteerAssignmentDao.findByDisasterId(disasterId);
        dto.setVolunteerAssignments(volunteerAssignments);

        return dto;
    }

    @Override
    public List<Victim> getVictimRoster(Long disasterId, String status, String priority) {
        return victimDao.findAll(0, 1000, null, disasterId, status, priority);
    }

    @Override
    public List<InventoryItem> getInventoryReport(String category, Boolean lowStockOnly) {
        List<InventoryItem> items;
        if (category != null && !category.isBlank()) {
            items = inventoryDao.findByCategory(ItemCategory.valueOf(category));
        } else {
            items = inventoryDao.findAll();
        }

        if (Boolean.TRUE.equals(lowStockOnly)) {
            items = items.stream()
                    .filter(i -> i.getQuantityAvailable() <= i.getMinimumStockLevel())
                    .collect(Collectors.toList());
        }
        return items;
    }

    @Override
    public List<Donation> getDonationReport(String status, String type) {
        List<Donation> donations = donationDao.findAll();
        if (status != null && !status.isBlank()) {
            DonationStatus s = DonationStatus.valueOf(status);
            donations = donations.stream().filter(d -> d.getStatus() == s).collect(Collectors.toList());
        }
        if (type != null && !type.isBlank()) {
            DonationType t = DonationType.valueOf(type);
            donations = donations.stream().filter(d -> d.getDonationType() == t).collect(Collectors.toList());
        }
        return donations;
    }

    @Override
    public byte[] exportVictimsCsv(Long disasterId, String status, String priority) {
        List<Victim> victims = getVictimRoster(disasterId, status, priority);
        StringBuilder sb = new StringBuilder();
        sb.append("Reference Number,Full Name,Age,Disaster,Priority,Rescue Status,Location,Contact Number,Accessibility Needs,Date Registered\n");

        for (Victim v : victims) {
            sb.append(escapeCsv(v.getReferenceNumber())).append(",")
              .append(escapeCsv(v.getFullName())).append(",")
              .append(v.getAge() != null ? v.getAge() : "").append(",")
              .append(escapeCsv(v.getDisasterName())).append(",")
              .append(escapeCsv(v.getPriority() != null ? v.getPriority().name() : "")).append(",")
              .append(escapeCsv(v.getRescueStatus() != null ? v.getRescueStatus().name() : "")).append(",")
              .append(escapeCsv(v.getLocation())).append(",")
              .append(escapeCsv(v.getContactNumber())).append(",")
              .append(escapeCsv(v.getAccessibilityNeeds())).append(",")
              .append(v.getCreatedAt() != null ? v.getCreatedAt().format(DATE_TIME_FORMATTER) : "")
              .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportInventoryCsv(String category, Boolean lowStockOnly) {
        List<InventoryItem> items = getInventoryReport(category, lowStockOnly);
        StringBuilder sb = new StringBuilder();
        sb.append("Item ID,Item Name,Category,Quantity Available,Unit,Min Stock Level,Low Stock Status,Storage Location,Last Updated\n");

        for (InventoryItem item : items) {
            boolean isLow = item.getQuantityAvailable() <= item.getMinimumStockLevel();
            sb.append(item.getId()).append(",")
              .append(escapeCsv(item.getItemName())).append(",")
              .append(escapeCsv(item.getCategory() != null ? item.getCategory().name() : "")).append(",")
              .append(item.getQuantityAvailable()).append(",")
              .append(escapeCsv(item.getUnit() != null ? item.getUnit().name() : "")).append(",")
              .append(item.getMinimumStockLevel()).append(",")
              .append(isLow ? "ALERT_LOW" : "OPTIMAL").append(",")
              .append(escapeCsv(item.getStorageLocation())).append(",")
              .append(item.getLastUpdatedAt() != null ? item.getLastUpdatedAt().format(DATE_TIME_FORMATTER) : "")
              .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportDonationsCsv(String status, String type) {
        List<Donation> donations = getDonationReport(status, type);
        StringBuilder sb = new StringBuilder();
        sb.append("Donation Reference,Donor Name,Email,Phone,Type,Amount / Quantity,Unit,Status,Notes,Created At\n");

        for (Donation d : donations) {
            sb.append(escapeCsv(d.getDonationReference())).append(",")
              .append(escapeCsv(d.getDonorName())).append(",")
              .append(escapeCsv(d.getDonorEmail())).append(",")
              .append(escapeCsv(d.getDonorPhone())).append(",")
              .append(escapeCsv(d.getDonationType() != null ? d.getDonationType().name() : "")).append(",")
              .append(d.getAmount() != null ? d.getAmount() : d.getItemQuantity()).append(",")
              .append(escapeCsv(d.getItemUnit())).append(",")
              .append(escapeCsv(d.getStatus() != null ? d.getStatus().name() : "")).append(",")
              .append(escapeCsv(d.getNotes())).append(",")
              .append(d.getCreatedAt() != null ? d.getCreatedAt().format(DATE_TIME_FORMATTER) : "")
              .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
