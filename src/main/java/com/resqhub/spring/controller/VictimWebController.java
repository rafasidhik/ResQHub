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
import com.resqhub.controller.VictimController;
import com.resqhub.model.Disaster;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.Gender;
import com.resqhub.model.RoleType;
import com.resqhub.model.ShelterStatus;
import com.resqhub.model.Victim;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/app/victims")
public class VictimWebController extends BaseWebController {

    private final VictimController victimController = new VictimController();
    private final DisasterController disasterController = new DisasterController();

    @GetMapping
    public String listVictims(@RequestParam(value = "disaster", required = false) Long disasterFilter,
                              Model model) throws Exception {
        com.resqhub.model.User u = com.resqhub.service.SessionManager.getInstance().getCurrentUser();
        if (u != null && u.getRole() != RoleType.ADMIN && u.getRole() != RoleType.RESCUE_OFFICER) {
            return "redirect:/app/dashboard";
        }
        List<Victim> list = disasterFilter == null
                ? victimController.getAllVictims()
                : victimController.getVictimsByDisaster(disasterFilter);
        List<Disaster> disasters = disasterController.getAllDisasters();

        model.addAttribute("items", list);
        model.addAttribute("headers", VictimController.tableHeaders());
        model.addAttribute("rows", rows(list, VictimController::toRow));
        model.addAttribute("disasterOptions", opts(disasters, Disaster::getId, d -> "#" + d.getId() + " " + d.getTitle()));
        model.addAttribute("disasterFilter", disasterFilter);
        model.addAttribute("genders", WebUtil.options(Gender.class));
        model.addAttribute("emergencies", WebUtil.options(EmergencyStatus.class));
        model.addAttribute("shelterStatuses", WebUtil.options(ShelterStatus.class));
        model.addAttribute("pageTitle", "Victim Management");
        return "victims";
    }

    @PostMapping
    public String handleAction(@RequestParam("action") String action,
                               HttpServletRequest req,
                               RedirectAttributes ra) throws Exception {
        ActionResult r;
        switch (action) {
            case "create" -> r = victimController.registerVictim(
                    WebUtil.str(req, "fullName"),
                    WebUtil.str(req, "age"),
                    WebUtil.optEnum(req, "gender", Gender.class, "Gender"),
                    WebUtil.opt(req, "phone"),
                    WebUtil.optEnum(req, "emergencyStatus", EmergencyStatus.class, "Emergency status"),
                    WebUtil.opt(req, "medicalCondition"),
                    WebUtil.opt(req, "familyInfo"),
                    WebUtil.str(req, "currentLocation"),
                    WebUtil.optLong(req, "disasterId", "Disaster")
            );
            case "update" -> r = victimController.updateVictim(
                    WebUtil.reqLong(req, "id", "Victim id"),
                    WebUtil.str(req, "fullName"),
                    WebUtil.str(req, "age"),
                    WebUtil.optEnum(req, "gender", Gender.class, "Gender"),
                    WebUtil.opt(req, "phone"),
                    WebUtil.optEnum(req, "emergencyStatus", EmergencyStatus.class, "Emergency status"),
                    WebUtil.opt(req, "medicalCondition"),
                    WebUtil.opt(req, "familyInfo"),
                    WebUtil.str(req, "currentLocation"),
                    WebUtil.optLong(req, "disasterId", "Disaster")
            );
            case "triage" -> r = victimController.updateEmergencyStatus(
                    WebUtil.reqLong(req, "id", "Victim id"),
                    WebUtil.reqEnum(req, "emergencyStatus", EmergencyStatus.class, "Emergency status")
            );
            case "shelterStatus" -> r = victimController.markShelterStatus(
                    WebUtil.reqLong(req, "id", "Victim id"),
                    WebUtil.reqEnum(req, "shelterStatus", ShelterStatus.class, "Shelter status")
            );
            case "delete" -> r = victimController.deleteVictim(
                    WebUtil.reqLong(req, "id", "Victim id")
            );
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:/app/victims";
            }
        }
        return handleResult(r, ra, "/app/victims");
    }
}
