package com.resqhub.exception;

/**
 * Thrown when user account data fails validation
 * (bad username format, weak password, duplicate username/email...).
 */
public class InvalidUserDataException extends ResQHubException {

    public InvalidUserDataException(String message) {
        super(message);
    }
}
