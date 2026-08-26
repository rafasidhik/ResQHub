package com.resqhub.service;

import java.time.LocalDateTime;
import java.util.List;

import com.resqhub.dao.AccountDeletionRequestDAO;
import com.resqhub.dao.UserDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidUserDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.AccountDeletionRequest;
import com.resqhub.model.DeletionRequestStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;

/**
 * Account deletion request workflow: users request, admins review.
 */
public class AccountDeletionRequestService {

    private final AccountDeletionRequestDAO requestDAO =
            new AccountDeletionRequestDAO();
    private final UserDAO userDAO = new UserDAO();
    private final SessionManager session = SessionManager.getInstance();

    /** Any logged-in user can request deletion of their own account. */
    public AccountDeletionRequest requestDeletion()
            throws InvalidUserDataException, DataAccessException {

        if (!session.isLoggedIn()) {
            throw new InvalidUserDataException("No active session");
        }
        long userId = session.currentUserId();

        AccountDeletionRequest existing =
                requestDAO.findPendingByUser(userId);
        if (existing != null) {
            throw new InvalidUserDataException(
                    "You already have a pending deletion request");
        }

        AccountDeletionRequest request =
                new AccountDeletionRequest(userId);
        return requestDAO.save(request);
    }

    /** ADMIN view of all pending requests. */
    public List<AccountDeletionRequest> getPendingRequests()
            throws UnauthorizedOperationException, DataAccessException {

        session.requireRole(RoleType.ADMIN);
        return requestDAO.findByStatus(DeletionRequestStatus.PENDING);
    }

    /** Count of pending requests (no role guard — used by stats). */
    public int countPending() throws DataAccessException {
        return requestDAO.findByStatus(DeletionRequestStatus.PENDING).size();
    }

    /** ADMIN view of all requests (any status). */
    public List<AccountDeletionRequest> getAllRequests()
            throws UnauthorizedOperationException, DataAccessException {

        session.requireRole(RoleType.ADMIN);
        return requestDAO.findAll();
    }

    /** ADMIN approve: deletes the target user account. */
    public void approveRequest(long requestId, String notes)
            throws UnauthorizedOperationException,
            InvalidUserDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN);
        AccountDeletionRequest request = requireExisting(requestId);
        if (request.getStatus() != DeletionRequestStatus.PENDING) {
            throw new InvalidUserDataException(
                    "Request #" + requestId + " is already "
                            + request.getStatus().getLabel());
        }
        if (session.currentUserId() == request.getUserId()) {
            throw new InvalidUserDataException(
                    "You cannot approve deletion of your own account");
        }

        User target = userDAO.findById(request.getUserId());
        if (target == null) {
            request.setStatus(DeletionRequestStatus.APPROVED);
            request.setReviewedBy(session.currentUserId());
            request.setReviewedAt(LocalDateTime.now());
            request.setAdminNotes("User already removed");
            requestDAO.save(request);
            return;
        }

        request.setStatus(DeletionRequestStatus.APPROVED);
        request.setReviewedBy(session.currentUserId());
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminNotes(notes);
        requestDAO.save(request);

        userDAO.deleteById(request.getUserId());
    }

    /** ADMIN deny: keeps the account, records the decision. */
    public void denyRequest(long requestId, String notes)
            throws UnauthorizedOperationException,
            InvalidUserDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN);
        AccountDeletionRequest request = requireExisting(requestId);
        if (request.getStatus() != DeletionRequestStatus.PENDING) {
            throw new InvalidUserDataException(
                    "Request #" + requestId + " is already "
                            + request.getStatus().getLabel());
        }

        request.setStatus(DeletionRequestStatus.DENIED);
        request.setReviewedBy(session.currentUserId());
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminNotes(notes);
        requestDAO.save(request);
    }

    private AccountDeletionRequest requireExisting(long id)
            throws InvalidUserDataException, DataAccessException {
        AccountDeletionRequest request = requestDAO.findById(id);
        if (request == null) {
            throw new InvalidUserDataException(
                    "No deletion request with id " + id);
        }
        return request;
    }
}
