package com.resqhub.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationUtil Unit Tests")
class ValidationUtilTest {

    @Test
    @DisplayName("Phone validation requires exactly 10 digits")
    void testPhoneValidation() {
        assertTrue(ValidationUtil.isValidPhone("9876543210"));
        assertTrue(ValidationUtil.isValidPhone("1234567890"));

        assertFalse(ValidationUtil.isValidPhone(null));
        assertFalse(ValidationUtil.isValidPhone(""));
        assertFalse(ValidationUtil.isValidPhone("12345"));
        assertFalse(ValidationUtil.isValidPhone("98765432101")); // 11 digits
        assertFalse(ValidationUtil.isValidPhone("987654321a")); // contains letter
        assertFalse(ValidationUtil.isValidPhone("+919876543210")); // contains +
    }

    @Test
    @DisplayName("Email validation checks standard email patterns")
    void testEmailValidation() {
        assertTrue(ValidationUtil.isValidEmail("admin@resqhub.org"));
        assertTrue(ValidationUtil.isValidEmail("user.name+tag@domain.co.in"));

        assertFalse(ValidationUtil.isValidEmail(null));
        assertFalse(ValidationUtil.isValidEmail(""));
        assertFalse(ValidationUtil.isValidEmail("notanemail"));
        assertFalse(ValidationUtil.isValidEmail("@missinguser.com"));
        assertFalse(ValidationUtil.isValidEmail("missingdomain@"));
    }

    @Test
    @DisplayName("Password policy requires at least 8 characters with letters and digits")
    void testPasswordPolicy() {
        assertTrue(ValidationUtil.isValidPassword("ResQHub@123"));
        assertTrue(ValidationUtil.isValidPassword("Passw0rd"));
        assertTrue(ValidationUtil.isValidPassword("secret12"));

        assertFalse(ValidationUtil.isValidPassword(null));
        assertFalse(ValidationUtil.isValidPassword(""));
        assertFalse(ValidationUtil.isValidPassword("short1")); // 6 chars
        assertFalse(ValidationUtil.isValidPassword("allletters")); // no digit
        assertFalse(ValidationUtil.isValidPassword("123456789")); // no letter
    }

    @Test
    @DisplayName("Username validation allows 4-50 letters, numbers, and underscores")
    void testUsernameValidation() {
        assertTrue(ValidationUtil.isValidUsername("admin"));
        assertTrue(ValidationUtil.isValidUsername("rescue_officer"));
        assertTrue(ValidationUtil.isValidUsername("user123"));

        assertFalse(ValidationUtil.isValidUsername(null));
        assertFalse(ValidationUtil.isValidUsername("abc")); // too short
        assertFalse(ValidationUtil.isValidUsername("user@name")); // invalid special char
        assertFalse(ValidationUtil.isValidUsername("user name")); // space
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 25, 65, 130})
    @DisplayName("Age validation accepts 0 to 130 inclusive")
    void testValidAge(int age) {
        assertTrue(ValidationUtil.isValidAge(age));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -50, 131, 200})
    @DisplayName("Age validation rejects negative or extreme values")
    void testInvalidAge(int age) {
        assertFalse(ValidationUtil.isValidAge(age));
    }

    @Test
    @DisplayName("Clean utility trims whitespace and handles nulls safely")
    void testClean() {
        assertNull(ValidationUtil.clean(null));
        assertEquals("", ValidationUtil.clean("   "));
        assertEquals("ResQHub", ValidationUtil.clean("  ResQHub  "));
    }
}
