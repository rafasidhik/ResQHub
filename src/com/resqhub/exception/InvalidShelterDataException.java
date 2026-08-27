package com.resqhub.exception;

/** Thrown when shelter or allocation data fails validation. */
public class InvalidShelterDataException extends ResQHubException {

    public InvalidShelterDataException(String message) {
        super(message);
    }
}
