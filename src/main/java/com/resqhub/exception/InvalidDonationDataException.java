package com.resqhub.exception;

/**
 * Thrown when donation data is invalid (bad amount, negative quantity,
 * missing donor, distribution exceeding available quantity...).
 */
public class InvalidDonationDataException extends ResQHubException {

    public InvalidDonationDataException(String message) {
        super(message);
    }
}
