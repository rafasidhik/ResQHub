package com.resqhub.util;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Shared validation rules used by every service class.
 * Methods return boolean so each service throws its own
 * domain-specific custom exception when a rule fails.
 */
public final class ValidationUtil {

    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z][A-Za-z .'-]{1,99}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("[0-9]{10}");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final int MIN_AGE = 0;
    private static final int MAX_AGE = 130;

    private ValidationUtil() {
    }

    /**
     * VARARGS demonstration: accepts any number of values and fails
     * if any one of them is null or blank.
     */
    public static boolean requireNonBlank(String... values) {
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidAge(int age) {
        return age >= MIN_AGE && age <= MAX_AGE;
    }

    public static boolean isPositive(int value) {
        return value > 0;
    }

    public static boolean isNonNegative(int value) {
        return value >= 0;
    }

    /** End date, when present, must not be before the start date. */
    public static boolean isChronological(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            return false;
        }
        return end == null || !end.isBefore(start);
    }

    public static String clean(String text) {
        return text == null ? null : text.trim();
    }
}
