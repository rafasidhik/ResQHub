package com.resqhub.controller;

import com.resqhub.dto.ResidentCheckInDto;
import com.resqhub.dto.ShelterDto;
import com.resqhub.dto.ShelterRecommendation;
import com.resqhub.model.*;
import com.resqhub.service.DisasterService;
import com.resqhub.service.ShelterService;
import com.resqhub.service.UserService;
import com.resqhub.service.VictimService;
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
@RequestMapping("/shelters")
public class ShelterController {

    private final ShelterService shelterService;
    private final DisasterService disasterService;
    private final VictimService victimService;
    private final UserService userService;

    @Autowired
    public ShelterController(ShelterService shelterService,
                             DisasterService disasterService,
                             VictimService victimService,
                             UserService userService) {
        this.shelterService = shelterService;
        this.disasterService = disasterService;
        this.victimService = victimService;
        this.userService = userService;
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return userService.findByUsername(auth.getName()).map(User::getId).orElse(null);
        }
        return null;
    }

    @GetMapping
    public String listShelters(@RequestParam(value = "status", required = false) String statusFilter,
                               Model model) {
        model.addAttribute("activeMenu", "shelters");

        List<Shelter> shelters;
        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
            try {
                ShelterStatus status = ShelterStatus.valueOf(statusFilter.toUpperCase());
                shelters = shelterService.getAllShelters().stream()
                        .filter(s -> s.getStatus() == status)
                        .toList();
            } catch (IllegalArgumentException e) {
                shelters = shelterService.getAllShelters();
            }
        } else {
            shelters = shelterService.getAllShelters();
        }

        model.addAttribute("shelters", shelters);
        model.addAttribute("selectedStatus", statusFilter != null ? statusFilter : "ALL");
        model.addAttribute("totalShelters", shelterService.getTotalShelterCount());
        model.addAttribute("totalCapacity", shelterService.getTotalCapacity());
        model.addAttribute("currentOccupancy", shelterService.getCurrentOccupancy());
        model.addAttribute("overallOccupancyPercentage", shelterService.getOverallOccupancyPercentage());

        return "shelters/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String showCreateForm(Model model) {
        model.addAttribute("activeMenu", "shelters");
        if (!model.containsAttribute("shelterDto")) {
            model.addAttribute("shelterDto", new ShelterDto());
        }
        model.addAttribute("statuses", ShelterStatus.values());
        return "shelters/form";
    }

    @PostMapping("/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String createShelter(@Valid @ModelAttribute("shelterDto") ShelterDto dto,
                                BindingResult bindingResult,
                                Authentication auth,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "shelters");
            model.addAttribute("statuses", ShelterStatus.values());
            return "shelters/form";
        }

        try {
            Long userId = getCurrentUserId(auth);
            Shelter created = shelterService.createShelter(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Shelter '" + created.getName() + "' (" + created.getShelterIdentifier() + ") registered successfully.");
            return "redirect:/shelters/" + created.getId();
        } catch (Exception e) {
            model.addAttribute("activeMenu", "shelters");
            model.addAttribute("errorMessage", "Failed to register shelter: " + e.getMessage());
            model.addAttribute("statuses", ShelterStatus.values());
            return "shelters/form";
        }
    }

    @GetMapping("/{id}")
    public String viewShelter(@PathVariable Long id, Model model) {
        model.addAttribute("activeMenu", "shelters");

        Shelter shelter = shelterService.getShelterById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shelter not found with id: " + id));

        model.addAttribute("shelter", shelter);
        model.addAttribute("activeResidents", shelterService.getActiveResidentsByShelter(id));
        model.addAttribute("allDisasters", disasterService.getActiveDisasters());
        model.addAttribute("allVictims", victimService.getVictims(0, 100, null, null, "ALL", "ALL"));

        if (!model.containsAttribute("checkInDto")) {
            ResidentCheckInDto checkInDto = new ResidentCheckInDto();
            checkInDto.setShelterId(shelter.getId());
            model.addAttribute("checkInDto", checkInDto);
        }

        return "shelters/details";
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("activeMenu", "shelters");

        Shelter shelter = shelterService.getShelterById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shelter not found with id: " + id));

        if (!model.containsAttribute("shelterDto")) {
            ShelterDto dto = new ShelterDto();
            dto.setId(shelter.getId());
            dto.setShelterIdentifier(shelter.getShelterIdentifier());
            dto.setName(shelter.getName());
            dto.setAddress(shelter.getAddress());
            dto.setLocationDescription(shelter.getLocationDescription());
            dto.setContactPerson(shelter.getContactPerson());
            dto.setContactNumber(shelter.getContactNumber());
            dto.setTotalCapacity(shelter.getTotalCapacity());
            dto.setStatus(shelter.getStatus());
            model.addAttribute("shelterDto", dto);
        }

        model.addAttribute("shelter", shelter);
        model.addAttribute("statuses", ShelterStatus.values());
        return "shelters/form";
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String updateShelter(@PathVariable Long id,
                                @Valid @ModelAttribute("shelterDto") ShelterDto dto,
                                BindingResult bindingResult,
                                Authentication auth,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "shelters");
            model.addAttribute("statuses", ShelterStatus.values());
            return "shelters/form";
        }

        try {
            Long userId = getCurrentUserId(auth);
            Shelter updated = shelterService.updateShelter(id, dto, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Shelter '" + updated.getName() + "' updated successfully.");
            return "redirect:/shelters/" + id;
        } catch (Exception e) {
            model.addAttribute("activeMenu", "shelters");
            model.addAttribute("errorMessage", "Failed to update shelter: " + e.getMessage());
            model.addAttribute("statuses", ShelterStatus.values());
            return "shelters/form";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteShelter(@PathVariable Long id,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            shelterService.deleteShelter(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Shelter successfully deleted.");
            return "redirect:/shelters";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/shelters/" + id;
        }
    }

    @GetMapping("/{id}/residents")
    public String viewResidents(@PathVariable Long id, Model model) {
        model.addAttribute("activeMenu", "shelters");

        Shelter shelter = shelterService.getShelterById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shelter not found with id: " + id));

        model.addAttribute("shelter", shelter);
        model.addAttribute("residents", shelterService.getResidentsByShelter(id));
        return "shelters/residents";
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String checkInResident(@PathVariable Long id,
                                  @Valid @ModelAttribute("checkInDto") ResidentCheckInDto dto,
                                  BindingResult bindingResult,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        dto.setShelterId(id);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.checkInDto", bindingResult);
            redirectAttributes.addFlashAttribute("checkInDto", dto);
            redirectAttributes.addFlashAttribute("errorMessage", "Validation failed. Please verify family size and required fields.");
            return "redirect:/shelters/" + id;
        }

        try {
            Long userId = getCurrentUserId(auth);
            ShelterResident resident = shelterService.checkInResident(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Successfully checked in " + resident.getResidentName() + " (family of " + resident.getFamilySize() + ").");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Check-in failed: " + e.getMessage());
        }

        return "redirect:/shelters/" + id;
    }

    @PostMapping("/residents/{residentId}/check-out")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String checkOutResident(@PathVariable Long residentId,
                                   @RequestParam(value = "shelterId", required = false) Long shelterId,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            shelterService.checkOutResident(residentId, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Resident successfully checked out.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Check-out failed: " + e.getMessage());
        }

        if (shelterId != null) {
            return "redirect:/shelters/" + shelterId;
        }
        return "redirect:/shelters";
    }

    @GetMapping("/smart-allocate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String smartAllocateView(@RequestParam(value = "disasterId", required = false) Long disasterId,
                                    @RequestParam(value = "peopleCount", defaultValue = "5") int peopleCount,
                                    Model model) {
        model.addAttribute("activeMenu", "shelters");
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("selectedDisasterId", disasterId);
        model.addAttribute("peopleCount", peopleCount);

        if (disasterId != null) {
            List<ShelterRecommendation> recommendations = shelterService.recommendShelters(disasterId, peopleCount);
            model.addAttribute("recommendations", recommendations);
            model.addAttribute("allocations", shelterService.getAllocationsByDisaster(disasterId));
        }

        return "shelters/smart-allocate";
    }

    @PostMapping("/allocations/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String createAllocation(@RequestParam("disasterId") Long disasterId,
                                   @RequestParam("shelterId") Long shelterId,
                                   @RequestParam("peopleCount") int peopleCount,
                                   @RequestParam("reason") String reason,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            shelterService.createAllocation(disasterId, shelterId, peopleCount, reason, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Shelter allocation recorded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Allocation failed: " + e.getMessage());
        }
        return "redirect:/shelters/smart-allocate?disasterId=" + disasterId + "&peopleCount=" + peopleCount;
    }

    @PostMapping("/allocations/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String confirmAllocation(@PathVariable Long id,
                                    @RequestParam(value = "disasterId", required = false) Long disasterId,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            shelterService.confirmAllocation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Shelter allocation confirmed.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to confirm allocation: " + e.getMessage());
        }
        if (disasterId != null) {
            return "redirect:/shelters/smart-allocate?disasterId=" + disasterId;
        }
        return "redirect:/shelters";
    }

    @PostMapping("/allocations/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String rejectAllocation(@PathVariable Long id,
                                   @RequestParam(value = "disasterId", required = false) Long disasterId,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            shelterService.rejectAllocation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Shelter allocation rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to reject allocation: " + e.getMessage());
        }
        if (disasterId != null) {
            return "redirect:/shelters/smart-allocate?disasterId=" + disasterId;
        }
        return "redirect:/shelters";
    }
}
