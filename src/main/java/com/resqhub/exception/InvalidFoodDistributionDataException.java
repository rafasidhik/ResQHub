package com.resqhub.exception;

/**
 * Thrown when food distribution data or a workflow step is invalid
 * (missing fields, negative quantity, insufficient stock, distribution
 * exceeding the allocation, cancelled/completed request reuse...).
 */
public class InvalidFoodDistributionDataException
        extends ResQHubException {

    public InvalidFoodDistributionDataException(String message) {
        super(message);
    }
}
