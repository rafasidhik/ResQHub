package com.resqhub.spring.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.DisasterController;
import com.resqhub.controller.ShelterController;
import com.resqhub.controller.SmartAllocationController;
import com.resqhub.controller.VictimController;
import com.resqhub.model.Disaster;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RoleType;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterAllocationStatus;
import com.resqhub.model.ShelterFacility;
import com.resqhub.model.ShelterOperationalStatus;
import com.resqhub.model.SmartAllocationResult;
import com.resqhub.model.User;
import com.resqhub.model.Victim;
import com.resqhub.service.SessionManager;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ShelterWebController extends BaseWebController {

    private final ShelterController shelterController = new ShelterController();
    private final DisasterController disasterController = new DisasterController();
    private final VictimController victimController = new VictimController();
    private final SmartAllocationController smartController = new SmartAllocationController();

    @GetMapping("/app/shelters")
    public String shelters(@RequestParam(value = "id", required = false) Long id, Model model) throws Exception {
        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null && (u.getRole() == RoleType.RESCUE_OFFICER || u.getRole() == RoleType.CITIZEN || u.getRole() == RoleType.VOLUNTEER)) {
            return "redirect:/app/dashboard";
        }
        List<Shelter> shelters = shelterController.getAllShelters();
        model.addAttribute("statuses", WebUtil.options(ShelterOperationalStatus.class));
        List<Disaster> disasters = disasterController.getAllDisasters();
        model.addAttribute("disasterOptions", opts(disasters, Disaster::getId,
                d -> "#" + d.getId() + " " + d.getTitle()));

        if (id == null) {
            model.addAttribute("items", shelters);
            model.addAttribute("headers", ShelterController.tableHeaders());
            model.addAttribute("rows", rows(shelters, ShelterController::toRow));
            model.addAttribute("pageTitle", "Shelter Management");
            return "shelters";
        }

        Shelter s = shelters.stream().filter(x -> x.getId().equals(id)).findFirst().orElse(null);
        if (s == null) {
            model.addAttribute("errorTitle", "Shelter not found");
            model.addAttribute("errorMessage", "Shelter #" + id + " does not exist.");
            return "error";
        }

        List<ShelterFacility> facilities = shelterController.getFacilities(id);
        List<ShelterAllocation> allocations = shelterController.getAllocations(id);
        List<Victim> victims = victimController.getAllVictims();

        int percent = s.getMaxCapacity() == 0 ? 0
                : Math.min(100, s.getCurrentOccupancy() * 100 / s.getMaxCapacity());

        model.addAttribute("s", s);
        model.addAttribute("percent", percent);
        model.addAttribute("facilities", facilities);
        model.addAttribute("facilityRows", rows(facilities, ShelterController::facilityRow));
        model.addAttribute("facilityHeaders", ShelterController.facilityHeaders());
        model.addAttribute("allocations", allocations);
        model.addAttribute("allocationRows", rows(allocations, ShelterController::allocationRow));
        model.addAttribute("allocationHeaders", ShelterController.allocationHeaders());
        model.addAttribute("victimOptions", opts(victims, Victim::getId,
                v -> "#" + v.getId() + " " + v.getFullName()));
        model.addAttribute("pageTitle", s.getName());
        return "shelter-detail";
    }

    @PostMapping("/app/shelters")
    public String handleShelterAction(@RequestParam("action") String action,
                                      HttpServletRequest req,
                                      RedirectAttributes ra) throws Exception {
        ActionResult r;
        String idParam = req.getParameter("id");
        String detailRedirect = idParam != null && !idParam.isBlank() ? "/app/shelters?id=" + idParam : "/app/shelters";

        switch (action) {
            case "create" -> {
                r = shelterController.createShelter(
                        WebUtil.str(req, "name"),
                        WebUtil.str(req, "code"),
                        WebUtil.str(req, "district"),
                        WebUtil.str(req, "city"),
                        WebUtil.str(req, "address"),
                        WebUtil.opt(req, "locationDescription"),
                        WebUtil.str(req, "maxCapacity"),
                        WebUtil.str(req, "occupancy"),
                        WebUtil.str(req, "contact"),
                        WebUtil.str(req, "manager"),
                        WebUtil.str(req, "disasterId"),
                        WebUtil.flag(req, "wheelchair"),
                        WebUtil.flag(req, "elderly"),
                        WebUtil.flag(req, "medical"),
                        WebUtil.flag(req, "assistance"),
                        WebUtil.optEnum(req, "status", ShelterOperationalStatus.class, "Status")
                );
                return handleResult(r, ra, "/app/shelters");
            }
            case "update" -> r = shelterController.updateShelter(
                    WebUtil.reqLong(req, "id", "Shelter id"),
                    WebUtil.str(req, "name"),
                    WebUtil.str(req, "code"),
                    WebUtil.str(req, "district"),
                    WebUtil.str(req, "city"),
                    WebUtil.str(req, "address"),
                    WebUtil.opt(req, "locationDescription"),
                    WebUtil.str(req, "maxCapacity"),
                    WebUtil.str(req, "occupancy"),
                    WebUtil.str(req, "contact"),
                    WebUtil.str(req, "manager"),
                    WebUtil.str(req, "disasterId"),
                    WebUtil.flag(req, "wheelchair"),
                    WebUtil.flag(req, "elderly"),
                    WebUtil.flag(req, "medical"),
                    WebUtil.flag(req, "assistance"),
                    WebUtil.optEnum(req, "status", ShelterOperationalStatus.class, "Status")
            );
            case "occupancy" -> r = shelterController.updateOccupancy(
                    WebUtil.reqLong(req, "id", "Shelter id"),
                    WebUtil.str(req, "occupancy")
            );
            case "status" -> r = shelterController.updateStatus(
                    WebUtil.reqLong(req, "id", "Shelter id"),
                    WebUtil.reqEnum(req, "status", ShelterOperationalStatus.class, "Status")
            );
            case "addFacility" -> r = shelterController.addFacility(
                    WebUtil.reqLong(req, "id", "Shelter id"),
                    WebUtil.str(req, "facilityName"),
                    WebUtil.flag(req, "available")
            );
            case "removeFacility" -> r = shelterController.removeFacility(
                    WebUtil.reqLong(req, "id", "Shelter id"),
                    WebUtil.str(req, "facilityName")
            );
            case "allocate" -> r = shelterController.allocate(
                    WebUtil.reqLong(req, "id", "Shelter id"),
                    WebUtil.str(req, "victimId"),
                    WebUtil.str(req, "familyName"),
                    WebUtil.str(req, "people"),
                    WebUtil.opt(req, "notes")
            );
            case "release" -> r = shelterController.release(
                    WebUtil.reqLong(req, "allocationId", "Allocation")
            );
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:" + detailRedirect;
            }
        }
        return handleResult(r, ra, detailRedirect);
    }

    @GetMapping("/app/smart-allocation")
    public String smartAllocation(@RequestParam(value = "statusFilter", required = false) String statusFilterStr,
                                  Model model) throws Exception {
        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null && (u.getRole() == RoleType.RESCUE_OFFICER || u.getRole() == RoleType.CITIZEN || u.getRole() == RoleType.VOLUNTEER)) {
            return "redirect:/app/dashboard";
        }
        populateSmartAllocationModel(model, statusFilterStr);
        model.addAttribute("pageTitle", "Smart Shelter Allocation");
        return "smart-allocation";
    }

    @PostMapping("/app/smart-allocation")
    public String handleSmartAllocationAction(@RequestParam("action") String action,
                                              HttpServletRequest req,
                                              Model model,
                                              RedirectAttributes ra) throws Exception {
        if ("preview".equals(action)) {
            ActionResult r = invokeSmart(req, false, true);
            if (!r.isSuccess()) {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", r.getMessage());
                return "redirect:/app/smart-allocation";
            }
            SmartAllocationResult data = r.getData();
            populateSmartAllocationModel(model, req.getParameter("statusFilter"));
            model.addAttribute("preview", data);
            model.addAttribute("previewHeaders", SmartAllocationController.rankedHeaders());
            model.addAttribute("previewRows", rows(data.getRanked(), SmartAllocationController::rankedRow));
            model.addAttribute("flashType", "success");
            model.addAttribute("flashMessage", r.getMessage());
            model.addAttribute("pageTitle", "Smart Shelter Allocation");
            return "smart-allocation";
        }

        ActionResult r;
        switch (action) {
            case "allocate" -> r = invokeSmart(req, WebUtil.flag(req, "createPending"), false);
            case "confirmPending" -> r = smartController.confirmPending(WebUtil.reqLong(req, "allocationId", "Allocation"));
            case "checkIn" -> r = smartController.checkIn(WebUtil.reqLong(req, "allocationId", "Allocation"));
            case "complete" -> r = smartController.complete(WebUtil.reqLong(req, "allocationId", "Allocation"));
            case "cancel" -> r = smartController.cancel(WebUtil.reqLong(req, "allocationId", "Allocation"));
            case "release" -> r = smartController.release(WebUtil.reqLong(req, "allocationId", "Allocation"));
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:/app/smart-allocation";
            }
        }
        return handleResult(r, ra, "/app/smart-allocation");
    }

    private void populateSmartAllocationModel(Model model, String statusFilterStr) throws Exception {
        ShelterAllocationStatus filter = null;
        if (statusFilterStr != null && !statusFilterStr.isBlank()) {
            try {
                filter = ShelterAllocationStatus.valueOf(statusFilterStr);
            } catch (Exception ignored) {}
        }

        List<ShelterAllocation> allocations = filter == null
                ? smartController.getAllAllocations() : smartController.getByStatus(filter);
        List<Victim> victims = smartController.getVictims();
        List<Victim> waiting = smartController.getWaitingForShelter();

        model.addAttribute("victimOptions", opts(victims, Victim::getId,
                v -> "#" + v.getId() + " " + v.getFullName()));
        model.addAttribute("priorities", WebUtil.options(PriorityLevel.class));
        model.addAttribute("facilityOptions", smartController.getFacilityOptions());
        model.addAttribute("statusFilters", WebUtil.options(ShelterAllocationStatus.class));
        model.addAttribute("statusFilter", filter);
        model.addAttribute("allocations", allocations);
        model.addAttribute("allocationHeaders", SmartAllocationController.allocationHeaders());
        model.addAttribute("allocationRows", rows(allocations, SmartAllocationController::allocationRow));
        model.addAttribute("waitHeaders", SmartAllocationController.waitListHeaders());
        model.addAttribute("waitRows", rows(waiting, SmartAllocationController::waitListRow));
        model.addAttribute("countWaiting", smartController.countWaiting());
        model.addAttribute("countActive", smartController.countActive());
        model.addAttribute("countPending", smartController.countByStatus(ShelterAllocationStatus.PENDING));
    }

    private ActionResult invokeSmart(HttpServletRequest req, boolean createPending, boolean preview) {
        String[] fac = req.getParameterValues("facility");
        List<String> facilities = fac == null ? new ArrayList<>() : new ArrayList<>(List.of(fac));
        PriorityLevel priority = WebUtil.reqEnum(req, "priority", PriorityLevel.class, "Priority");
        String victim = WebUtil.str(req, "victimId");
        String family = WebUtil.str(req, "familyName");
        String people = WebUtil.str(req, "people");
        String location = WebUtil.str(req, "location");
        String notes = WebUtil.opt(req, "notes");
        boolean wc = WebUtil.flag(req, "needWheelchair");
        boolean el = WebUtil.flag(req, "needElderly");
        boolean med = WebUtil.flag(req, "needMedical");
        boolean sp = WebUtil.flag(req, "needSpecial");
        return preview
                ? smartController.preview(victim, family, people, priority, location, facilities, wc, el, med, sp, notes)
                : smartController.allocate(victim, family, people, priority, location, facilities, wc, el, med, sp,
                        notes, createPending);
    }
}
