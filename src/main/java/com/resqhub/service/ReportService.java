package com.resqhub.service;

import com.resqhub.dto.report.IncidentReportDto;
import com.resqhub.model.Donation;
import com.resqhub.model.InventoryItem;
import com.resqhub.model.Victim;

import java.util.List;

public interface ReportService {

    IncidentReportDto generateIncidentReport(Long disasterId);

    List<Victim> getVictimRoster(Long disasterId, String status, String priority);

    List<InventoryItem> getInventoryReport(String category, Boolean lowStockOnly);

    List<Donation> getDonationReport(String status, String type);

    byte[] exportVictimsCsv(Long disasterId, String status, String priority);

    byte[] exportInventoryCsv(String category, Boolean lowStockOnly);

    byte[] exportDonationsCsv(String status, String type);
}
