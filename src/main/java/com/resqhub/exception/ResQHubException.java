package com.resqhub.exception;

/**
 * Base checked exception for the whole ResQHub system.
 * Every custom exception extends this class so callers can catch
 * either a specific problem or all ResQHub problems with one catch.
 */
public class ResQHubException extends Exception {

    public ResQHubException() {
        super();
    }

    public ResQHubException(String message) {
        super(message);
    }

    public ResQHubException(String message, Throwable cause) {
        super(message, cause);
    }
}
