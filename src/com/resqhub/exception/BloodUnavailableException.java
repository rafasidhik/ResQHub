package com.resqhub.exception;

/**
 * Thrown when no compatible available donor can be found for an emergency
 * blood request (spec: Blood Unavailable Exception, Error Handling).
 */
public class BloodUnavailableException extends ResQHubException {

    private final String bloodGroup;
    private final int unitsRequired;

    public BloodUnavailableException(String bloodGroup, int unitsRequired,
            String message) {
        super(message);
        this.bloodGroup = bloodGroup;
        this.unitsRequired = unitsRequired;
    }

    public BloodUnavailableException(String bloodGroup, int unitsRequired) {
        this(bloodGroup, unitsRequired,
                "No compatible available donor found for "
                        + bloodGroup + " x" + unitsRequired);
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public int getUnitsRequired() {
        return unitsRequired;
    }
}
