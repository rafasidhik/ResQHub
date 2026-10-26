package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.ResQHubException;
import com.resqhub.model.AccountDeletionRequest;
import com.resqhub.service.AccountDeletionRequestService;

/**
 * Controller for the account deletion request workflow.
 */
public class AccountDeletionRequestController {

    private final AccountDeletionRequestService service =
            new AccountDeletionRequestService();

    public ActionResult requestDeletion() {
        try {
            service.requestDeletion();
            return ActionResult.success(
                    "Deletion request submitted. An admin will review it.");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public List<AccountDeletionRequest> getPendingRequests() {
        try {
            return service.getPendingRequests();
        } catch (ResQHubException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AccountDeletionRequest> getAllRequests() {
        try {
            return service.getAllRequests();
        } catch (ResQHubException e) {
            throw new RuntimeException(e);
        }
    }

    public ActionResult approveRequest(long requestId, String notes) {
        try {
            service.approveRequest(requestId, notes);
            return ActionResult.success("Request approved and user deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult denyRequest(long requestId, String notes) {
        try {
            service.denyRequest(requestId, notes);
            return ActionResult.success("Request denied");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }
}
