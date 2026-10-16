package com.resqhub.controller;

import com.resqhub.dto.DonationFormDto;
import com.resqhub.model.*;
import com.resqhub.service.DonationService;
import com.resqhub.service.ResourceService;
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

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/donations")
public class DonationController {

    private final DonationService donationService;
    private final ResourceService resourceService;
    private final UserService userService;

    @Autowired
    public DonationController(DonationService donationService,
                              ResourceService resourceService,
                              UserService userService) {
        this.donationService = donationService;
        this.resourceService = resourceService;
        this.userService = userService;
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            return userService.findByUsername(auth.getName()).map(User::getId).orElse(null);
        }
        return null;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listDonations(@RequestParam(value = "status", required = false) String statusFilter,
                                @RequestParam(value = "type", required = false) String typeFilter,
                                Model model) {
        model.addAttribute("activeMenu", "donations");

        List<Donation> donations = donationService.getAllDonations();

        if (statusFilter != null && !statusFilter.isBlank() && !statusFilter.equalsIgnoreCase("ALL")) {
            try {
                DonationStatus status = DonationStatus.valueOf(statusFilter.toUpperCase());
                donations = donations.stream().filter(d -> d.getStatus() == status).toList();
            } catch (IllegalArgumentException ignored) {}
        }

        if (typeFilter != null && !typeFilter.isBlank() && !typeFilter.equalsIgnoreCase("ALL")) {
            try {
                DonationType type = DonationType.valueOf(typeFilter.toUpperCase());
                donations = donations.stream().filter(d -> d.getDonationType() == type).toList();
            } catch (IllegalArgumentException ignored) {}
        }

        model.addAttribute("donations", donations);
        model.addAttribute("selectedStatus", statusFilter != null ? statusFilter : "ALL");
        model.addAttribute("selectedType", typeFilter != null ? typeFilter : "ALL");
        model.addAttribute("statuses", DonationStatus.values());
        model.addAttribute("types", DonationType.values());

        model.addAttribute("totalCount", donationService.getTotalDonationCount());
        model.addAttribute("totalFunds", donationService.getTotalMonetaryFunds());
        model.addAttribute("totalMaterialUnits", donationService.getTotalMaterialUnits());

        return "donations/list";
    }

    @GetMapping("/new")
    public String showDonationForm(Authentication auth, Model model) {
        model.addAttribute("activeMenu", "donations");

        if (!model.containsAttribute("donationDto")) {
            DonationFormDto dto = new DonationFormDto();
            if (auth != null && auth.isAuthenticated()) {
                userService.findByUsername(auth.getName()).ifPresent(u -> {
                    dto.setDonorName(u.getFullName());
                    dto.setDonorEmail(u.getEmail());
                    dto.setDonorPhone(u.getPhone());
                });
            }
            model.addAttribute("donationDto", dto);
        }

        model.addAttribute("inventoryItems", resourceService.getAllItems());
        model.addAttribute("types", DonationType.values());

        return "donations/form";
    }

    @PostMapping("/new")
    public String submitDonation(@Valid @ModelAttribute("donationDto") DonationFormDto dto,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeMenu", "donations");
            model.addAttribute("inventoryItems", resourceService.getAllItems());
            model.addAttribute("types", DonationType.values());
            return "donations/form";
        }

        try {
            Long userId = getCurrentUserId(auth);
            Donation donation = donationService.submitDonation(dto, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Thank you for your generosity! Your donation reference is " + donation.getDonationReference() + ".");
            return "redirect:/donations/receipt/" + donation.getDonationReference();
        } catch (Exception e) {
            model.addAttribute("activeMenu", "donations");
            model.addAttribute("errorMessage", "Donation submission failed: " + e.getMessage());
            model.addAttribute("inventoryItems", resourceService.getAllItems());
            model.addAttribute("types", DonationType.values());
            return "donations/form";
        }
    }

    @GetMapping("/receipt/{reference}")
    public String viewReceipt(@PathVariable String reference, Model model) {
        model.addAttribute("activeMenu", "donations");

        Donation donation = donationService.getDonationByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Donation record not found with reference: " + reference));

        model.addAttribute("donation", donation);
        return "donations/receipt";
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public String verifyDonation(@PathVariable Long id,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            donationService.verifyDonation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Donation successfully marked as VERIFIED.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Verification failed: " + e.getMessage());
        }
        return "redirect:/donations";
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'CAMP_MANAGER')")
    public String receiveMaterialDonation(@PathVariable Long id,
                                          Authentication auth,
                                          RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            donationService.receiveMaterialDonation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Relief supplies received and stock deposited into warehouse inventory!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Receiving supplies failed: " + e.getMessage());
        }
        return "redirect:/donations";
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public String cancelDonation(@PathVariable Long id,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long userId = getCurrentUserId(auth);
            donationService.cancelDonation(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Donation marked as CANCELLED.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Cancellation failed: " + e.getMessage());
        }
        return "redirect:/donations";
    }

    @GetMapping("/my-donations")
    @PreAuthorize("isAuthenticated()")
    public String myDonations(Authentication auth, Model model) {
        model.addAttribute("activeMenu", "my-donations");

        List<Donation> userDonations = Collections.emptyList();
        if (auth != null && auth.isAuthenticated()) {
            userDonations = userService.findByUsername(auth.getName())
                    .map(u -> donationService.getDonationsByEmail(u.getEmail()))
                    .orElse(Collections.emptyList());
        }

        model.addAttribute("donations", userDonations);
        return "donations/my-donations";
    }
}
