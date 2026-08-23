package com.resqhub.service;

import com.resqhub.dao.UserDAO;
import com.resqhub.exception.AuthenticationException;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidUserDataException;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.util.PasswordUtil;
import com.resqhub.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Public citizen self-registration: no session or role required.
     * Self-service signups are ALWAYS RoleType.CITIZEN - staff roles
     * can only be granted by an administrator via UserService.
     */
    public User selfRegister(String username, String rawPassword,
                             String fullName, String email, String phone)
            throws InvalidUserDataException, DataAccessException {

        List<String> errors = new ArrayList<>();
        if (username == null || !username.matches("[A-Za-z0-9_]{4,50}")) {
            errors.add("username must be 4-50 characters (letters, digits, underscore)");
        }
        try {
            UserService.validatePasswordPolicy(rawPassword);
        } catch (InvalidUserDataException e) {
            errors.add(e.getMessage());
        }
        if (!ValidationUtil.isValidName(fullName)) {
            errors.add("full name is invalid");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            errors.add("email is invalid");
        }
        String cleanPhone = ValidationUtil.clean(phone);
        if (cleanPhone != null && !cleanPhone.isEmpty()
                && !ValidationUtil.isValidPhone(cleanPhone)) {
            errors.add("phone must be 10 digits");
        }
        if (!errors.isEmpty()) {
            throw new InvalidUserDataException(String.join("; ", errors));
        }

        if (userDAO.findByUsername(username.trim()) != null) {
            throw new InvalidUserDataException("username already exists");
        }
        for (User existing : userDAO.findAll()) {
            if (existing.getEmail().equalsIgnoreCase(email.trim())) {
                throw new InvalidUserDataException("email already registered");
            }
        }

        User user = new User(ValidationUtil.clean(fullName),
                cleanPhone == null || cleanPhone.isEmpty() ? null : cleanPhone,
                ValidationUtil.clean(email));
        user.setUsername(username.trim());
        user.setPasswordHash(PasswordUtil.hash(rawPassword));
        user.setRole(RoleType.CITIZEN);
        user.setAccountStatus(AccountStatus.ACTIVE);

        return userDAO.save(user);
    }
}
