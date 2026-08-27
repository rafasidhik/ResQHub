package com.resqhub.exception;

/**
 * Thrown when a report cannot be generated (unsupported type, invalid
 * filter value, or an aggregation query fails).
 */
public class InvalidReportException extends ResQHubException {

    public InvalidReportException(String message) {
        super(message);
    }
}
