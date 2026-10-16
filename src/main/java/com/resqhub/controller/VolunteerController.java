package com.resqhub.controller;

import com.resqhub.dto.VolunteerAssignmentDto;
import com.resqhub.dto.VolunteerRegistrationDto;
import com.resqhub.model.*;
import com.resqhub.service.DisasterService;
import com.resqhub.service.UserService;
import com.resqhub.service.VolunteerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/volunteers")
public class VolunteerController {

    private final VolunteerService volunteerService;
    private final DisasterService disasterService;
    private final UserService userService;

    @Autowired
    public VolunteerController(VolunteerService volunteerService,
                               DisasterService disasterService,
                               UserService userService) {
        this.volunteerService = volunteerService;
        this.disasterService = disasterService;
        this.userService = userService;
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return userService.findByUsername(auth.getName()).map(User::getId).orElse(null);
        }
        return null;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String listVolunteers(@RequestParam(value = "approval", required = false) String approvalFilter,
                                 @RequestParam(value = "availableOnly", defaultValue = "false") boolean availableOnly,
                                 Model model) {
        model.addAttribute("activeMenu", "volunteers");

        List<Volunteer> volunteers;
        if (availableOnly) {
            volunteers = volunteerService.getAvailableVolunteers();
        } else if (approvalFilter != null && !approvalFilter.isBlank() && !approvalFilter.equalsIgnoreCase("ALL")) {
            try {
                VolunteerApprovalStatus status = VolunteerApprovalStatus.valueOf(approvalFilter.toUpperCase());
                volunteers = volunteerService.getVolunteersByApproval(status);
            } catch (IllegalArgumentException e) {
                volunteers = volunteerService.getAllVolunteers();
            }
        } else {
            volunteers = volunteerService.getAllVolunteers();
        }

        model.addAttribute("volunteers", volunteers);
        model.addAttribute("selectedApproval", approvalFilter != null ? approvalFilter : "ALL");
        model.addAttribute("availableOnly", availableOnly);
        model.addAttribute("approvalStatuses", VolunteerApprovalStatus.values());

        model.addAttribute("totalCount", volunteerService.getTotalVolunteerCount());
        model.addAttribute("pendingCount", volunteerService.getPendingApprovalCount());
        model.addAttribute("availableCount", volunteerService.getAvailableCount());
        model.addAttribute("totalAssignments", volunteerService.getTotalAssignmentsCount());

        return "volunteers/list";
    }

    @GetMapping("/register")
    public String showRegisterForm(Authentication auth, Model model) {
        model.addAttribute("activeMenu", "vol-register");
        if (!model.containsAttribute("volunteerDto")) {
            VolunteerRegistrationDto dto = new VolunteerRegistrationDto();
            if (auth != null && auth.isAuthenticated()) {
                userService.findByUsername(auth.getName()).ifPresent(u -> {
                    dto.setFullName(u.getFullName());
                    dto.setEmail(u.getEmail());
                    dto.setPhone(u.getPhone());
                });
            }
            model.addAttribute("volunteerDto", dto);
        }
        return "volunteers/register";
    }

    @PostMapping("/register")
    public String submitRegistration(@Valid @ModelAttribute("volunteerDto") VolunteerRegistrationDto dto,
                                     BindingResult bindingResult,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes,
                                     Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "vol-register");
            return "volunteers/register";
        }

        try {
            Long userId = getCurrentUserId(auth);
            Volunteer registered = volunteerService.registerVolunteer(dto, userId);
            return "redirect:/volunteers/register?submitted=true&ref=" + registered.getVolunteerIdentifier();
        } catch (Exception e) {
            model.addAttribute("activeMenu", "vol-register");
            model.addAttribute("errorMessage", "Registration failed: " + e.getMessage());
            return "volunteers/register";
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String viewVolunteerProfile(@PathVariable Long id, Model model) {
        model.addAttribute("activeMenu", "volunteers");

        Volunteer volunteer = volunteerService.getVolunteerById(id)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer not found with id: " + id));

        model.addAttribute("volunteer", volunteer);
        model.addAttribute("assignments", volunteerService.getAssignmentsByVolunteer(id));
        model.addAttribute("availabilityStatuses", VolunteerAvailability.values());

        return "volunteers/details";
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveVolunteer(@PathVariable Long id,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            volunteerService.approveVolunteer(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Volunteer has been approved and vetted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Approval failed: " + e.getMessage());
        }
        return "redirect:/volunteers/" + id;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public String rejectVolunteer(@PathVariable Long id,
                                  Authentication auth,
                                  RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            volunteerService.rejectVolunteer(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Volunteer application marked as rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Rejection failed: " + e.getMessage());
        }
        return "redirect:/volunteers/" + id;
    }

    @PostMapping("/{id}/availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String updateAvailability(@PathVariable Long id,
                                     @RequestParam("availabilityStatus") VolunteerAvailability status,
                                     Authentication auth,
                                     RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            volunteerService.updateAvailability(id, status, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Availability status updated to " + status.getDisplayName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update availability: " + e.getMessage());
        }
        return "redirect:/volunteers/" + id;
    }

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String listAssignments(@RequestParam(value = "disasterId", required = false) Long disasterId,
                                  @RequestParam(value = "status", required = false) String statusFilter,
                                  Model model) {
        model.addAttribute("activeMenu", "volunteer-assignments");

        List<VolunteerAssignment> assignments;
        if (disasterId != null) {
            assignments = volunteerService.getAssignmentsByDisaster(disasterId);
        } else {
            assignments = volunteerService.getAllAssignments();
        }

        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
            try {
                VolunteerAssignmentStatus status = VolunteerAssignmentStatus.valueOf(statusFilter.toUpperCase());
                assignments = assignments.stream().filter(a -> a.getStatus() == status).toList();
            } catch (IllegalArgumentException ignored) {}
        }

        model.addAttribute("assignments", assignments);
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("selectedDisasterId", disasterId);
        model.addAttribute("selectedStatus", statusFilter != null ? statusFilter : "ALL");
        model.addAttribute("statuses", VolunteerAssignmentStatus.values());

        return "volunteers/assignments-list";
    }

    @GetMapping("/assignments/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String showAssignTaskForm(@RequestParam(value = "volunteerId", required = false) Long volunteerId,
                                     Model model) {
        model.addAttribute("activeMenu", "volunteer-assignments");

        if (!model.containsAttribute("assignmentDto")) {
            VolunteerAssignmentDto dto = new VolunteerAssignmentDto();
            if (volunteerId != null) {
                dto.setVolunteerId(volunteerId);
            }
            model.addAttribute("assignmentDto", dto);
        }

        model.addAttribute("availableVolunteers", volunteerService.getAvailableVolunteers());
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("priorities", TaskPriority.values());

        return "volunteers/assign-task";
    }

    @PostMapping("/assignments/new")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String assignTask(@Valid @ModelAttribute("assignmentDto") VolunteerAssignmentDto dto,
                             BindingResult bindingResult,
                             Authentication auth,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "volunteer-assignments");
            model.addAttribute("availableVolunteers", volunteerService.getAvailableVolunteers());
            model.addAttribute("disasters", disasterService.getActiveDisasters());
            model.addAttribute("priorities", TaskPriority.values());
            return "volunteers/assign-task";
        }

        try {
            Long userId = getCurrentUserId(auth);
            VolunteerAssignment created = volunteerService.assignTask(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Task '" + created.getTaskTitle() + "' successfully offered to volunteer.");
            return "redirect:/volunteers/assignments";
        } catch (Exception e) {
            model.addAttribute("activeMenu", "volunteer-assignments");
            model.addAttribute("errorMessage", "Failed to assign task: " + e.getMessage());
            model.addAttribute("availableVolunteers", volunteerService.getAvailableVolunteers());
            model.addAttribute("disasters", disasterService.getActiveDisasters());
            model.addAttribute("priorities", TaskPriority.values());
            return "volunteers/assign-task";
        }
    }

    @GetMapping("/my-assignments")
    @PreAuthorize("isAuthenticated()")
    public String myAssignments(Authentication auth, Model model) {
        model.addAttribute("activeMenu", "my-assignments");

        Long userId = getCurrentUserId(auth);
        List<VolunteerAssignment> myAssignments = Collections.emptyList();
        Volunteer myProfile = null;

        if (userId != null) {
            Optional<Volunteer> volOpt = volunteerService.getVolunteerByUserId(userId);
            if (volOpt.isPresent()) {
                myProfile = volOpt.get();
                myAssignments = volunteerService.getAssignmentsByVolunteer(myProfile.getId());
            }
        }

        model.addAttribute("volunteer", myProfile);
        model.addAttribute("assignments", myAssignments);
        return "volunteers/my-assignments";
    }

    @PostMapping("/assignments/{id}/accept")
    @PreAuthorize("isAuthenticated()")
    public String acceptTask(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            volunteerService.acceptTask(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment accepted! Thank you for your service.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to accept task: " + e.getMessage());
        }
        return "redirect:/volunteers/my-assignments";
    }

    @PostMapping("/assignments/{id}/decline")
    @PreAuthorize("isAuthenticated()")
    public String declineTask(@PathVariable Long id,
                              @RequestParam(value = "reason", required = false) String reason,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            volunteerService.declineTask(id, reason, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment declined. You have been released to the available pool.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to decline task: " + e.getMessage());
        }
        return "redirect:/volunteers/my-assignments";
    }

    @PostMapping("/assignments/{id}/start")
    @PreAuthorize("isAuthenticated()")
    public String startTask(@PathVariable Long id,
                            Authentication auth,
                            RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            volunteerService.startTask(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Task status changed to IN_PROGRESS.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to start task: " + e.getMessage());
        }
        return "redirect:/volunteers/my-assignments";
    }

    @PostMapping("/assignments/{id}/progress")
    @PreAuthorize("isAuthenticated()")
    public String updateProgress(@PathVariable Long id,
                                 @RequestParam("progressNotes") String notes,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            volunteerService.updateProgressNotes(id, notes, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Progress notes logged.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update notes: " + e.getMessage());
        }
        return "redirect:/volunteers/my-assignments";
    }

    @PostMapping("/assignments/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public String completeTask(@PathVariable Long id,
                               @RequestParam(value = "notes", required = false) String notes,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            volunteerService.completeTask(id, notes, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Task completed successfully! You are now available for future assignments.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to complete task: " + e.getMessage());
        }
        return "redirect:/volunteers/my-assignments";
    }

    @PostMapping("/assignments/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER', 'RESCUE_OFFICER')")
    public String cancelTask(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            volunteerService.cancelTask(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Assignment cancelled.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to cancel assignment: " + e.getMessage());
        }
        return "redirect:/volunteers/assignments";
    }
}
