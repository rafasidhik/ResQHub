package com.resqhub.spring.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.DisasterController;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/app/disasters")
public class DisasterWebController extends BaseWebController {

    private final DisasterController disasterController = new DisasterController();

    @GetMapping
    public String listDisasters(Model model) throws Exception {
        List<Disaster> list = disasterController.getAllDisasters();
        model.addAttribute("items", list);
        model.addAttribute("headers", DisasterController.tableHeaders());
        model.addAttribute("rows", rows(list, DisasterController::toRow));
        model.addAttribute("types", WebUtil.options(DisasterType.class));
        model.addAttribute("severities", WebUtil.options(DisasterSeverity.class));
        model.addAttribute("statuses", WebUtil.options(DisasterStatus.class));
        model.addAttribute("pageTitle", "Disaster Management");
        return "disasters";
    }

    @PostMapping
    public String handleAction(@RequestParam("action") String action,
                               HttpServletRequest req,
                               RedirectAttributes ra) throws Exception {
        ActionResult r;
        switch (action) {
            case "create" -> r = disasterController.createDisaster(
                    WebUtil.str(req, "title"),
                    WebUtil.optEnum(req, "type", DisasterType.class, "Type"),
                    WebUtil.optEnum(req, "severity", DisasterSeverity.class, "Severity"),
                    WebUtil.str(req, "location"),
                    WebUtil.str(req, "population"),
                    WebUtil.dtText(req, "start"),
                    WebUtil.dtText(req, "end"),
                    WebUtil.opt(req, "description")
            );
            case "update" -> r = disasterController.updateDisaster(
                    WebUtil.reqLong(req, "id", "Disaster id"),
                    WebUtil.str(req, "title"),
                    WebUtil.optEnum(req, "type", DisasterType.class, "Type"),
                    WebUtil.optEnum(req, "severity", DisasterSeverity.class, "Severity"),
                    WebUtil.str(req, "location"),
                    WebUtil.str(req, "population"),
                    WebUtil.dtText(req, "start"),
                    WebUtil.dtText(req, "end"),
                    WebUtil.opt(req, "description")
            );
            case "status" -> r = disasterController.updateStatus(
                    WebUtil.reqLong(req, "id", "Disaster id"),
                    WebUtil.reqEnum(req, "status", DisasterStatus.class, "Status")
            );
            case "close" -> r = disasterController.closeDisaster(
                    WebUtil.reqLong(req, "id", "Disaster id")
            );
            case "delete" -> r = disasterController.deleteDisaster(
                    WebUtil.reqLong(req, "id", "Disaster id")
            );
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:/app/disasters";
            }
        }
        return handleResult(r, ra, "/app/disasters");
    }
}
