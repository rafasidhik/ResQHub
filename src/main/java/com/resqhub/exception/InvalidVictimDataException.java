package com.resqhub.exception;

/**
 * Thrown when victim data fails validation
 * (blank name/location, age out of range, invalid emergency status...).
 */
public class InvalidVictimDataException extends ResQHubException {

    public InvalidVictimDataException(String message) {
        super(message);
    }
}
