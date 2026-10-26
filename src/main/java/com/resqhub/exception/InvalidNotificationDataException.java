package com.resqhub.exception;

/**
 * Thrown when notification data is invalid (blank message, no recipient,
 * unknown type/priority, marking a notification that does not exist...).
 */
public class InvalidNotificationDataException extends ResQHubException {

    public InvalidNotificationDataException(String message) {
        super(message);
    }
}
