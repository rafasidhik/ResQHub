package com.resqhub.exception;

/** Thrown when resource or inventory data fails validation, e.g. a
 *  stock-out request that would push quantity below zero. */
public class InvalidResourceDataException extends ResQHubException {

    public InvalidResourceDataException(String message) {
        super(message);
    }
}
