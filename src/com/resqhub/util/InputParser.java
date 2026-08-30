package com.resqhub.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/** Converts raw Swing text-field content into typed, validated values. */
public final class InputParser {

    /** Shared with the views so input hints match parsing exactly. */
    public static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private InputParser() {
    }

    public static int parseInt(String text, String fieldName) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NullPointerException | NumberFormatException e) {
            NumberFormatException nfe = new NumberFormatException(
                    fieldName + " must be a whole number");
            throw nfe;
        }
    }

    public static long parseLong(String text, String fieldName) {
        try {
            return Long.parseLong(text.trim());
        } catch (NullPointerException | NumberFormatException e) {
            throw new NumberFormatException(fieldName + " must be a whole number");
        }
    }

    /** Parses a positive decimal amount (e.g. cash value); blank becomes 0. */
    public static double parseAmount(String text, String fieldName) {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(
                    fieldName + " must be a valid amount");
        }
    }

    /** Parses "yyyy-MM-dd HH:mm"; blank text becomes null (optional field). */
    public static LocalDateTime parseOptionalDateTime(String text)
            throws DateTimeParseException {
        String cleaned = text == null ? "" : text.trim();
        if (cleaned.isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(cleaned, DATE_TIME_FORMAT);
    }

    /** Trims input; empty strings become null for nullable DB columns. */
    public static String textOrNull(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
