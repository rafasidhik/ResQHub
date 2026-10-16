package com.resqhub.controller;

import com.resqhub.dto.RescueRequestDto;
import com.resqhub.dto.TeamRecommendation;
import com.resqhub.model.*;
import com.resqhub.service.DisasterService;
import com.resqhub.service.RescueService;
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
@RequestMapping("/rescue-requests")
public class RescueRequestController {

    private final RescueService rescueService;
    private final DisasterService disasterService;
    private final VictimService victimService;
    private final UserService userService;

    @Autowired
    public RescueRequestController(RescueService rescueService,
                                   DisasterService disasterService,
                                   VictimService victimService,
                                   UserService userService) {
        this.rescueService = rescueService;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String listRequests(@RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "10") int size,
                               @RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "disasterId", required = false) Long disasterId,
                               @RequestParam(value = "status", defaultValue = "ALL") String status,
                               @RequestParam(value = "priority", defaultValue = "ALL") String priority,
                               Model model) {
        model.addAttribute("activeMenu", "rescue-requests");

        List<RescueRequest> requests = rescueService.getRequests(page, size, search, disasterId, status, priority);
        int totalRequests = rescueService.countRequests(search, disasterId, status, priority);
        int totalPages = (int) Math.ceil((double) totalRequests / size);

        model.addAttribute("requests", requests);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalRequests", totalRequests);
        model.addAttribute("search", search);
        model.addAttribute("selectedDisasterId", disasterId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);

        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("statuses", RescueRequestStatus.values());
        model.addAttribute("priorities", VictimPriority.values());

        return "rescue-requests/list";
    }

    @GetMapping("/my-requests")
    public String myRequests(@RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "size", defaultValue = "10") int size,
                             Authentication auth,
                             Model model) {
        model.addAttribute("activeMenu", "my-requests");
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            return "redirect:/login";
        }

        List<RescueRequest> requests = rescueService.getRequestsByCitizen(userId, page, size);
        int total = rescueService.countRequestsByCitizen(userId);
        int totalPages = (int) Math.ceil((double) total / size);

        model.addAttribute("requests", requests);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalRequests", total);

        return "rescue-requests/my-requests";
    }

    @GetMapping("/new")
    public String newRequestForm(@RequestParam(value = "disasterId", required = false) Long disasterId,
                                 @RequestParam(value = "victimId", required = false) Long victimId,
                                 Model model) {
        model.addAttribute("activeMenu", "new-request");
        RescueRequestDto dto = new RescueRequestDto();

        if (disasterId != null) {
            dto.setDisasterId(disasterId);
        }

        if (victimId != null) {
            victimService.getVictimById(victimId).ifPresent(v -> {
                dto.setVictimId(v.getId());
                dto.setDisasterId(v.getDisasterId());
                dto.setLocation(v.getLocation());
                dto.setContactNumber(v.getContactNumber() != null ? v.getContactNumber() : "");
                dto.setPeopleCount(v.getAffectedFamilyMembers());
                dto.setDescription("Rescue request logged for registered victim: " + v.getFullName() +
                        " (" + v.getReferenceNumber() + "). " + (v.getCurrentSituation() != null ? v.getCurrentSituation() : ""));
            });
        }

        model.addAttribute("requestDto", dto);
        model.addAttribute("disasters", disasterService.getActiveDisasters());
        model.addAttribute("urgencies", Urgency.values());

        return "rescue-requests/form";
    }

    @PostMapping("/new")
    public String submitRequest(@Valid @ModelAttribute("requestDto") RescueRequestDto dto,
                                BindingResult result,
                                Authentication auth,
                                RedirectAttributes redirectAttributes,
                                Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "new-request");
            model.addAttribute("disasters", disasterService.getActiveDisasters());
            model.addAttribute("urgencies", Urgency.values());
            return "rescue-requests/form";
        }

        Long userId = getCurrentUserId(auth);
        Long newId = rescueService.submitRescueRequest(dto, userId);

        redirectAttributes.addFlashAttribute("successMessage",
                "Your emergency rescue request has been logged into ResQHub. Dispatch coordinators have been alerted.");

        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CITIZEN"))) {
            return "redirect:/rescue-requests/my-requests";
        }
        if (userId == null) {
            return "redirect:/";
        }
        return "redirect:/rescue-requests/" + newId;
    }

    @GetMapping("/{id}")
    public String viewRequestDetails(@PathVariable("id") Long id,
                                     Authentication auth,
                                     Model model) {
        model.addAttribute("activeMenu", "rescue-requests");
        RescueRequest req = rescueService.getRequestById(id)
                .orElseThrow(() -> new IllegalArgumentException("Rescue request not found with ID: " + id));

        // Ownership check for citizens
        if (auth != null && auth.getAuthorities().stream().allMatch(a -> a.getAuthority().equals("ROLE_CITIZEN"))) {
            Long userId = getCurrentUserId(auth);
            if (req.getReportedByUserId() != null && !req.getReportedByUserId().equals(userId)) {
                return "redirect:/access-denied";
            }
        }

        model.addAttribute("request", req);
        model.addAttribute("assignments", rescueService.getRequestHistory(id));
        return "rescue-requests/details";
    }

    @GetMapping("/smart-allocation")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String smartAllocationDashboard(@RequestParam(value = "requestId", required = false) Long requestId,
                                           Model model) {
        model.addAttribute("activeMenu", "smart-rescue");

        // Pending and verified requests awaiting team dispatch
        List<RescueRequest> pendingRequests = rescueService.getRequests(0, 50, null, null, "PENDING", null);
        List<RescueRequest> verifiedRequests = rescueService.getRequests(0, 50, null, null, "VERIFIED", null);
        pendingRequests.addAll(verifiedRequests);

        model.addAttribute("pendingRequests", pendingRequests);

        if (requestId != null) {
            rescueService.getRequestById(requestId).ifPresent(req -> {
                model.addAttribute("selectedRequest", req);
                List<TeamRecommendation> recommendations = rescueService.getRecommendedTeams(requestId);
                model.addAttribute("recommendations", recommendations);
            });
        } else if (!pendingRequests.isEmpty()) {
            RescueRequest firstReq = pendingRequests.get(0);
            model.addAttribute("selectedRequest", firstReq);
            List<TeamRecommendation> recommendations = rescueService.getRecommendedTeams(firstReq.getId());
            model.addAttribute("recommendations", recommendations);
        }

        return "rescue-requests/smart-assign";
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String assignTeam(@PathVariable("id") Long id,
                             @RequestParam("teamId") Long teamId,
                             @RequestParam(value = "notes", required = false) String notes,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            rescueService.assignTeam(id, teamId, notes, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Rescue team successfully deployed to mission coordinates.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/rescue-requests/" + id;
    }

    @PostMapping("/assignment/{id}/progress")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String updateProgress(@PathVariable("id") Long assignmentId,
                                 @RequestParam("requestId") Long requestId,
                                 @RequestParam("status") RescueAssignmentStatus status,
                                 @RequestParam(value = "notes", required = false) String notes,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            rescueService.updateAssignmentProgress(assignmentId, status, notes, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "Operation status updated to " + status.getDisplayName());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/rescue-requests/" + requestId;
    }

    @PostMapping("/assignment/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESCUE_OFFICER')")
    public String completeOperation(@PathVariable("id") Long assignmentId,
                                    @RequestParam("requestId") Long requestId,
                                    @RequestParam(value = "notes", required = false) String notes,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            rescueService.completeRescueOperation(assignmentId, notes, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage",
                    "Rescue operation successfully completed! Team has been released and marked AVAILABLE.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/rescue-requests/" + requestId;
    }
}
