package com.resqhub.exception;

/**
 * Thrown when an operation is rejected for workflow-state reasons
 * rather than validation: assigning an unavailable team, completing
 * an already-closed assignment, cancelling a rescued request, etc.
 */
public class OperationNotAllowedException extends ResQHubException {

    public OperationNotAllowedException(String message) {
        super(message);
    }
}
