package com.resqhub.spring.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.DisasterController;
import com.resqhub.controller.RescueRequestController;
import com.resqhub.controller.RescueTeamController;
import com.resqhub.controller.VictimController;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.RescueAssignment;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.Victim;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class RescueRequestWebController extends BaseWebController {

    private final RescueRequestController requestController = new RescueRequestController();
    private final DisasterController disasterController = new DisasterController();
    private final VictimController victimController = new VictimController();
    private final RescueTeamController teamController = new RescueTeamController();

    @GetMapping("/app/requests")
    public String listOrDetail(@RequestParam(value = "id", required = false) Long id, Model model) throws Exception {
        com.resqhub.model.User u = com.resqhub.service.SessionManager.getInstance().getCurrentUser();
        if (u != null && u.getRole() != com.resqhub.model.RoleType.ADMIN && u.getRole() != com.resqhub.model.RoleType.RESCUE_OFFICER) {
            return "redirect:/app/dashboard";
        }
        List<RescueRequest> list = requestController.getAllRequests();
        if (id != null) {
            RescueRequest r = list.stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
            if (r == null) {
                model.addAttribute("errorTitle", "Request not found");
                model.addAttribute("errorMessage", "Rescue request #" + id + " does not exist.");
                return "error";
            }
            List<RescueAssignment> history = requestController.getAssignmentHistory(r.getId());
            List<RescueTeam> allTeams = teamController.getAllTeams();
            Map<Long, String> teamNames = new HashMap<>();
            allTeams.forEach(t -> teamNames.put(t.getId(), t.getTeamName()));

            RescueAssignment active = history.stream()
                    .filter(a -> a.getAssignmentStatus() != AssignmentStatus.COMPLETED && a.getAssignmentStatus() != AssignmentStatus.ABORTED)
                    .findFirst().orElse(null);

            ActionResult explain = requestController.explainPriority(r.getId());
            List<RescueTeam> suitable = teamController.findSuitableTeams(r);
            List<RescueTeam> available = teamController.getAvailableTeams();

            model.addAttribute("r", r);
            model.addAttribute("history", history);
            model.addAttribute("teamNames", teamNames);
            model.addAttribute("active", active);
            model.addAttribute("explanation", explain.isSuccess() ? explain.getMessage() : "");
            model.addAttribute("suitableIds", opts(suitable, RescueTeam::getId, RescueTeam::getTeamName));
            model.addAttribute("teamOptions", opts(available, RescueTeam::getId,
                    t -> t.getTeamName() + " (" + t.getTeamType().getLabel() + ", " + t.getMemberCount() + " members)"));
            model.addAttribute("suitableCount", suitable.size());
            populateFormLists(model);
            model.addAttribute("pageTitle", "Rescue request #" + r.getId());
            return "request-detail";
        }

        model.addAttribute("items", list);
        model.addAttribute("headers", RescueRequestController.tableHeaders());
        model.addAttribute("rows", rows(list, RescueRequestController::toRow));
        populateFormLists(model);
        model.addAttribute("pageTitle", "Rescue Requests");
        return "requests";
    }

    private void populateFormLists(Model model) throws Exception {
        List<Disaster> disasters = disasterController.getAllDisasters();
        List<Victim> victims = victimController.getAllVictims();
        model.addAttribute("disasterOptions", opts(disasters, Disaster::getId, d -> "#" + d.getId() + " " + d.getTitle()));
        model.addAttribute("victimOptions", opts(victims, Victim::getId, v -> "#" + v.getId() + " " + v.getFullName()));
    }

    @PostMapping("/app/requests")
    public String handleAction(@RequestParam("action") String action,
                               HttpServletRequest req,
                               RedirectAttributes ra) throws Exception {
        ActionResult r;
        String redirectPath = "/app/requests";
        String idParam = req.getParameter("id");
        if (idParam != null && !idParam.isBlank()) {
            redirectPath = "/app/requests?id=" + idParam;
        }

        switch (action) {
            case "create" -> {
                r = requestController.submitRequest(
                        WebUtil.optLong(req, "disasterId", "Disaster"),
                        WebUtil.optLong(req, "victimId", "Victim"),
                        WebUtil.str(req, "requesterName"),
                        WebUtil.str(req, "contactNumber"),
                        WebUtil.str(req, "location"),
                        WebUtil.str(req, "people"),
                        WebUtil.str(req, "children"),
                        WebUtil.str(req, "elderly"),
                        WebUtil.flag(req, "lifeThreatening"),
                        WebUtil.flag(req, "medicalEmergency"),
                        WebUtil.flag(req, "trapped"),
                        WebUtil.str(req, "assistance")
                );
                return handleResult(r, ra, "/app/requests");
            }
            case "update" -> r = requestController.updateRequest(
                    WebUtil.reqLong(req, "id", "Request id"),
                    WebUtil.optLong(req, "disasterId", "Disaster"),
                    WebUtil.optLong(req, "victimId", "Victim"),
                    WebUtil.str(req, "requesterName"),
                    WebUtil.str(req, "contactNumber"),
                    WebUtil.str(req, "location"),
                    WebUtil.str(req, "people"),
                    WebUtil.str(req, "children"),
                    WebUtil.str(req, "elderly"),
                    WebUtil.flag(req, "lifeThreatening"),
                    WebUtil.flag(req, "medicalEmergency"),
                    WebUtil.flag(req, "trapped"),
                    WebUtil.str(req, "assistance")
            );
            case "startReview" -> r = requestController.startReview(WebUtil.reqLong(req, "id", "Request id"));
            case "unreview" -> r = requestController.unreview(WebUtil.reqLong(req, "id", "Request id"));
            case "cancel" -> r = requestController.cancelRequest(WebUtil.reqLong(req, "id", "Request id"));
            case "delete" -> {
                r = requestController.deleteRequest(WebUtil.reqLong(req, "id", "Request id"));
                return handleResult(r, ra, "/app/requests");
            }
            case "assign" -> r = requestController.assignTeam(
                    WebUtil.reqLong(req, "id", "Request id"),
                    WebUtil.reqLong(req, "teamId", "Team")
            );
            case "progress" -> r = requestController.progressAssignment(
                    WebUtil.reqLong(req, "assignmentId", "Assignment"),
                    WebUtil.reqEnum(req, "target", AssignmentStatus.class, "Target status"),
                    WebUtil.opt(req, "notes")
            );
            case "complete" -> r = requestController.completeAssignment(WebUtil.reqLong(req, "assignmentId", "Assignment"));
            case "abort" -> r = requestController.abortAssignment(
                    WebUtil.reqLong(req, "assignmentId", "Assignment"),
                    WebUtil.opt(req, "notes")
            );
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:" + redirectPath;
            }
        }
        return handleResult(r, ra, redirectPath);
    }

    @GetMapping("/app/report-emergency")
    public String reportEmergencyForm(Model model) throws Exception {
        List<Disaster> disasters = disasterController.getActiveDisasters();
        model.addAttribute("disasterOptions", opts(disasters, Disaster::getId, d -> d.getTitle() + " — " + d.getLocation()));
        model.addAttribute("pageTitle", "Report Emergency");
        return "report-emergency";
    }

    @PostMapping("/app/report-emergency")
    public String handleReportEmergency(HttpServletRequest req, RedirectAttributes ra) throws Exception {
        ActionResult r = requestController.submitRequest(
                WebUtil.optLong(req, "disasterId", "Disaster"),
                null,
                WebUtil.str(req, "requesterName"),
                WebUtil.str(req, "contactNumber"),
                WebUtil.str(req, "location"),
                WebUtil.str(req, "people"),
                WebUtil.str(req, "children"),
                WebUtil.str(req, "elderly"),
                WebUtil.flag(req, "lifeThreatening"),
                WebUtil.flag(req, "medicalEmergency"),
                WebUtil.flag(req, "trapped"),
                WebUtil.str(req, "assistance")
        );
        return handleResult(r, ra, "/app/dashboard", "/app/report-emergency");
    }
}
