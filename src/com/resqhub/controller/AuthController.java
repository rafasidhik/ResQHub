package com.resqhub.controller;

import com.resqhub.exception.ResQHubException;
import com.resqhub.model.User;
import com.resqhub.service.AuthService;
import com.resqhub.service.SessionManager;

/**
 * Authentication controller: the only screen-to-service bridge for
 * login, logout and password change. Converts every checked exception
 * into an ActionResult the login dialog can display directly.
 */
public class AuthController {

    private final AuthService authService = new AuthService();

    public ActionResult login(String username, String password) {
        try {
            User user = authService.login(username, password);
            return ActionResult.successWithData(
                    "Welcome " + user.getFullName()
                            + " (" + user.getRole().getLabel() + ")",
                    user);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error during login: "
                    + e.getMessage());
        }
    }

    public void logout() {
        authService.logout();
    }

    public ActionResult changeOwnPassword(String oldPassword, String newPassword) {
        try {
            authService.changeOwnPassword(oldPassword, newPassword);
            return ActionResult.success("Password changed successfully");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error changing password: "
                    + e.getMessage());
        }
    }

    public boolean isLoggedIn() {
        return SessionManager.getInstance().isLoggedIn();
    }

    /** Citizen self-signup: creates a CITIZEN account without admin help. */
    public ActionResult registerCitizen(String username, String password,
                                        String fullName, String email,
                                        String phone) {
        try {
            User user = authService.selfRegister(username, password,
                    fullName, email, phone);
            return ActionResult.successWithData(
                    "Account created for " + user.getUsername()
                            + ". You can now log in.",
                    user);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error during signup: "
                    + e.getMessage());
        }
    }

    /** Role of the logged-in user - drives dashboard navigation. */
    public boolean hasRole(com.resqhub.model.RoleType... roles) {
        return SessionManager.getInstance().hasRole(roles);
    }
}
