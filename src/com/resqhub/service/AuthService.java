package com.resqhub.service;

import com.resqhub.dao.UserDAO;
import com.resqhub.exception.AuthenticationException;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidUserDataException;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.User;
import com.resqhub.util.PasswordUtil;
import com.resqhub.util.ValidationUtil;

/**
 * Authentication workflow: credential checking, account lockout after
 * repeated failures, and session lifecycle.
 */
public class AuthService {

    public static final int MAX_FAILED_ATTEMPTS = 3;

    private final UserDAO userDAO = new UserDAO();
    private final SessionManager session = SessionManager.getInstance();

    /**
     * Verifies credentials and opens a session on success.
     * Failure messages never reveal whether the username or the
     * password was wrong (basic security hygiene).
     */
    public User login(String username, String password)
            throws AuthenticationException, DataAccessException {

        if (!ValidationUtil.requireNonBlank(username, password)) {
            throw new AuthenticationException("Username and password are required");
        }

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            throw new AuthenticationException("Invalid username or password");
        }

        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            throw new AuthenticationException(
                    "Account is locked. Ask an administrator to unlock it.");
        }
        if (user.getAccountStatus() == AccountStatus.INACTIVE) {
            throw new AuthenticationException(
                    "Account is inactive. Contact an administrator.");
        }

        if (!PasswordUtil.matches(password, user.getPasswordHash())) {
            int attempts = userDAO.recordFailedLogin(user.getId());
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setAccountStatus(AccountStatus.LOCKED);
                user.setFailedLoginAttempts(attempts);
                userDAO.save(user);
                throw new AuthenticationException(
                        "Account locked after " + attempts + " failed attempts");
            }
            throw new AuthenticationException("Invalid username or password ("
                    + (MAX_FAILED_ATTEMPTS - attempts) + " attempt(s) left)");
        }

        userDAO.recordSuccessfulLogin(user.getId());
        session.login(user);
        return user;
    }

    public void logout() {
        session.logout();
    }

    /** Changes the logged-in user's own password after re-checking the old one. */
    public void changeOwnPassword(String oldPassword, String newPassword)
            throws AuthenticationException, InvalidUserDataException,
            DataAccessException {

        if (!session.isLoggedIn()) {
            throw new AuthenticationException("No active session");
        }
        User current = session.getCurrentUser();
        if (!PasswordUtil.matches(oldPassword, current.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }
        UserService.validatePasswordPolicy(newPassword);

        current.setPasswordHash(PasswordUtil.hash(newPassword));
        userDAO.save(current);
    }
}
