package com.resqhub.controller;

import com.resqhub.dto.DisasterFormDto;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.model.Severity;
import com.resqhub.model.User;
import com.resqhub.service.DisasterService;
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
@RequestMapping("/disasters")
public class DisasterController {

    private final DisasterService disasterService;
    private final UserService userService;

    @Autowired
    public DisasterController(DisasterService disasterService, UserService userService) {
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
    public String listDisasters(@RequestParam(value = "page", defaultValue = "0") int page,
                                @RequestParam(value = "size", defaultValue = "9") int size,
                                @RequestParam(value = "search", required = false) String search,
                                @RequestParam(value = "status", defaultValue = "ALL") String status,
                                @RequestParam(value = "severity", defaultValue = "ALL") String severity,
                                @RequestParam(value = "type", defaultValue = "ALL") String type,
                                Model model) {
        model.addAttribute("activeMenu", "disasters");

        List<Disaster> disasters = disasterService.getDisasters(page, size, search, status, severity, type);
        int totalDisasters = disasterService.countDisasters(search, status, severity, type);
        int totalPages = (int) Math.ceil((double) totalDisasters / size);

        model.addAttribute("disasters", disasters);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalDisasters", totalDisasters);
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSeverity", severity);
        model.addAttribute("selectedType", type);

        model.addAttribute("statuses", DisasterStatus.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("types", DisasterType.values());

        return "disasters/list";
    }

    @GetMapping("/{id}")
    public String viewDisasterDetails(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activeMenu", "disasters");
        Disaster disaster = disasterService.getDisasterById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disaster record not found with ID: " + id));

        model.addAttribute("disaster", disaster);
        return "disasters/details";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String createDisasterForm(Model model) {
        model.addAttribute("activeMenu", "disasters");
        DisasterFormDto dto = new DisasterFormDto();
        model.addAttribute("disasterDto", dto);
        model.addAttribute("statuses", DisasterStatus.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("types", DisasterType.values());
        model.addAttribute("isCreate", true);
        return "disasters/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String createDisaster(@Valid @ModelAttribute("disasterDto") DisasterFormDto dto,
                                 BindingResult result,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "disasters");
            model.addAttribute("statuses", DisasterStatus.values());
            model.addAttribute("severities", Severity.values());
            model.addAttribute("types", DisasterType.values());
            model.addAttribute("isCreate", true);
            return "disasters/form";
        }

        Long newId = disasterService.createDisaster(dto, getCurrentUserId(auth));
        redirectAttributes.addFlashAttribute("successMessage",
                "Disaster incident '" + dto.getName() + "' has been successfully registered.");
        return "redirect:/disasters/" + newId;
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String editDisasterForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activeMenu", "disasters");
        Disaster disaster = disasterService.getDisasterById(id)
                .orElseThrow(() -> new IllegalArgumentException("Disaster record not found with ID: " + id));

        DisasterFormDto dto = new DisasterFormDto();
        dto.setId(disaster.getId());
        dto.setName(disaster.getName());
        dto.setDisasterType(disaster.getDisasterType());
        dto.setDescription(disaster.getDescription());
        dto.setLocation(disaster.getLocation());
        dto.setSeverity(disaster.getSeverity());
        dto.setStatus(disaster.getStatus());
        dto.setStartDate(disaster.getStartDate());

        model.addAttribute("disasterDto", dto);
        model.addAttribute("statuses", DisasterStatus.values());
        model.addAttribute("severities", Severity.values());
        model.addAttribute("types", DisasterType.values());
        model.addAttribute("isCreate", false);
        return "disasters/form";
    }

    @PostMapping("/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String updateDisaster(@Valid @ModelAttribute("disasterDto") DisasterFormDto dto,
                                 BindingResult result,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "disasters");
            model.addAttribute("statuses", DisasterStatus.values());
            model.addAttribute("severities", Severity.values());
            model.addAttribute("types", DisasterType.values());
            model.addAttribute("isCreate", false);
            return "disasters/form";
        }

        try {
            disasterService.updateDisaster(dto, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Disaster incident '" + dto.getName() + "' updated successfully.");
            return "redirect:/disasters/" + dto.getId();
        } catch (Exception ex) {
            model.addAttribute("activeMenu", "disasters");
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("statuses", DisasterStatus.values());
            model.addAttribute("severities", Severity.values());
            model.addAttribute("types", DisasterType.values());
            model.addAttribute("isCreate", false);
            return "disasters/form";
        }
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteDisaster(@PathVariable("id") Long id,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            disasterService.deleteDisaster(id, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Disaster record deleted successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/disasters";
    }
}
