package com.resqhub.exception;

/**
 * Thrown by the smart allocation engine when no shelter can currently
 * accept the victim / family (all are full, inactive or lack the
 * required facilities / accessibility).
 */
public class NoSuitableShelterException extends ResQHubException {

    public NoSuitableShelterException(String message) {
        super(message);
    }
}
