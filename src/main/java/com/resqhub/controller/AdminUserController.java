package com.resqhub.controller;

import com.resqhub.dto.UserEditDto;
import com.resqhub.dto.UserRegistrationDto;
import com.resqhub.model.Role;
import com.resqhub.model.User;
import com.resqhub.service.ActivityLogService;
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
import java.util.Map;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final ActivityLogService activityLogService;

    @Autowired
    public AdminUserController(UserService userService, ActivityLogService activityLogService) {
        this.userService = userService;
        this.activityLogService = activityLogService;
    }

    private Long getCurrentUserId(Authentication auth) {
        if (auth != null) {
            return userService.findByUsername(auth.getName()).map(User::getId).orElse(1L);
        }
        return 1L;
    }

    @GetMapping("/users")
    public String listUsers(@RequestParam(value = "page", defaultValue = "0") int page,
                            @RequestParam(value = "size", defaultValue = "10") int size,
                            @RequestParam(value = "search", required = false) String search,
                            @RequestParam(value = "role", defaultValue = "ALL") String role,
                            Model model) {
        model.addAttribute("activeMenu", "users");
        
        List<User> users = userService.getUsers(page, size, search, role);
        int totalUsers = userService.countUsers(search, role);
        int totalPages = (int) Math.ceil((double) totalUsers / size);

        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("search", search);
        model.addAttribute("selectedRole", role);
        model.addAttribute("roles", Role.values());

        return "admin/users/list";
    }

    @GetMapping("/users/create")
    public String createUserForm(Model model) {
        model.addAttribute("activeMenu", "users");
        UserEditDto dto = new UserEditDto();
        dto.setStatus("ACTIVE");
        dto.setRole(Role.RESCUE_OFFICER);
        model.addAttribute("userDto", dto);
        model.addAttribute("roles", Role.values());
        model.addAttribute("isCreate", true);
        return "admin/users/form";
    }

    @PostMapping("/users/create")
    public String createUser(@Valid @ModelAttribute("userDto") UserEditDto dto,
                             BindingResult result,
                             Authentication auth,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (dto.getNewPassword() == null || dto.getNewPassword().length() < 6) {
            result.rejectValue("newPassword", "error.userDto", "Password is required and must be at least 6 characters.");
        }
        if (userService.existsByUsername(dto.getUsername())) {
            result.rejectValue("username", "error.userDto", "Username already exists.");
        }
        if (userService.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "error.userDto", "Email address already registered.");
        }

        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "users");
            model.addAttribute("roles", Role.values());
            model.addAttribute("isCreate", true);
            return "admin/users/form";
        }

        UserRegistrationDto regDto = new UserRegistrationDto();
        regDto.setUsername(dto.getUsername());
        regDto.setEmail(dto.getEmail());
        regDto.setFullName(dto.getFullName());
        regDto.setPhone(dto.getPhone());
        regDto.setPassword(dto.getNewPassword());
        regDto.setConfirmPassword(dto.getNewPassword());
        regDto.setRequestedRole(dto.getRole());

        // Save with selected admin-assigned role
        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setFullName(dto.getFullName().trim());
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        user.setRole(dto.getRole());
        user.setStatus(dto.getStatus());
        user.setPasswordHash(dto.getNewPassword()); // Will be updated in service
        
        dto.setId(0L);
        userService.registerUser(regDto);
        // If non-citizen role was selected by admin, apply it directly:
        userService.findByUsername(dto.getUsername()).ifPresent(u -> {
            dto.setId(u.getId());
            userService.updateUser(dto, getCurrentUserId(auth));
        });

        redirectAttributes.addFlashAttribute("successMessage", "User account '" + dto.getUsername() + "' created successfully.");
        return "redirect:/admin/users";
    }

    @GetMapping("/users/edit/{id}")
    public String editUserForm(@PathVariable("id") Long id, Model model) {
        model.addAttribute("activeMenu", "users");
        User user = userService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + id));

        UserEditDto dto = new UserEditDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());

        model.addAttribute("userDto", dto);
        model.addAttribute("roles", Role.values());
        model.addAttribute("isCreate", false);
        return "admin/users/form";
    }

    @PostMapping("/users/edit")
    public String updateUser(@Valid @ModelAttribute("userDto") UserEditDto dto,
                             BindingResult result,
                             Authentication auth,
                             RedirectAttributes redirectAttributes,
                             Model model) {
        if (result.hasErrors()) {
            model.addAttribute("activeMenu", "users");
            model.addAttribute("roles", Role.values());
            model.addAttribute("isCreate", false);
            return "admin/users/form";
        }

        try {
            userService.updateUser(dto, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "User '" + dto.getUsername() + "' updated successfully.");
            return "redirect:/admin/users";
        } catch (Exception ex) {
            model.addAttribute("activeMenu", "users");
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("roles", Role.values());
            model.addAttribute("isCreate", false);
            return "admin/users/form";
        }
    }

    @PostMapping("/users/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") Long id,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            userService.toggleUserStatus(id, getCurrentUserId(auth));
            redirectAttributes.addFlashAttribute("successMessage", "User status updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/activity-logs")
    public String viewActivityLogs(@RequestParam(value = "page", defaultValue = "0") int page,
                                   @RequestParam(value = "size", defaultValue = "15") int size,
                                   @RequestParam(value = "module", defaultValue = "ALL") String module,
                                   @RequestParam(value = "search", required = false) String search,
                                   Model model) {
        model.addAttribute("activeMenu", "activity-logs");
        
        List<Map<String, Object>> logs = activityLogService.getAllLogs(page, size, module, search);
        int totalLogs = activityLogService.countLogs(module, search);
        int totalPages = (int) Math.ceil((double) totalLogs / size);

        model.addAttribute("logs", logs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalLogs", totalLogs);
        model.addAttribute("selectedModule", module);
        model.addAttribute("search", search);

        return "admin/logs";
    }
}
