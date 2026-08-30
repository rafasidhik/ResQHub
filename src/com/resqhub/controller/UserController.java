package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.service.UserService;

/** Admin-only user administration screen controller. */
public class UserController {

    private final UserService userService = new UserService();

    public ActionResult registerUser(String username, String password,
                                     String fullName, String email,
                                     String phone, RoleType role) {
        try {
            User user = userService.registerUser(username, password,
                    fullName, email, phone, role);
            return ActionResult.successWithData(
                    "Account created: " + user.getUsername()
                            + " (" + user.getRole().getLabel() + ")",
                    user);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error creating account: "
                    + e.getMessage());
        }
    }

    public ActionResult unlockUser(long userId) {
        try {
            userService.unlockAccount(userId);
            return ActionResult.success("Account #" + userId + " unlocked");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error unlocking: "
                    + e.getMessage());
        }
    }

    public ActionResult setStatus(long userId, AccountStatus status) {
        try {
            userService.setUserStatus(userId, status);
            return ActionResult.success("Account #" + userId
                    + " set to " + status.getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error updating status: "
                    + e.getMessage());
        }
    }

    public ActionResult updateUser(long userId, String fullName,
                                   String email, String phone,
                                   RoleType role) {
        try {
            User updated = userService.updateUser(userId, fullName, email,
                    phone, role);
            return ActionResult.success("Account #" + updated.getId()
                    + " updated");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error updating account: "
                    + e.getMessage());
        }
    }

    public ActionResult resetPassword(long userId, String newPassword) {
        try {
            userService.resetPassword(userId, newPassword);
            return ActionResult.success("Password reset for user #"
                    + userId);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error resetting password: "
                    + e.getMessage());
        }
    }

    public ActionResult deleteUser(long userId) {
        try {
            userService.deleteUser(userId);
            return ActionResult.success("User #" + userId + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error deleting user: "
                    + e.getMessage());
        }
    }

    /** Read method: authorization failures surface as DataAccessException. */
    public List<User> listUsers() throws DataAccessException {
        try {
            return userService.listUsers();
        } catch (UnauthorizedOperationException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    public static Object[] toRow(User u) {
        return new Object[] {
                u.getId(),
                u.getUsername(),
                u.getFullName(),
                u.getEmail(),
                u.getPhone() == null ? "-" : u.getPhone(),
                u.getRole().getLabel(),
                u.getAccountStatus().getLabel()
        };
    }

    public static String[] tableHeaders() {
        return new String[] {"ID", "Username", "Full Name", "Email",
                "Phone", "Role", "Status"};
    }
}
