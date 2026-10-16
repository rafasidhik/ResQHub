package com.resqhub.controller;

import com.resqhub.dto.report.IncidentReportDto;
import com.resqhub.model.*;
import com.resqhub.service.DisasterService;
import com.resqhub.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;
    private final DisasterService disasterService;

    @Autowired
    public ReportController(ReportService reportService, DisasterService disasterService) {
        this.reportService = reportService;
        this.disasterService = disasterService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER', 'CAMP_MANAGER')")
    public String index(Model model) {
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("pageTitle", "Reports & Operational Analytics — ResQHub");
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        return "reports/index";
    }

    @GetMapping("/incident/{disasterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER', 'CAMP_MANAGER')")
    public String incidentSummary(@PathVariable Long disasterId, Model model) {
        IncidentReportDto report = reportService.generateIncidentReport(disasterId);
        model.addAttribute("report", report);
        model.addAttribute("pageTitle", "Incident Dossier: " + report.getDisaster().getName() + " — ResQHub");
        model.addAttribute("reportDate", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        return "reports/incident-summary";
    }

    @GetMapping("/victims")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String victimRoster(@RequestParam(value = "disasterId", required = false) Long disasterId,
                              @RequestParam(value = "status", required = false) String status,
                              @RequestParam(value = "priority", required = false) String priority,
                              Model model) {
        List<Victim> victims = reportService.getVictimRoster(disasterId, status, priority);
        model.addAttribute("victims", victims);
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("selectedDisasterId", disasterId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("pageTitle", "Victim Rescue Roster & Triage Report — ResQHub");
        return "reports/victim-roster";
    }

    @GetMapping("/victims/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public ResponseEntity<byte[]> exportVictimsCsv(@RequestParam(value = "disasterId", required = false) Long disasterId,
                                                   @RequestParam(value = "status", required = false) String status,
                                                   @RequestParam(value = "priority", required = false) String priority) {
        byte[] csv = reportService.exportVictimsCsv(disasterId, status, priority);
        String filename = "victims_roster_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String inventoryStatus(@RequestParam(value = "category", required = false) String category,
                                 @RequestParam(value = "lowStockOnly", required = false) Boolean lowStockOnly,
                                 Model model) {
        List<InventoryItem> items = reportService.getInventoryReport(category, lowStockOnly);
        model.addAttribute("items", items);
        model.addAttribute("categories", ItemCategory.values());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("lowStockOnly", Boolean.TRUE.equals(lowStockOnly));
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("pageTitle", "Relief Inventory & Supply Roster — ResQHub");
        return "reports/inventory-status";
    }

    @GetMapping("/inventory/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public ResponseEntity<byte[]> exportInventoryCsv(@RequestParam(value = "category", required = false) String category,
                                                     @RequestParam(value = "lowStockOnly", required = false) Boolean lowStockOnly) {
        byte[] csv = reportService.exportInventoryCsv(category, lowStockOnly);
        String filename = "inventory_status_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/donations")
    @PreAuthorize("hasRole('ADMIN')")
    public String donationAudit(@RequestParam(value = "status", required = false) String status,
                               @RequestParam(value = "type", required = false) String type,
                               Model model) {
        List<Donation> donations = reportService.getDonationReport(status, type);
        model.addAttribute("donations", donations);
        model.addAttribute("statuses", DonationStatus.values());
        model.addAttribute("types", DonationType.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);
        model.addAttribute("activeMenu", "reports");
        model.addAttribute("pageTitle", "Humanitarian Donations Audit Report — ResQHub");
        return "reports/donation-audit";
    }

    @GetMapping("/donations/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportDonationsCsv(@RequestParam(value = "status", required = false) String status,
                                                     @RequestParam(value = "type", required = false) String type) {
        byte[] csv = reportService.exportDonationsCsv(status, type);
        String filename = "donations_audit_" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
