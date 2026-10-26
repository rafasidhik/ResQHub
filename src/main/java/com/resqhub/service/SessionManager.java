package com.resqhub.service;

import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.model.Volunteer;

/**
 * Holds the logged-in user for the CURRENT REQUEST THREAD.
 *
 * WEB MIGRATION NOTE: the desktop version kept one global "currentUser"
 * field, which is fine for a single-user Swing app but would leak one
 * user's identity to every other visitor on a web server. The public API
 * is unchanged, so every service/controller keeps working, but the state
 * now lives in a ThreadLocal. AuthFilter binds the user at the start of
 * each HTTP request and always clears it in a finally block, so a pooled
 * server thread can never carry a user over into the next request.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    private static final ThreadLocal<User> CURRENT = new ThreadLocal<>();

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    /** Binds the user to the current request thread. */
    public void login(User user) {
        CURRENT.set(user);
    }

    /** Unbinds the user; AuthFilter calls this after every request. */
    public void logout() {
        CURRENT.remove();
    }

    public boolean isLoggedIn() {
        return CURRENT.get() != null;
    }

    public User getCurrentUser() {
        return CURRENT.get();
    }

    /** VARARGS role check: hasRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER). */
    public boolean hasRole(RoleType... allowedRoles) {
        User user = CURRENT.get();
        if (user == null || allowedRoles == null) {
            return false;
        }
        for (RoleType allowed : allowedRoles) {
            if (allowed == user.getRole()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Authorization gate used by every service method. Throws instead of
     * returning boolean so callers cannot forget to check the result.
     */
    public void requireRole(RoleType... allowedRoles)
            throws UnauthorizedOperationException {
        User user = CURRENT.get();
        if (user == null) {
            throw new UnauthorizedOperationException(
                    "You must be logged in to perform this operation");
        }
        boolean openToAllLoggedIn =
                allowedRoles == null || allowedRoles.length == 0;
        if (!openToAllLoggedIn && !hasRole(allowedRoles)) {
            throw new UnauthorizedOperationException("Role "
                    + user.getRole().getLabel()
                    + " is not permitted to perform this operation");
        }
    }

    public Long currentUserId() {
        User user = CURRENT.get();
        return user == null ? null : user.getId();
    }

    /** The volunteer record linked to the logged-in auth user (nullable). */
    public Volunteer getCurrentVolunteer() {
        User user = CURRENT.get();
        if (user == null || user.getId() == null) {
            return null;
        }
        try {
            return new com.resqhub.dao.VolunteerDAO().findByUserId(user.getId());
        } catch (com.resqhub.exception.DataAccessException e) {
            return null;
        }
    }
}
