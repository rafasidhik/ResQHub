package com.resqhub.exception;

/** Thrown when blood donor / request / match data is invalid (spec: Validation, Error Handling). */
public class InvalidBloodDataException extends ResQHubException {
    public InvalidBloodDataException(String message) {
        super(message);
    }
}
