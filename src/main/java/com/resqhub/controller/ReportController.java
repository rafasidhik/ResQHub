package com.resqhub.controller;

import java.util.List;

import com.resqhub.dao.DisasterDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.Disaster;
import com.resqhub.model.ReportFilters;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;
import com.resqhub.service.ReportService;

/** Reports &amp; Analytics screen controller. */
public class ReportController {

    private final ReportService reportService = new ReportService();
    private final DisasterDAO disasterDAO = new DisasterDAO();

    /** Generates the chosen report, wrapped for the view. */
    public ActionResult generateReport(ReportType type, ReportFilters filters) {
        try {
            ReportResult result = reportService.generateReport(type, filters);
            if (type == ReportType.OVERVIEW || type == ReportType.DISASTERS
                    || type == ReportType.SHELTER_OCCUPANCY
                    || type == ReportType.RESOURCE_INVENTORY) {
                return ActionResult.successWithData("Report generated", result);
            }
            return ActionResult.successWithData(
                    result.summaryLines().size() + " summary line(s); "
                            + result.rows().size() + " detail row(s).",
                    result);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /** Disasters for the report filter dropdown (all disasters). */
    public List<Disaster> getDisasters() throws DataAccessException {
        return disasterDAO.findAll();
    }

    /** A suggested CSV filename for the current report. */
    public String csvName(ReportType type) {
        return "report_" + type.name().toLowerCase();
    }
}
