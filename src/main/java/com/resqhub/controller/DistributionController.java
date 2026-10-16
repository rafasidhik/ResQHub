package com.resqhub.controller;

import com.resqhub.dto.FoodDistributionDto;
import com.resqhub.dto.ResourceAllocationDto;
import com.resqhub.model.*;
import com.resqhub.service.DisasterService;
import com.resqhub.service.ResourceService;
import com.resqhub.service.ShelterService;
import com.resqhub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/resources")
public class DistributionController {

    private final ResourceService resourceService;
    private final DisasterService disasterService;
    private final ShelterService shelterService;
    private final UserService userService;

    @Autowired
    public DistributionController(ResourceService resourceService,
                                  DisasterService disasterService,
                                  ShelterService shelterService,
                                  UserService userService) {
        this.resourceService = resourceService;
        this.disasterService = disasterService;
        this.shelterService = shelterService;
        this.userService = userService;
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return userService.findByUsername(auth.getName()).map(User::getId).orElse(null);
        }
        return null;
    }

    @GetMapping("/allocations")
    public String listAllocations(@RequestParam(value = "disasterId", required = false) Long disasterId,
                                  @RequestParam(value = "status", required = false) String statusFilter,
                                  Model model) {
        model.addAttribute("activeMenu", "allocations");

        List<ResourceAllocation> allocations;
        if (disasterId != null) {
            allocations = resourceService.getAllocationsByDisaster(disasterId);
        } else {
            allocations = resourceService.getAllAllocations();
        }

        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
            try {
                ResourceAllocationStatus status = ResourceAllocationStatus.valueOf(statusFilter.toUpperCase());
                allocations = allocations.stream().filter(a -> a.getStatus() == status).toList();
            } catch (IllegalArgumentException ignored) {}
        }

        model.addAttribute("allocations", allocations);
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("selectedDisasterId", disasterId);
        model.addAttribute("selectedStatus", statusFilter != null ? statusFilter : "ALL");
        model.addAttribute("statuses", ResourceAllocationStatus.values());

        return "resources/allocations-list";
    }

    @GetMapping("/allocations/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String showCreateAllocationForm(Model model) {
        model.addAttribute("activeMenu", "allocations");
        if (!model.containsAttribute("allocationDto")) {
            model.addAttribute("allocationDto", new ResourceAllocationDto());
        }
        model.addAttribute("inventoryItems", resourceService.getAllItems());
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("shelters", shelterService.getAllShelters());
        return "resources/allocation-form";
    }

    @PostMapping("/allocations/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String createAllocation(@Valid @ModelAttribute("allocationDto") ResourceAllocationDto dto,
                                   BindingResult bindingResult,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "allocations");
            model.addAttribute("inventoryItems", resourceService.getAllItems());
            model.addAttribute("disasters", disasterService.getActiveDisasters());
            model.addAttribute("shelters", shelterService.getAllShelters());
            return "resources/allocation-form";
        }

        try {
            Long userId = getCurrentUserId(auth);
            ResourceAllocation created = resourceService.createAllocation(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Resource allocation plan created successfully for " + created.getDestinationLocation() + ".");
            return "redirect:/resources/allocations";
        } catch (Exception e) {
            model.addAttribute("activeMenu", "allocations");
            model.addAttribute("errorMessage", "Failed to create allocation plan: " + e.getMessage());
            model.addAttribute("inventoryItems", resourceService.getAllItems());
            model.addAttribute("disasters", disasterService.getActiveDisasters());
            model.addAttribute("shelters", shelterService.getAllShelters());
            return "resources/allocation-form";
        }
    }

    @PostMapping("/allocations/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String approveAllocation(@PathVariable Long id,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            resourceService.approveAllocation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Resource allocation #" + id + " approved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Approval failed: " + e.getMessage());
        }
        return "redirect:/resources/allocations";
    }

    @PostMapping("/allocations/{id}/dispatch")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String dispatchAllocation(@PathVariable Long id,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            resourceService.dispatchAllocation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Resource allocation #" + id + " dispatched! Stock deducted atomically from inventory.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Dispatch failed: " + e.getMessage());
        }
        return "redirect:/resources/allocations";
    }

    @PostMapping("/allocations/{id}/deliver")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String deliverAllocation(@PathVariable Long id,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            resourceService.deliverAllocation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Resource allocation #" + id + " confirmed as DELIVERED.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Delivery update failed: " + e.getMessage());
        }
        return "redirect:/resources/allocations";
    }

    @PostMapping("/allocations/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String cancelAllocation(@PathVariable Long id,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            resourceService.cancelAllocation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Resource allocation #" + id + " cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cancellation failed: " + e.getMessage());
        }
        return "redirect:/resources/allocations";
    }

    @GetMapping("/food-distributions")
    public String listFoodDistributions(Model model) {
        model.addAttribute("activeMenu", "food-distributions");
        model.addAttribute("distributions", resourceService.getAllFoodDistributions());
        model.addAttribute("totalServed", resourceService.getTotalRecipientsServed());
        model.addAttribute("foodItems", resourceService.getAllItems().stream()
                .filter(i -> i.getCategory() == ItemCategory.FOOD || i.getCategory() == ItemCategory.DRINKING_WATER)
                .toList());
        model.addAttribute("shelters", shelterService.getAllShelters());

        if (!model.containsAttribute("distributionDto")) {
            model.addAttribute("distributionDto", new FoodDistributionDto());
        }

        return "resources/food-distributions";
    }

    @PostMapping("/food-distributions/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String createFoodDistribution(@Valid @ModelAttribute("distributionDto") FoodDistributionDto dto,
                                         BindingResult bindingResult,
                                         Authentication auth,
                                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.distributionDto", bindingResult);
            redirectAttributes.addFlashAttribute("distributionDto", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed. Please verify recipient count and food item selection.");
            return "redirect:/resources/food-distributions";
        }

        try {
            Long userId = getCurrentUserId(auth);
            FoodDistribution created = resourceService.createFoodDistribution(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Food distribution event recorded: " + created.getRecipientCount() + " served at " + created.getLocation() + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to record food distribution: " + e.getMessage());
        }

        return "redirect:/resources/food-distributions";
    }

    @PostMapping("/food-distributions/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String completeFoodDistribution(@PathVariable Long id,
                                           Authentication auth,
                                           RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            resourceService.completeFoodDistribution(id, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Food distribution marked as COMPLETED! Inventory stock updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Completion failed: " + e.getMessage());
        }
        return "redirect:/resources/food-distributions";
    }
}
