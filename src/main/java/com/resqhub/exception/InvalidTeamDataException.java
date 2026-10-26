package com.resqhub.exception;

/**
 * Thrown when rescue team data fails validation
 * (duplicate team name, invalid contact, member count below 1...).
 */
public class InvalidTeamDataException extends ResQHubException {

    public InvalidTeamDataException(String message) {
        super(message);
    }
}
