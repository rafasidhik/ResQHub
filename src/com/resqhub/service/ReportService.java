package com.resqhub.service;

import java.util.ArrayList;
import java.util.List;

import com.resqhub.dao.ReportDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidReportException;
import com.resqhub.model.ReportFilters;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;

/**
 * Analytical / reporting service. Each report family owns a
 * SQL aggregation (COUNT / SUM / AVG / MIN / MAX / GROUP BY / HAVING /
 * JOIN), its column headers, a formatting/labelling step and the
 * summary lines that give the reader an instant aggregate picture.
 *
 * The module primarily READS from every other module's tables, so this
 * service sits on top of the reporting DAO and is intentionally
 * read-only — it never mutates the underlying data.
 */
public class ReportService {

    private final ReportDAO reportDAO = new ReportDAO();

    public ReportResult generateReport(ReportType type, ReportFilters filters)
            throws DataAccessException, InvalidReportException {
        ReportFilters f = filters == null ? ReportFilters.empty() : filters;
        return switch (type) {
            case OVERVIEW -> overview();
            case DISASTERS -> disasterReport();
            case VICTIMS -> victimReport(f);
            case RESCUE_REQUESTS -> rescueRequestReport(f);
            case RESCUE_PERFORMANCE -> rescuePerformanceReport(f);
            case VOLUNTEERS -> volunteerReport();
            case DONATIONS -> donationReport(f);
            case SHELTER_OCCUPANCY -> shelterOccupancyReport();
            case HOSPITAL_CAPACITY -> notPresent("Hospital Capacity Report");
            case BLOOD_AVAILABILITY -> notPresent("Blood Availability Report");
            case RESOURCE_INVENTORY -> resourceInventoryReport();
            case FOOD_DISTRIBUTION -> notPresent("Food Distribution Report");
        };
    }

    /** Cross-module dashboard overview (summary cards). */
    public ReportResult overview() throws DataAccessException {
        List<Object[]> rows = reportDAO.overview();
        return new ReportResult(
                "Dashboard Overview - System Summary",
                new String[]{"Metric", "Value"},
                rows,
                List.of("Live cross-module picture of the current disaster-response situation.",
                        "Sources: disasters, victims, rescue_requests, rescue_teams, "
                                + "volunteers, donors & donations (COUNT / SUM)."),
                "");
    }

    // ── active disaster report ───────────────────────────────────────

    private ReportResult disasterReport() throws DataAccessException {
        List<String> notes = new ArrayList<>();
        notes.add("Grouped views over the disasters table (COUNT, SUM, MAX, GROUP BY).");
        notes.add("HAVING sample: disasters affecting 50+ people appear below.");

        List<Object[]> byType = toDisplay(reportDAO.disastersByType(),
                new int[]{1, 2});
        List<Object[]> bySeverity = toDisplay(reportDAO.disastersBySeverity(),
                new int[]{1, 2});
        List<Object[]> byStatus = toDisplay(reportDAO.disastersByStatus(),
                new int[]{1, 2});
        List<Object[]> byLocation = toDisplay(reportDAO.disastersByLocation(),
                new int[]{2});
        List<Object[]> having = toDisplay(reportDAO.disastersAbovePopulation(50),
                new int[]{3});

        StringBuilder combined = new StringBuilder();
        combined.append("Type breakdown:\n");
        for (Object[] r : byType) {
            combined.append("  ").append(r[0]).append(" \u2192 ").append(r[1])
                    .append("\n");
        }
        combined.append("Severity breakdown (worst first):\n");
        for (Object[] r : bySeverity) {
            combined.append("  ").append(r[0]).append(" \u2192 ").append(r[1])
                    .append("\n");
        }
        combined.append("Status breakdown:\n");
        for (Object[] r : byStatus) {
            combined.append("  ").append(r[0]).append(" \u2192 ").append(r[1])
                    .append("\n");
        }
        combined.append("Locations by disaster count (top):\n");
        for (Object[] r : byLocation) {
            combined.append("  ").append(r[0]).append(" \u2192 ").append(r[1])
                    .append(" disasters\n");
        }
        combined.append("Disasters affecting 50+ people (HAVING):\n");
        if (having.isEmpty()) {
            combined.append("  (none)\n");
        } else {
            for (Object[] r : having) {
                combined.append("  ").append(r[0]).append(" @ ").append(r[1])
                        .append(" \u2192 ").append(r[2]).append(" people\n");
            }
        }

        List<Object[]> combinedRows = new ArrayList<>();
        for (Object[] r : byType) {
            combinedRows.add(new Object[]{"By type: " + r[0], r[1],
                    "Sum affected " + r[2]});
        }
        for (Object[] r : bySeverity) {
            combinedRows.add(new Object[]{"Severity " + r[0], r[1],
                    "Sum affected " + r[2]});
        }
        for (Object[] r : byStatus) {
            combinedRows.add(new Object[]{"Status " + r[0], r[1],
                    "Sum affected " + r[2]});
        }
        for (Object[] r : byLocation) {
            combinedRows.add(new Object[]{"Location " + r[0], r[1],
                    "Max affected " + r[2]});
        }
        for (Object[] r : having) {
            combinedRows.add(new Object[]{"[HAVING] " + r[0], r[1],
                    "Affected " + r[2]});
        }
        // also append per-request detail? keep concise.

        List<String> summary = new ArrayList<>();
        summary.addAll(notes);
        summary.add(combined.toString());

        return new ReportResult("Active Disaster Report",
                new String[]{"Grouping", "Disasters", "Population"},
                combinedRows, summary, "");
    }

    // ── victim statistics ────────────────────────────────────────────

    private ReportResult victimReport(ReportFilters f)
            throws DataAccessException {
        List<Object[]> byDisaster = toDisplay(
                reportDAO.victimsByDisaster(f), new int[]{2, 3, 4, 5, 6});

        List<Object[]> byStatus = toDisplay(reportDAO.victimsByStatus(),
                new int[]{1});
        List<Object[]> byGender = toDisplay(reportDAO.victimsByGender(),
                new int[]{1, 2});

        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : byDisaster) {
            rows.add(new Object[]{r[0], r[1], r[2], r[3], r[4], r[5]});
        }

        List<String> summary = new ArrayList<>();
        summary.add("Victims grouped by disaster, then by emergency status and gender.");
        summary.add("Status (COUNT):");
        for (Object[] r : byStatus) {
            summary.add("  " + r[0] + " \u2192 " + r[1]);
        }
        summary.add("Gender (COUNT / AVG / MIN / MAX age):");
        for (Object[] r : byGender) {
            summary.add("  " + r[0] + " \u2192 " + r[1] + " | avg "
                    + r[2] + " | min " + r[3] + " | max " + r[4]);
        }

        return new ReportResult("Victim Statistics",
                new String[]{"Disaster", "Total", "Safe",
                        "Rescue Required", "Injured", "Critical"},
                rows, summary, "");
    }

    // ── rescue request statistics ────────────────────────────────────

    private ReportResult rescueRequestReport(ReportFilters f)
            throws DataAccessException {
        List<Object[]> byStatus = toDisplay(reportDAO.requestsByStatus(),
                new int[]{2});
        List<Object[]> byPriority = toDisplay(reportDAO.requestsByPriority(),
                new int[]{1});
        List<Object[]> detail = toDisplay(reportDAO.requestsWithDetails(f),
                new int[]{6});

        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : detail) {
            rows.add(new Object[]{r[0], r[1], r[2], r[3], r[4], r[5],
                    r[6] == null ? "-" : r[6]});
        }

        List<String> summary = new ArrayList<>();
        summary.add("Rescue requests grouped by status (COUNT) and priority (COUNT):");
        for (Object[] r : byStatus) {
            summary.add("  status " + r[0] + " \u2192 " + r[1]
                    + " requests, " + r[2] + " people");
        }
        for (Object[] r : byPriority) {
            summary.add("  priority " + r[0] + " \u2192 " + r[1]);
        }
        summary.add("Detail rows join rescue_requests \u00d7 disasters \u00d7 victims "
                + "(JOIN), sorted by priority (highest first).");

        return new ReportResult("Rescue Request Statistics",
                new String[]{"ID", "Location", "Disaster", "Priority",
                        "Status", "People", "Victim"},
                rows, summary, "");
    }

    // ── rescue performance report ────────────────────────────────────

    private ReportResult rescuePerformanceReport(ReportFilters f)
            throws DataAccessException {
        List<Object[]> byTeam = toDisplay(reportDAO.assignmentsByTeam(f),
                new int[]{1, 2, 3});
        List<Object[]> byStatus = toDisplay(reportDAO.assignmentsByStatus(),
                new int[]{1});
        List<Object[]> byDisaster = toDisplay(reportDAO.operationsByDisaster(),
                new int[]{1, 2});

        List<String> summary = new ArrayList<>();
        summary.add("Rescue assignment workload per team "
                + "(COUNT / completed vs active), JOIN over rescue_teams "
                + "\u00d7 rescue_assignments \u00d7 rescue_requests):");
        for (Object[] r : byTeam) {
            summary.add("  " + r[0] + " \u2192 " + r[1] + " total ("
                    + r[2] + " completed, " + r[3] + " active)");
        }
        summary.add("Assignments by status:");
        for (Object[] r : byStatus) {
            summary.add("  " + r[0] + " \u2192 " + r[1]);
        }
        summary.add("Operations by disaster:");
        for (Object[] r : byDisaster) {
            summary.add("  " + r[0] + " \u2192 " + r[1] + " ops"
                    + (r.length > 2 ? ", " + r[2] + " completed" : ""));
        }

        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : byDisaster) {
            rows.add(new Object[]{r[0], r[1], r[2]});
        }

        return new ReportResult("Rescue Performance Report",
                new String[]{"Disaster", "Operations", "Completed"},
                rows, summary, "");
    }

    // ── volunteer report ─────────────────────────────────────────────

    private ReportResult volunteerReport() throws DataAccessException {
        List<Object[]> byAvailability = toDisplay(
                reportDAO.volunteersByAvailability(), new int[]{1});
        List<Object[]> byRole = toDisplay(reportDAO.volunteersByRole(),
                new int[]{1});
        List<Object[]> byLocation = toDisplay(reportDAO.volunteersByLocation(),
                new int[]{1});
        List<Object[]> load = toDisplay(reportDAO.volunteerTaskLoad(),
                new int[]{1, 2, 3});

        List<String> summary = new ArrayList<>();
        summary.add("Volunteer availability (COUNT):");
        for (Object[] r : byAvailability) {
            summary.add("  " + r[0] + " \u2192 " + r[1]);
        }
        summary.add("Volunteers by emergency role:");
        for (Object[] r : byRole) {
            summary.add("  " + r[0] + " \u2192 " + r[1]);
        }
        summary.add("Volunteers by location:");
        for (Object[] r : byLocation) {
            summary.add("  " + r[0] + " \u2192 " + r[1]);
        }
        summary.add("Current task load (volunteer_assignments, COUNT):");

        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : load) {
            rows.add(new Object[]{r[0], r[1], r[2]});
            summary.add("  " + r[0] + ": " + r[1] + " completed, "
                    + r[2] + " active");
        }

        return new ReportResult("Volunteer Reports",
                new String[]{"Volunteer", "Completed Tasks", "Active Tasks"},
                rows, summary, "");
    }

    // ── donation statistics ──────────────────────────────────────────

    private ReportResult donationReport(ReportFilters f)
            throws DataAccessException {
        List<Object[]> byType = toDisplay(reportDAO.donationsByType(),
                new int[]{2});
        List<Object[]> byStatus = toDisplay(reportDAO.donationsByStatus(),
                new int[]{1});
        List<Object[]> byDonor = toDisplay(reportDAO.donationsByDonor(f),
                new int[]{3});
        List<Object[]> distributions = toDisplay(
                reportDAO.distributionAggregate(), new int[]{2});

        List<String> summary = new ArrayList<>();
        summary.add("Donations by type (COUNT / SUM quantity):");
        for (Object[] r : byType) {
            summary.add("  " + r[0] + " \u2192 " + r[1]
                    + (r[2] == null ? "" : " items"));
        }
        summary.add("Donations by status:");
        for (Object[] r : byStatus) {
            summary.add("  " + r[0] + " \u2192 " + r[1]);
        }
        summary.add("Cash donated per donor (SUM) - JOIN donors \u00d7 donations:");
        for (Object[] r : byDonor) {
            summary.add("  " + r[0] + " (" + r[1] + ") \u2192 Rs "
                    + r[3]);
        }
        summary.add("Distribution by beneficiary (COUNT / SUM):");
        for (Object[] r : distributions) {
            summary.add("  " + r[0] + " \u2192 " + r[1] + " events, "
                    + r[2] + " units");
        }

        List<Object[]> rows = new ArrayList<>();
        for (Object[] r : byDonor) {
            rows.add(new Object[]{r[0], r[1], r[2], r[3]});
        }

        return new ReportResult("Donation Statistics",
                new String[]{"Donor", "Type", "Donations", "Cash (Rs)"},
                rows, summary, "");
    }

    // ── shelter / resource ───────────────────────────────────────────

    private ReportResult shelterOccupancyReport() throws DataAccessException {
        List<Object[]> rows = toDisplay(reportDAO.shelterOccupancy(),
                new int[]{1, 2});
        List<String> summary = new ArrayList<>();
        summary.add("The dedicated shelters/camps table is part of another team "
                + "member's module and does not exist in this build.");
        summary.add("As a placeholder, victims are grouped by their disaster "
                + "(zones often used for sheltering):");
        for (Object[] r : rows) {
            summary.add("  " + r[0] + " \u2192 " + r[1]
                    + " victims placed, " + r[2] + " safe");
        }
        return new ReportResult("Shelter Occupancy Report",
                new String[]{"Shelter Zone", "Victims Placed", "Safe"},
                rows, summary, "");
    }

    private ReportResult resourceInventoryReport() throws DataAccessException {
        List<Object[]> rows = toDisplay(reportDAO.resourceInventory(),
                new int[]{1, 2});
        List<String> summary = new ArrayList<>();
        summary.add("Material donations act as the resource inventory in this "
                + "build (aggregated by item name):");
        for (Object[] r : rows) {
            summary.add("  " + r[0] + " \u2192 " + r[1] + " units total, "
                    + r[2] + " distributed");
        }
        summary.add("Items with 10 or fewer units remaining raise a "
                + "LOW_STOCK notification (see Alerts).");
        return new ReportResult("Resource & Inventory Report",
                new String[]{"Resource", "Total Units", "Used"},
                rows, summary, "");
    }

    private ReportResult notPresent(String label) {
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{"No data in this build",
                "The " + label + " relies on a module owned by another team "
                        + "member. Its table is not present, so this report "
                        + "returns no records. The reporting framework below "
                        + "is ready to render it once the table exists."});
        List<String> notes = new ArrayList<>();
        notes.add(label + " is awaiting integration with its source module.");
        return new ReportResult(label,
                new String[]{"Status", "Detail"},
                rows, notes, "");
    }

    // ── helpers ──────────────────────────────────────────────────────

    /** Returns a deep copy as display strings, marking numeric columns. */
    private List<Object[]> toDisplay(List<Object[]> rows, int[] numericCols) {
        List<Object[]> out = new ArrayList<>();
        for (Object[] row : rows) {
            Object[] copy = new Object[row.length];
            for (int i = 0; i < row.length; i++) {
                copy[i] = format(row[i], isNumericCol(i, numericCols));
            }
            out.add(copy);
        }
        return out;
    }

    private boolean isNumericCol(int col, int[] numericCols) {
        for (int n : numericCols) {
            if (n == col) {
                return true;
            }
        }
        return false;
    }

    private String format(Object value, boolean numeric) {
        if (value == null) {
            return "0";
        }
        String s = String.valueOf(value).trim();
        if (numeric && value instanceof Number) {
            return s;
        }
        return s;
    }
}
