package com.resqhub.controller;

import com.resqhub.dto.UserRegistrationDto;
import com.resqhub.model.Role;
import com.resqhub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password, or account is disabled.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out securely.");
        }

        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }

        if (!model.containsAttribute("userDto")) {
            UserRegistrationDto dto = new UserRegistrationDto();
            dto.setRequestedRole(Role.CITIZEN);
            model.addAttribute("userDto", dto);
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("userDto") UserRegistrationDto userDto,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {
        if (!userDto.getPassword().equals(userDto.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "error.userDto", "Passwords do not match.");
        }
        if (userService.existsByUsername(userDto.getUsername())) {
            bindingResult.rejectValue("username", "error.userDto", "Username is already taken.");
        }
        if (userService.existsByEmail(userDto.getEmail())) {
            bindingResult.rejectValue("email", "error.userDto", "Email address is already in use.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(userDto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Registration successful! You can now log in with your credentials.");
            return "redirect:/login";
        } catch (Exception ex) {
            bindingResult.reject("registrationError", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/dashboard-redirect")
    public String dashboardRedirect(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return "redirect:/dashboard";
            } else if ("ROLE_RESCUE_OFFICER".equals(role)) {
                return "redirect:/dashboard";
            } else if ("ROLE_CAMP_MANAGER".equals(role)) {
                return "redirect:/dashboard";
            } else if ("ROLE_VOLUNTEER".equals(role)) {
                return "redirect:/dashboard";
            } else if ("ROLE_CITIZEN".equals(role)) {
                return "redirect:/dashboard";
            }
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model) {
        model.addAttribute("errorMessage", "Access Denied: You do not have permission to view this restricted page.");
        return "errors/403";
    }
}
