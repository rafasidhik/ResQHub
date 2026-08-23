package com.resqhub.exception;

/**
 * Thrown when the logged-in user's role does not permit an operation,
 * e.g. a CITIZEN trying to register a rescue team.
 */
public class UnauthorizedOperationException extends ResQHubException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
