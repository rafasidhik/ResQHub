package com.resqhub.service;

import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;

/**
 * EAGER singleton holding the logged-in user for this application run.
 *
 * Singleton style comparison (viva point): DatabaseConnectionManager uses
 * lazy initialisation because opening config/connections is expensive and
 * may fail; SessionManager is eager - it is cheap, cannot fail, and is
 * guaranteed needed in every GUI session.
 */
public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private User currentUser;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public void login(User user) {
        this.currentUser = user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    /** VARARGS role check: hasRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER). */
    public boolean hasRole(RoleType... allowedRoles) {
        if (!isLoggedIn() || allowedRoles == null) {
            return false;
        }
        for (RoleType allowed : allowedRoles) {
            if (allowed == currentUser.getRole()) {
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
        if (!isLoggedIn()) {
            throw new UnauthorizedOperationException(
                    "You must be logged in to perform this operation");
        }
        boolean openToAllLoggedIn =
                allowedRoles == null || allowedRoles.length == 0;
        if (!openToAllLoggedIn && !hasRole(allowedRoles)) {
            throw new UnauthorizedOperationException("Role "
                    + currentUser.getRole().getLabel()
                    + " is not permitted to perform this operation");
        }
    }

    public Long currentUserId() {
        return isLoggedIn() ? currentUser.getId() : null;
    }
}
