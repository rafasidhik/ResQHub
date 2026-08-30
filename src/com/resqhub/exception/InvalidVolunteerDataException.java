package com.resqhub.exception;

/**
 * Thrown when volunteer data or an assignment is invalid
 * (duplicate record, missing skill, unavailable volunteer...).
 */
public class InvalidVolunteerDataException extends ResQHubException {

    public InvalidVolunteerDataException(String message) {
        super(message);
    }
}
