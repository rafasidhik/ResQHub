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
import com.resqhub.controller.ResourceController;
import com.resqhub.controller.ShelterController;
import com.resqhub.controller.VictimController;
import com.resqhub.model.Disaster;
import com.resqhub.model.DistributionDestination;
import com.resqhub.model.Resource;
import com.resqhub.model.ResourceCategory;
import com.resqhub.model.RoleType;
import com.resqhub.model.Shelter;
import com.resqhub.model.User;
import com.resqhub.model.Victim;
import com.resqhub.service.SessionManager;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ResourceWebController extends BaseWebController {

    private final ResourceController resourceController = new ResourceController();
    private final DisasterController disasterController = new DisasterController();
    private final ShelterController shelterController = new ShelterController();
    private final VictimController victimController = new VictimController();

    @GetMapping("/app/resources")
    public String list(Model model) throws Exception {
        User u = SessionManager.getInstance().getCurrentUser();
        if (u != null && (u.getRole() == RoleType.RESCUE_OFFICER || u.getRole() == RoleType.CITIZEN || u.getRole() == RoleType.VOLUNTEER)) {
            return "redirect:/app/dashboard";
        }
        List<Resource> list = resourceController.getAllResources();
        Map<Long, String> names = new HashMap<>();
        list.forEach(r -> names.put(r.getId(), r.getName()));
        var movements = resourceController.getAllMovements();
        var distributions = resourceController.getAllDistributions();

        model.addAttribute("items", list);
        model.addAttribute("headers", ResourceController.resourceHeaders());
        model.addAttribute("rows", rows(list, ResourceController::resourceRow));
        model.addAttribute("lowStock", resourceController.getLowStock());
        model.addAttribute("movementHeaders", ResourceController.movementHeaders());
        model.addAttribute("movementRows", rows(movements,
                m -> ResourceController.movementRow(m, names.getOrDefault(m.getResourceId(), "?"))));
        model.addAttribute("distributionHeaders", ResourceController.distributionHeaders());
        model.addAttribute("distributionRows", rows(distributions,
                d -> ResourceController.distributionRow(d, names.getOrDefault(d.getResourceId(), "?"))));
        model.addAttribute("categories", WebUtil.options(ResourceCategory.class));
        model.addAttribute("destinations", WebUtil.options(DistributionDestination.class));

        List<Disaster> disasters = disasterController.getAllDisasters();
        List<Shelter> shelters = shelterController.getAllShelters();
        List<Victim> victims = victimController.getAllVictims();

        model.addAttribute("disasterOptions", opts(disasters, Disaster::getId, d -> "#" + d.getId() + " " + d.getTitle()));
        model.addAttribute("shelterOptions", opts(shelters, Shelter::getId, s -> s.getCode() + " " + s.getName()));
        model.addAttribute("victimOptions", opts(victims, Victim::getId, v -> "#" + v.getId() + " " + v.getFullName()));

        User currentUser = SessionManager.getInstance().getCurrentUser();
        boolean canAdminDelete = currentUser != null && currentUser.getRole() == RoleType.ADMIN;
        model.addAttribute("canAdminDelete", canAdminDelete);
        model.addAttribute("pageTitle", "Resources & Inventory");
        return "resources";
    }

    @PostMapping("/app/resources")
    public String handleAction(@RequestParam("action") String action,
                               HttpServletRequest req,
                               RedirectAttributes ra) throws Exception {
        ActionResult r;
        switch (action) {
            case "create" -> r = resourceController.createResource(
                    WebUtil.str(req, "name"),
                    WebUtil.str(req, "code"),
                    WebUtil.optEnum(req, "category", ResourceCategory.class, "Category"),
                    WebUtil.str(req, "available"),
                    WebUtil.str(req, "minLevel"),
                    WebUtil.str(req, "unit"),
                    WebUtil.opt(req, "description")
            );
            case "update" -> r = resourceController.updateResource(
                    WebUtil.reqLong(req, "id", "Resource id"),
                    WebUtil.str(req, "name"),
                    WebUtil.optEnum(req, "category", ResourceCategory.class, "Category"),
                    WebUtil.str(req, "minLevel"),
                    WebUtil.str(req, "unit"),
                    WebUtil.opt(req, "description")
            );
            case "delete" -> r = resourceController.deleteResource(
                    WebUtil.reqLong(req, "id", "Resource id")
            );
            case "stockIn" -> r = resourceController.stockIn(
                    WebUtil.reqLong(req, "id", "Resource id"),
                    WebUtil.str(req, "quantity"),
                    WebUtil.opt(req, "source"),
                    WebUtil.opt(req, "reason"),
                    WebUtil.str(req, "disasterId")
            );
            case "stockOut" -> r = resourceController.stockOut(
                    WebUtil.reqLong(req, "id", "Resource id"),
                    WebUtil.str(req, "quantity"),
                    WebUtil.opt(req, "destination"),
                    WebUtil.opt(req, "reason"),
                    WebUtil.str(req, "disasterId")
            );
            case "distribute" -> r = resourceController.distribute(
                    WebUtil.reqLong(req, "id", "Resource id"),
                    WebUtil.str(req, "quantity"),
                    WebUtil.reqEnum(req, "destination", DistributionDestination.class, "Destination"),
                    WebUtil.str(req, "distributedTo"),
                    WebUtil.str(req, "disasterId"),
                    WebUtil.str(req, "shelterId"),
                    WebUtil.str(req, "victimId"),
                    WebUtil.opt(req, "reason")
            );
            case "alerts" -> r = resourceController.generateLowStockAlerts();
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:/app/resources";
            }
        }
        return handleResult(r, ra, "/app/resources");
    }
}
