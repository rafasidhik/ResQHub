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
import com.resqhub.controller.FoodDistributionController;
import com.resqhub.model.BeneficiaryType;
import com.resqhub.model.Disaster;
import com.resqhub.model.FoodDistributionRequest;
import com.resqhub.model.FoodRequestStatus;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.Resource;
import com.resqhub.model.RoleType;
import com.resqhub.model.Shelter;
import com.resqhub.model.User;
import com.resqhub.model.Volunteer;
import com.resqhub.service.SessionManager;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class FoodWebController extends BaseWebController {

    private final FoodDistributionController foodController = new FoodDistributionController();

    @GetMapping("/app/food")
    public String listOrDetail(@RequestParam(value = "id", required = false) Long id, Model model) throws Exception {
        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null && (u.getRole() == RoleType.RESCUE_OFFICER || u.getRole() == RoleType.CITIZEN || u.getRole() == RoleType.VOLUNTEER)) {
            return "redirect:/app/dashboard";
        }
        List<Disaster> disasters = foodController.getDisasters();
        List<Resource> foodResources = foodController.getFoodResources();

        Map<Long, String> disasterNames = new HashMap<>();
        disasters.forEach(d -> disasterNames.put(d.getId(), d.getTitle()));
        Map<Long, String> resourceNames = new HashMap<>();
        foodResources.forEach(r -> resourceNames.put(r.getId(), r.getName()));

        model.addAttribute("priorities", WebUtil.options(PriorityLevel.class));
        model.addAttribute("beneficiaryTypes", WebUtil.options(BeneficiaryType.class));
        model.addAttribute("statuses", WebUtil.options(FoodRequestStatus.class));
        model.addAttribute("disasterOptions", opts(disasters, Disaster::getId, d -> "#" + d.getId() + " " + d.getTitle()));
        model.addAttribute("foodResourceOptions", opts(foodResources, Resource::getId,
                r -> r.getName() + " (" + r.getAvailableQuantity() + " " + (r.getUnit() == null ? "" : r.getUnit()) + " in stock)"));

        if (id == null) {
            List<FoodDistributionRequest> list = foodController.getAllRequests();
            List<Shelter> shelters = foodController.getAllShelters();

            model.addAttribute("items", list);
            model.addAttribute("headers", FoodDistributionController.requestHeaders());
            model.addAttribute("rows", rows(list, r -> FoodDistributionController.requestRow(r, disasterNames)));
            model.addAttribute("shelterOptions", opts(shelters, Shelter::getId, s -> s.getCode() + " " + s.getName()));
            model.addAttribute("sOpen", foodController.countOpen());
            model.addAttribute("sPending", foodController.countPending());
            model.addAttribute("sDone", foodController.countCompleted());
            model.addAttribute("sBeneficiaries", foodController.totalBeneficiaries());
            model.addAttribute("sRequired", foodController.totalRequired());
            model.addAttribute("sDistributed", foodController.totalDistributed());
            model.addAttribute("sRemaining", foodController.totalRemaining());
            model.addAttribute("pageTitle", "Food Distribution");
            return "food";
        }

        FoodDistributionRequest r = foodController.getRequest(id);
        if (r == null) {
            model.addAttribute("errorTitle", "Food request not found");
            model.addAttribute("errorMessage", "Request #" + id + " does not exist.");
            return "error";
        }

        var dists = foodController.getDistributions(id);
        List<Volunteer> volunteers = foodController.getAllVolunteers();

        model.addAttribute("r", r);
        model.addAttribute("disasterName", r.getDisasterId() == null ? "—" : disasterNames.getOrDefault(r.getDisasterId(), "?"));
        model.addAttribute("allocatedResource", r.getAllocatedResourceId() == null ? "—"
                : resourceNames.getOrDefault(r.getAllocatedResourceId(), "#" + r.getAllocatedResourceId()));
        model.addAttribute("volunteerOptions", opts(volunteers, Volunteer::getId, v -> v.getFullName() + " (" + v.getLocation() + ")"));
        model.addAttribute("assignedVolunteer", r.getAssignedVolunteerId() == null ? "—" : volunteers.stream()
                .filter(v -> v.getId().equals(r.getAssignedVolunteerId())).map(Volunteer::getFullName)
                .findFirst().orElse("#" + r.getAssignedVolunteerId()));
        model.addAttribute("distHeaders", FoodDistributionController.distributionHeaders());
        model.addAttribute("distRows", rows(dists, d -> FoodDistributionController.distributionRow(d,
                r.getRequestCode(), resourceNames.getOrDefault(d.getResourceId(), "?"))));
        model.addAttribute("pageTitle", "Food request " + r.getRequestCode());
        return "food-detail";
    }

    @PostMapping("/app/food")
    public String handleAction(@RequestParam("action") String action,
                               HttpServletRequest req,
                               RedirectAttributes ra) throws Exception {
        ActionResult r;
        String idParam = req.getParameter("id");
        String detailRedirect = idParam != null && !idParam.isBlank() ? "/app/food?id=" + idParam : "/app/food";

        switch (action) {
            case "create" -> {
                String meals = WebUtil.str(req, "mealsPerPerson");
                if (!meals.isEmpty()) {
                    r = foodController.createRequestWithCalculation(
                            WebUtil.str(req, "code"),
                            WebUtil.str(req, "disasterId"),
                            WebUtil.str(req, "location"),
                            WebUtil.optEnum(req, "beneficiaryType", BeneficiaryType.class, "Beneficiary type"),
                            WebUtil.str(req, "beneficiaries"),
                            meals,
                            WebUtil.optEnum(req, "priority", PriorityLevel.class, "Priority"),
                            WebUtil.opt(req, "description")
                    );
                } else {
                    r = foodController.createRequest(
                            WebUtil.str(req, "code"),
                            WebUtil.str(req, "disasterId"),
                            WebUtil.str(req, "location"),
                            WebUtil.optEnum(req, "beneficiaryType", BeneficiaryType.class, "Beneficiary type"),
                            WebUtil.str(req, "beneficiaries"),
                            WebUtil.str(req, "required"),
                            WebUtil.optEnum(req, "priority", PriorityLevel.class, "Priority"),
                            WebUtil.opt(req, "description")
                    );
                }
                return handleResult(r, ra, "/app/food");
            }
            case "fromShelter" -> {
                r = foodController.createRequestFromShelter(
                        WebUtil.reqLong(req, "shelterId", "Shelter"),
                        WebUtil.str(req, "mealsPerPerson"),
                        WebUtil.reqEnum(req, "priority", PriorityLevel.class, "Priority")
                );
                return handleResult(r, ra, "/app/food");
            }
            case "update" -> r = foodController.updateRequest(
                    WebUtil.reqLong(req, "id", "Request id"),
                    WebUtil.str(req, "location"),
                    WebUtil.optEnum(req, "beneficiaryType", BeneficiaryType.class, "Beneficiary type"),
                    WebUtil.str(req, "beneficiaries"),
                    WebUtil.str(req, "required"),
                    WebUtil.optEnum(req, "priority", PriorityLevel.class, "Priority"),
                    WebUtil.opt(req, "description")
            );
            case "approve" -> r = foodController.approveRequest(WebUtil.reqLong(req, "id", "Request id"));
            case "allocate" -> r = foodController.allocateRequest(
                    WebUtil.reqLong(req, "id", "Request id"),
                    WebUtil.str(req, "resourceId"),
                    WebUtil.str(req, "quantity")
            );
            case "assignVolunteer" -> r = foodController.assignVolunteer(
                    WebUtil.reqLong(req, "id", "Request id"),
                    WebUtil.str(req, "volunteerId")
            );
            case "distribute" -> r = foodController.recordDistribution(
                    WebUtil.reqLong(req, "id", "Request id"),
                    WebUtil.str(req, "resourceId"),
                    WebUtil.str(req, "quantity"),
                    WebUtil.str(req, "served"),
                    WebUtil.str(req, "location"),
                    WebUtil.opt(req, "note")
            );
            case "status" -> r = foodController.setStatus(
                    WebUtil.reqLong(req, "id", "Request id"),
                    WebUtil.reqEnum(req, "status", FoodRequestStatus.class, "Status")
            );
            case "cancel" -> r = foodController.cancelRequest(WebUtil.reqLong(req, "id", "Request id"));
            case "alerts" -> r = foodController.generateFoodShortageAlerts();
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:" + detailRedirect;
            }
        }
        return handleResult(r, ra, detailRedirect);
    }
}
