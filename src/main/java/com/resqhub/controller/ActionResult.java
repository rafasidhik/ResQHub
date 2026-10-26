package com.resqhub.controller;

/**
 * Outcome of any controller operation, ready to be shown in a
 * JOptionPane by a view. Views never see checked exceptions -
 * controllers convert every failure into a readable message.
 */
public final class ActionResult {

    private final boolean success;
    private final String message;
    private final Object data;

    private ActionResult(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    public static ActionResult success(String message) {
        return new ActionResult(true, message, null);
    }

    public static ActionResult successWithData(String message, Object data) {
        return new ActionResult(true, message, data);
    }

    public static ActionResult failure(String message) {
        return new ActionResult(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    /** Typed convenience accessor for the payload, if present. */
    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }
}
