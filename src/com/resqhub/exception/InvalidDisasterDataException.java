package com.resqhub.exception;

/**
 * Thrown when disaster data fails validation
 * (blank title/location, negative population, end before start...).
 */
public class InvalidDisasterDataException extends ResQHubException {

    public InvalidDisasterDataException(String message) {
        super(message);
    }
}
