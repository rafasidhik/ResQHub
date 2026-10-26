package com.resqhub.exception;

/**
 * Thrown by the authentication module on login failure:
 * unknown username, wrong password, or non-ACTIVE account.
 */
public class AuthenticationException extends ResQHubException {

    public AuthenticationException(String message) {
        super(message);
    }
}
