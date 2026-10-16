package com.resqhub.service;

import com.resqhub.dto.report.IncidentReportDto;
import com.resqhub.model.Donation;
import com.resqhub.model.InventoryItem;
import com.resqhub.model.Victim;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReportServiceTest {

    @Autowired
    private ReportService reportService;

    @Test
    @DisplayName("Generate incident report should aggregate disaster metrics and records")
    void testGenerateIncidentReport() {
        IncidentReportDto report = reportService.generateIncidentReport(1L);
        assertNotNull(report);
        assertNotNull(report.getDisaster());
        assertEquals(1L, report.getDisaster().getId());
        assertTrue(report.getTotalVictimsCount() >= 0);
        assertNotNull(report.getVictims());
        assertNotNull(report.getRescueRequests());
        assertNotNull(report.getShelterResidents());
        assertNotNull(report.getVolunteerAssignments());
    }

    @Test
    @DisplayName("Get victim roster should retrieve filterable victim lists")
    void testGetVictimRoster() {
        List<Victim> victims = reportService.getVictimRoster(null, null, null);
        assertNotNull(victims);
        assertFalse(victims.isEmpty(), "Victim roster should contain records");
    }

    @Test
    @DisplayName("Get inventory report should return items with low stock filtering")
    void testGetInventoryReport() {
        List<InventoryItem> allItems = reportService.getInventoryReport(null, false);
        assertNotNull(allItems);
        assertFalse(allItems.isEmpty());

        List<InventoryItem> lowStock = reportService.getInventoryReport(null, true);
        assertNotNull(lowStock);
        for (InventoryItem item : lowStock) {
            assertTrue(item.getQuantityAvailable() <= item.getMinimumStockLevel());
        }
    }

    @Test
    @DisplayName("Get donation report should return donations")
    void testGetDonationReport() {
        List<Donation> donations = reportService.getDonationReport(null, null);
        assertNotNull(donations);
        assertFalse(donations.isEmpty());
    }

    @Test
    @DisplayName("Export victims CSV should generate valid CSV content")
    void testExportVictimsCsv() {
        byte[] csvBytes = reportService.exportVictimsCsv(null, null, null);
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("Reference Number,Full Name,Age,Disaster"));
    }

    @Test
    @DisplayName("Export inventory CSV should generate valid CSV content")
    void testExportInventoryCsv() {
        byte[] csvBytes = reportService.exportInventoryCsv(null, false);
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("Item ID,Item Name,Category,Quantity Available"));
    }

    @Test
    @DisplayName("Export donations CSV should generate valid CSV content")
    void testExportDonationsCsv() {
        byte[] csvBytes = reportService.exportDonationsCsv(null, null);
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("Donation Reference,Donor Name,Email,Phone"));
    }
}
