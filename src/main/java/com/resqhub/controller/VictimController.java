package com.resqhub.controller;

import com.resqhub.dto.VictimFormDto;
import com.resqhub.model.RescueStatus;
import com.resqhub.model.User;
import com.resqhub.model.Victim;
import com.resqhub.model.VictimPriority;
import com.resqhub.service.DisasterService;
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
@RequestMapping("/victims")
@PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
public class VictimController {

    private final VictimService victimService;
    private final DisasterService disasterService;
    private final UserService userService;

    @Autowired
    public VictimController(VictimService victimService, DisasterService disasterService, UserService userService) {
        this.victimService = victimService;
        this.disasterService = disasterService;
        this.userService = userService;
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth != null) {
            return userService.findByUsername(auth.getName()).map(User::getId).orElse(1L);
        }
        return 1L;
    }

    @GetMapping
    public String listVictims(@RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "10") int size,
                             @RequestParam(value = "search", required = false) String search,
                             @RequestParam(value = "disasterId", required = false) Long disasterId,
                             @RequestParam(value = "status", defaultValue = "ALL") String status,
                             @RequestParam(value = "priority", defaultValue = "ALL") String priority,
                             Model model) {
        model.addAttribute("activeMenu", "victims");

        List<Victim> victims = victimService.getVictims(page, size, search, disasterId, status, priority);
        int totalVictims = victimService.countVictims(search, disasterId, status, priority);
        int totalPages = (int) Math.ceil((double) totalVictims / size);

        model.addAttribute("victims", victims);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalVictims", totalVictims);
        model.addAttribute("search", search);
        model.addAttribute("selectedDisasterId", disasterId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);

        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("statuses", RescueStatus.values());
        model.addAttribute("priorities", VictimPriority.values());

        return "victims/list";
    }

    @GetMapping("/{id}")
    public String viewVictimDetails(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activeMenu", "victims");
        Victim victim = victimService.getVictimById(id)
                .orElseThrow(() -> new IllegalArgumentException("Victim record not found with ID: " + id));

        model.addAttribute("victim", victim);
        model.addAttribute("statuses", RescueStatus.values());
        return "victims/details";
    }

    @GetMapping("/create")
    public String createVictimForm(@RequestParam(value = "disasterId", required = false) Long disasterId,
                                  Model model) {
        model.addAttribute("activeMenu", "victims");
        VictimFormDto dto = new VictimFormDto();
        if (disasterId != null) {
            dto.setDisasterId(disasterId);
        }
        model.addAttribute("victimDto", dto);
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("statuses", RescueStatus.values());
        model.addAttribute("priorities", VictimPriority.values());
        model.addAttribute("isCreate", true);
        return "victims/form";
    }

    @PostMapping("/create")
    public String createVictim(@Valid @ModelAttribute("victimDto") VictimFormDto dto,
                               BindingResult result,
                               Authentication auth,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "victims");
            model.addAttribute("disasters", disasterService.getActiveDisasters());
            model.addAttribute("statuses", RescueStatus.values());
            model.addAttribute("priorities", VictimPriority.values());
            model.addAttribute("isCreate", true);
            return "victims/form";
        }

        Long newId = victimService.registerVictim(dto, getCurrentUserId(auth));
        redirectAttributes.addFlashAttribute("successMessage",
                "Victim '" + dto.getFullName() + "' has been successfully registered.");
        return "redirect:/victims/" + newId;
    }

    @GetMapping("/edit/{id}")
    public String editVictimForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activeMenu", "victims");
        Victim victim = victimService.getVictimById(id)
                .orElseThrow(() -> new IllegalArgumentException("Victim record not found with ID: " + id));

        VictimFormDto dto = new VictimFormDto();
        dto.setId(victim.getId());
        dto.setDisasterId(victim.getDisasterId());
        dto.setFullName(victim.getFullName());
        dto.setAge(victim.getAge());
        dto.setContactNumber(victim.getContactNumber());
        dto.setLocation(victim.getLocation());
        dto.setAffectedFamilyMembers(victim.getAffectedFamilyMembers());
        dto.setAccessibilityNeeds(victim.getAccessibilityNeeds());
        dto.setCurrentSituation(victim.getCurrentSituation());
        dto.setRescueStatus(victim.getRescueStatus());
        dto.setPriority(victim.getPriority());

        model.addAttribute("victimDto", dto);
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("statuses", RescueStatus.values());
        model.addAttribute("priorities", VictimPriority.values());
        model.addAttribute("isCreate", false);
        return "victims/form";
    }

    @PostMapping("/edit")
    public String updateVictim(@Valid @ModelAttribute("victimDto") VictimFormDto dto,
                               BindingResult result,
                               Authentication auth,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "victims");
            model.addAttribute("disasters", disasterService.getActiveDisasters());
            model.addAttribute("statuses", RescueStatus.values());
            model.addAttribute("priorities", VictimPriority.values());
            model.addAttribute("isCreate", false);
            return "victims/form";
        }

        try {
            victimService.updateVictim(dto, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Victim record for '" + dto.getFullName() + "' has been updated.");
            return "redirect:/victims/" + dto.getId();
        } catch (Exception ex) {
            model.addAttribute("activeMenu", "victims");
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("disasters", disasterService.getActiveDisasters());
            model.addAttribute("statuses", RescueStatus.values());
            model.addAttribute("priorities", VictimPriority.values());
            model.addAttribute("isCreate", false);
            return "victims/form";
        }
    }

    @PostMapping("/status/{id}")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") RescueStatus status,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            victimService.updateRescueStatus(id, status, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Rescue status updated to " + status.getDisplayName());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/victims/" + id;
    }
}
