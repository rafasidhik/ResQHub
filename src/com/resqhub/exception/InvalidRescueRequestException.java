package com.resqhub.exception;

/**
 * Thrown when rescue request data fails validation
 * (no people, children count > people count, no location...).
 */
public class InvalidRescueRequestException extends ResQHubException {

    public InvalidRescueRequestException(String message) {
        super(message);
    }
}
