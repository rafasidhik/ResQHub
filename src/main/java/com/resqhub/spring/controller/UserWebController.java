package com.resqhub.spring.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resqhub.controller.AccountDeletionRequestController;
import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;
import com.resqhub.controller.UserController;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.service.SessionManager;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class UserWebController extends BaseWebController {

    private final UserController users = new UserController();
    private final AccountDeletionRequestController deletions = new AccountDeletionRequestController();
    private final AuthController auth = new AuthController();

    @GetMapping("/app/users")
    public String listUsers(Model model) throws Exception {
        List<User> list = users.listUsers();
        Map<Long, String> names = new HashMap<>();
        list.forEach(u -> names.put(u.getId(), u.getUsername()));

        model.addAttribute("items", list);
        model.addAttribute("headers", UserController.tableHeaders());
        model.addAttribute("rows", list.stream().map(UserController::toRow).collect(Collectors.toList()));
        model.addAttribute("roles", WebUtil.options(RoleType.class));
        model.addAttribute("statuses", WebUtil.options(AccountStatus.class));
        model.addAttribute("pendingDeletions", deletions.getPendingRequests());
        model.addAttribute("userNames", names);
        model.addAttribute("allDeletions", deletions.getAllRequests());
        model.addAttribute("pageTitle", "User Management");
        return "users";
    }

    @PostMapping("/app/users")
    public String handleUserAction(@RequestParam("action") String action,
                                   HttpServletRequest req,
                                   RedirectAttributes ra) throws Exception {
        ActionResult r;
        switch (action) {
            case "create" -> {
                String pw = req.getParameter("password");
                if (pw == null || !pw.equals(req.getParameter("confirmPassword"))) {
                    ra.addFlashAttribute("flashType", "danger");
                    ra.addFlashAttribute("flashMessage", "Passwords do not match");
                    return "redirect:/app/users";
                }
                r = users.registerUser(
                        WebUtil.str(req, "username"),
                        pw,
                        WebUtil.str(req, "fullName"),
                        WebUtil.str(req, "email"),
                        WebUtil.opt(req, "phone"),
                        WebUtil.reqEnum(req, "role", RoleType.class, "Role")
                );
            }
            case "update" -> r = users.updateUser(
                    WebUtil.reqLong(req, "id", "User id"),
                    WebUtil.str(req, "fullName"),
                    WebUtil.str(req, "email"),
                    WebUtil.opt(req, "phone"),
                    WebUtil.reqEnum(req, "role", RoleType.class, "Role")
            );
            case "status" -> r = users.setStatus(
                    WebUtil.reqLong(req, "id", "User id"),
                    WebUtil.reqEnum(req, "status", AccountStatus.class, "Status")
            );
            case "unlock" -> r = users.unlockUser(WebUtil.reqLong(req, "id", "User id"));
            case "resetPassword" -> r = users.resetPassword(
                    WebUtil.reqLong(req, "id", "User id"),
                    req.getParameter("newPassword")
            );
            case "delete" -> r = users.deleteUser(WebUtil.reqLong(req, "id", "User id"));
            case "approveDeletion" -> r = deletions.approveRequest(
                    WebUtil.reqLong(req, "id", "Request id"),
                    WebUtil.opt(req, "notes")
            );
            case "denyDeletion" -> r = deletions.denyRequest(
                    WebUtil.reqLong(req, "id", "Request id"),
                    WebUtil.opt(req, "notes")
            );
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:/app/users";
            }
        }
        return handleResult(r, ra, "/app/users");
    }

    @GetMapping("/app/profile")
    public String profile(Model model) throws Exception {
        User u = SessionManager.getInstance().getCurrentUser();
        boolean canRequestDeletion = u != null && u.getRole() != RoleType.ADMIN;
        model.addAttribute("canRequestDeletion", canRequestDeletion);
        model.addAttribute("pageTitle", "My Profile");
        return "profile";
    }

    @PostMapping("/app/profile")
    public String handleProfileAction(@RequestParam("action") String action,
                                      HttpServletRequest req,
                                      RedirectAttributes ra) throws Exception {
        ActionResult r;
        switch (action) {
            case "profile" -> r = auth.updateOwnProfile(
                    WebUtil.str(req, "fullName"),
                    WebUtil.str(req, "email"),
                    WebUtil.opt(req, "phone")
            );
            case "password" -> {
                String next = req.getParameter("newPassword");
                if (next == null || !next.equals(req.getParameter("confirmPassword"))) {
                    ra.addFlashAttribute("flashType", "danger");
                    ra.addFlashAttribute("flashMessage", "New passwords do not match");
                    return "redirect:/app/profile";
                }
                r = auth.changeOwnPassword(req.getParameter("oldPassword"), next);
            }
            case "deletion" -> r = deletions.requestDeletion();
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:/app/profile";
            }
        }
        return handleResult(r, ra, "/app/profile");
    }
}
