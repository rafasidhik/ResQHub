package com.resqhub.service;

import java.util.ArrayList;
import java.util.List;

import com.resqhub.dao.UserDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidUserDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.util.PasswordUtil;
import com.resqhub.util.ValidationUtil;

/**
 * User account administration (ADMIN-only operations).
 */
public class UserService {

    private final UserDAO userDAO = new UserDAO();
    private final SessionManager session = SessionManager.getInstance();

    public User registerUser(String username, String rawPassword, String fullName,
                             String email, String phone, RoleType role)
            throws UnauthorizedOperationException, InvalidUserDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN);

        List<String> errors = new ArrayList<>();
        if (username == null || !username.matches("[A-Za-z0-9_]{4,50}")) {
            errors.add("username must be 4-50 characters (letters, digits, underscore)");
        }
        validatePasswordPolicy(rawPassword);
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
        if (role == null) {
            errors.add("a role must be selected");
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
        user.setRole(role);
        user.setAccountStatus(AccountStatus.ACTIVE);

        return userDAO.save(user);
    }

    /** Shared password rule: at least 8 chars with a letter and a digit. */
    static void validatePasswordPolicy(String password)
            throws InvalidUserDataException {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Za-z].*")
                || !password.matches(".*[0-9].*")) {
            throw new InvalidUserDataException(
                    "password must be at least 8 characters and contain letters and digits");
        }
    }

    public List<User> listUsers()
            throws UnauthorizedOperationException, DataAccessException {
        session.requireRole(RoleType.ADMIN);
        return userDAO.findAll();
    }

    public void setUserStatus(long userId, AccountStatus status)
            throws UnauthorizedOperationException, InvalidUserDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN);
        User user = requireExisting(userId);
        user.setAccountStatus(status);
        userDAO.save(user);
    }

    /** Unlock + clear the failure counter in one administrative action. */
    public void unlockAccount(long userId)
            throws UnauthorizedOperationException, InvalidUserDataException,
            DataAccessException {

        setUserStatus(userId, AccountStatus.ACTIVE);
        User user = requireExisting(userId);
        user.setFailedLoginAttempts(0);
        userDAO.save(user);
    }

    /** ADMIN-only hard delete. Child references use ON DELETE SET NULL,
     *  so any account can be removed - except your own active one. */
    public void deleteUser(long userId)
            throws UnauthorizedOperationException, InvalidUserDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN);
        if (session.currentUserId() == userId) {
            throw new InvalidUserDataException(
                    "You cannot delete the account you are logged in with");
        }
        try {
            if (!userDAO.deleteById(userId)) {
                throw new InvalidUserDataException("No user with id " + userId);
            }
        } catch (DataAccessException e) {
            throw new InvalidUserDataException(
                    "Cannot delete user #" + userId
                            + " - records still reference this account");
        }
    }

    private User requireExisting(long userId) throws InvalidUserDataException,
            DataAccessException {
        User user = userDAO.findById(userId);
        if (user == null) {
            throw new InvalidUserDataException("No user with id " + userId);
        }
        return user;
    }
}
