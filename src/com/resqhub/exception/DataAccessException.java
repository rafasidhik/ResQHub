package com.resqhub.exception;

/**
 * Wraps java.sql.SQLException raised in the DAO layer.
 * Keeps JDBC details away from services/controllers while still
 * being a checked exception that forces callers to handle failures.
 */
public class DataAccessException extends ResQHubException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(String message) {
        super(message);
    }
}
