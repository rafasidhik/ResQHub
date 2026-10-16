package com.resqhub.controller;

import com.resqhub.dto.RescueTeamDto;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.TeamAvailability;
import com.resqhub.model.User;
import com.resqhub.service.RescueService;
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
@RequestMapping("/rescue-teams")
@PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
public class RescueTeamController {

    private final RescueService rescueService;
    private final UserService userService;

    @Autowired
    public RescueTeamController(RescueService rescueService, UserService userService) {
        this.rescueService = rescueService;
        this.userService = userService;
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth != null) {
            return userService.findByUsername(auth.getName()).map(User::getId).orElse(1L);
        }
        return 1L;
    }

    @GetMapping
    public String listTeams(@RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            @RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "availability", defaultValue = "ALL") String availability,
                            @RequestParam(value = "status", defaultValue = "ACTIVE") String status,
                            Model model) {
        model.addAttribute("activeMenu", "rescue-teams");

        List<RescueTeam> teams = rescueService.getTeams(page, size, search, availability, status);
        int totalTeams = rescueService.countTeams(search, availability, status);
        int totalPages = (int) Math.ceil((double) totalTeams / size);

        model.addAttribute("teams", teams);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalTeams", totalTeams);
        model.addAttribute("search", search);
        model.addAttribute("selectedAvailability", availability);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("availabilities", TeamAvailability.values());

        return "rescue-teams/list";
    }

    @GetMapping("/{id}")
    public String viewTeamDetails(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activeMenu", "rescue-teams");
        RescueTeam team = rescueService.getTeamById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rescue team not found with ID: " + id));

        model.addAttribute("team", team);
        model.addAttribute("members", rescueService.getTeamMembers(id));
        model.addAttribute("history", rescueService.getTeamHistory(id));

        return "rescue-teams/details";
    }

    @GetMapping("/create")
    public String createTeamForm(Model model) {
        model.addAttribute("activeMenu", "rescue-teams");
        RescueTeamDto dto = new RescueTeamDto();
        dto.setAvailabilityStatus(TeamAvailability.AVAILABLE);
        dto.setStatus("ACTIVE");
        dto.setCapacity(10);

        model.addAttribute("teamDto", dto);
        model.addAttribute("availabilities", TeamAvailability.values());
        model.addAttribute("isCreate", true);

        return "rescue-teams/form";
    }

    @PostMapping("/create")
    public String createTeam(@Valid @ModelAttribute("teamDto") RescueTeamDto dto,
                             BindingResult result,
                             Authentication auth,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "rescue-teams");
            model.addAttribute("availabilities", TeamAvailability.values());
            model.addAttribute("isCreate", true);
            return "rescue-teams/form";
        }

        Long newId = rescueService.registerTeam(dto, getCurrentUserId(auth));
        redirectAttributes.addFlashAttribute("successMessage",
                "Rescue squad '" + dto.getName() + "' successfully registered.");
        return "redirect:/rescue-teams/" + newId;
    }

    @GetMapping("/edit/{id}")
    public String editTeamForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activeMenu", "rescue-teams");
        RescueTeam team = rescueService.getTeamById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rescue team not found with ID: " + id));

        RescueTeamDto dto = new RescueTeamDto();
        dto.setId(team.getId());
        dto.setTeamIdentifier(team.getTeamIdentifier());
        dto.setName(team.getName());
        dto.setTeamLeader(team.getTeamLeader());
        dto.setContactNumber(team.getContactNumber());
        dto.setSkills(team.getSkills());
        dto.setOperatingArea(team.getOperatingArea());
        dto.setCapacity(team.getCapacity());
        dto.setAvailabilityStatus(team.getAvailabilityStatus());
        dto.setStatus(team.getStatus());

        model.addAttribute("teamDto", dto);
        model.addAttribute("availabilities", TeamAvailability.values());
        model.addAttribute("isCreate", false);

        return "rescue-teams/form";
    }

    @PostMapping("/edit")
    public String updateTeam(@Valid @ModelAttribute("teamDto") RescueTeamDto dto,
                             BindingResult result,
                             Authentication auth,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "rescue-teams");
            model.addAttribute("availabilities", TeamAvailability.values());
            model.addAttribute("isCreate", false);
            return "rescue-teams/form";
        }

        try {
            rescueService.updateTeam(dto, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Rescue squad '" + dto.getName() + "' updated successfully.");
            return "redirect:/rescue-teams/" + dto.getId();
        } catch (Exception ex) {
            model.addAttribute("activeMenu", "rescue-teams");
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("availabilities", TeamAvailability.values());
            model.addAttribute("isCreate", false);
            return "rescue-teams/form";
        }
    }

    @PostMapping("/release/{id}")
    public String releaseTeam(@PathVariable("id") Long id,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            rescueService.releaseTeam(id, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Team status set to AVAILABLE.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/rescue-teams/" + id;
    }
}
