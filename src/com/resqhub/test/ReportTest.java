package com.resqhub.test;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.ReportController;
import com.resqhub.model.ReportFilters;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;

/**
 * ReportTest - end-to-end smoke test for the Reports &amp; Analytics
 * module. Generates every report family through the controller (exactly
 * as the swing panel does) and asserts each produces a well-formed
 * {@link ReportResult} with consistent column widths.
 */
public class ReportTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        ReportController controller = new ReportController();

        check("overview generates", () -> {
            ReportResult r = generate(controller, ReportType.OVERVIEW,
                    ReportFilters.empty());
            assertNotNull(r);
            assertTrue(r.headers().length == 2, "overview headers width");
            assertTrue(!r.rows().isEmpty(), "overview has metrics");
            assertTrue(r.summaryLines().size() > 0,
                    "overview has summary");
            return true;
        });

        for (ReportType type : ReportType.values()) {
            if (type == ReportType.OVERVIEW) {
                continue;
            }
            check(type + " generates", () -> {
                ReportResult r = generate(controller, type,
                        ReportFilters.empty());
                assertNotNull(r);
                assertTrue(r.headers() != null, type + " headers present");
                assertTrue(r.summaryLines() != null,
                        type + " summary present");
                for (Object[] row : r.rows()) {
                    assertTrue(row.length == r.headers().length,
                            type + " row/header width match");
                }
                return true;
            });
        }

        // filtering: rescue requests by disaster + status + priority
        check("rescue requests filtered", () -> {
            ReportResult r = generate(controller,
                    ReportType.RESCUE_REQUESTS,
                    new ReportFilters(null, "PENDING", "CRITICAL", null,
                            null, null, null, null));
            assertNotNull(r);
            return true;
        });

        // disaster list for the filter dropdown is populated
        check("disaster list for filter", () -> {
            var list = controller.getDisasters();
            assertNotNull(list);
            return true;
        });

        // acceptable CSV names
        check("csv name", () -> controller.csvName(ReportType.VICTIMS)
                .equals("report_victims"));

        System.out.println();
        System.out.println("ReportTest: " + passed + " passed, "
                + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static ReportResult generate(ReportController c,
            ReportType type, ReportFilters f) {
        ActionResult result = c.generateReport(type, f);
        if (!result.isSuccess()) {
            throw new AssertionError(type + " failed: " + result.getMessage());
        }
        return result.getData();
    }

    private static void check(String name, Check c) {
        try {
            if (c.run()) {
                passed++;
                System.out.println("[PASS] " + name);
            } else {
                failed++;
                System.out.println("[FAIL] " + name);
            }
        } catch (Exception e) {
            failed++;
            System.out.println("[FAIL] " + name + " -> " + e);
        }
    }

    private interface Check {
        boolean run() throws Exception;
    }

    private static void assertNotNull(Object o) {
        if (o == null) {
            throw new AssertionError("expected non-null");
        }
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) {
            throw new AssertionError(msg);
        }
    }
}
