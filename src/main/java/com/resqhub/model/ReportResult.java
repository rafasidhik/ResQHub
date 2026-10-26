package com.resqhub.model;

import java.util.List;

/**
 * Immutable output of a report run: the rendered column headers, the
 * result rows, a human-readable title and a few summary key/metric lines
 * that describe the aggregate picture (e.g. "Total donated: Rs 50,000").
 *
 * {@code headers} and each {@code rows} element must have equal lengths;
 * {@code summaryLines} are optional plain-text bullets shown above the
 * table by the report screen.
 */
public record ReportResult(
        String title,
        String[] headers,
        List<Object[]> rows,
        List<String> summaryLines,
        String sqlNote) {

    public ReportResult {
        headers = headers == null ? new String[0] : headers;
        rows = rows == null ? List.of() : rows;
        summaryLines = summaryLines == null ? List.of() : summaryLines;
    }
}
