package com.resqhub.spring.controller;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.ReportController;
import com.resqhub.model.Disaster;
import com.resqhub.model.ReportFilters;
import com.resqhub.model.ReportResult;
import com.resqhub.model.ReportType;
import com.resqhub.model.ResourceCategory;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.service.SessionManager;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class ReportWebController extends BaseWebController {

    private final ReportController reportController = new ReportController();

    private static final Set<ReportType> STAFF_ONLY =
            Set.of(ReportType.VOLUNTEERS, ReportType.DONATIONS);

    private static boolean mayView(ReportType t) {
        User u = SessionManager.getInstance().getCurrentUser();
        if (u == null) return false;
        return !STAFF_ONLY.contains(t) || u.getRole() == RoleType.ADMIN || u.getRole() == RoleType.RESCUE_OFFICER;
    }

    @GetMapping("/app/reports")
    public String reports(@RequestParam(value = "type", required = false) String typeStr,
                          @RequestParam(value = "disasterId", required = false) Long disasterId,
                          @RequestParam(value = "status", required = false) String status,
                          @RequestParam(value = "priority", required = false) String priority,
                          @RequestParam(value = "location", required = false) String location,
                          @RequestParam(value = "fromDate", required = false) String fromDate,
                          @RequestParam(value = "toDate", required = false) String toDate,
                          @RequestParam(value = "resourceCategory", required = false) String resourceCategory,
                          @RequestParam(value = "export", required = false) String export,
                          HttpServletRequest req,
                          HttpServletResponse resp,
                          Model model) throws Exception {

        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null && (u.getRole() == RoleType.CITIZEN || u.getRole() == RoleType.VOLUNTEER)) {
            return "redirect:/app/dashboard";
        }

        ReportType type = ReportType.OVERVIEW;
        if (typeStr != null && !typeStr.isBlank()) {
            try {
                type = ReportType.valueOf(typeStr);
            } catch (Exception ignored) {}
        }

        if (!mayView(type)) {
            model.addAttribute("flashType", "danger");
            model.addAttribute("flashMessage", "Your role is not permitted to view the " + type.getLabel() + ".");
            type = ReportType.OVERVIEW;
        }

        ReportFilters filters = new ReportFilters(disasterId, status, priority, location, fromDate, toDate, resourceCategory);
        ActionResult r = reportController.generateReport(type, filters);

        List<Disaster> disasters = reportController.getDisasters();
        List<String[]> allowedTypes = new ArrayList<>();
        for (String[] o : WebUtil.options(ReportType.class)) {
            if (mayView(ReportType.valueOf(o[0]))) {
                allowedTypes.add(o);
            }
        }

        model.addAttribute("reportTypes", allowedTypes);
        model.addAttribute("categories", WebUtil.options(ResourceCategory.class));
        model.addAttribute("disasterOptions", opts(disasters, Disaster::getId, d -> "#" + d.getId() + " " + d.getTitle()));
        model.addAttribute("selectedType", type);

        if (!r.isSuccess()) {
            model.addAttribute("flashType", "danger");
            model.addAttribute("flashMessage", r.getMessage());
        } else {
            ReportResult result = r.getData();
            if ("csv".equals(export)) {
                writeCsv(resp, reportController.csvName(type), result);
                return null;
            }
            String q = req.getQueryString();
            String exportUrl = req.getRequestURI() + (q == null || q.isEmpty() ? "?" : "?" + q + "&") + "export=csv";
            model.addAttribute("exportUrl", exportUrl);
            model.addAttribute("report", result);
            model.addAttribute("reportRows", result.rows());
            model.addAttribute("reportHeaders", result.headers());
        }

        model.addAttribute("pageTitle", "Reports & Analytics");
        return "reports";
    }

    private static void writeCsv(HttpServletResponse resp, String baseName, ReportResult result) throws Exception {
        resp.setContentType("text/csv");
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + baseName + ".csv\"");
        PrintWriter out = resp.getWriter();
        for (int i = 0; i < result.headers().length; i++) {
            if (i > 0) out.print(',');
            out.print(csvEscape(result.headers()[i]));
        }
        out.println();
        for (Object[] row : result.rows()) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) out.print(',');
                Object val = row[i];
                out.print(csvEscape(val == null ? "" : String.valueOf(val)));
            }
            out.println();
        }
        out.flush();
    }

    private static String csvEscape(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
