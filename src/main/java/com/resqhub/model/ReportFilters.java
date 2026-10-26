package com.resqhub.model;

/**
 * Optional filter set applied by the Report screen before generating a
 * report. All fields are optional; a null/blank value means "no filter".
 */
public record ReportFilters(
        Long disasterId,
        String status,
        String priority,
        String location,
        String fromDate,
        String toDate,
        String resourceCategory) {

    public ReportFilters {
        status = blankToNull(status);
        priority = blankToNull(priority);
        location = blankToNull(location);
        fromDate = blankToNull(fromDate);
        toDate = blankToNull(toDate);
        resourceCategory = blankToNull(resourceCategory);
    }

    public static ReportFilters empty() {
        return new ReportFilters(null, null, null, null, null, null, null);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
