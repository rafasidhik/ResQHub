package com.resqhub.exception;

/**
 * Thrown when hospital registration or capacity data fails validation
 * (e.g. occupied beds exceeding total capacity - spec section 18).
 */
public class InvalidHospitalDataException extends ResQHubException {
    public InvalidHospitalDataException(String message) {
        super(message);
    }
}
