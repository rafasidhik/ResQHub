package com.resqhub.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PasswordUtil Cryptographic and Upgrade Tests")
class PasswordUtilTest {

    @Test
    @DisplayName("Modern PBKDF2 hash creates salted hash with 210,000 iterations")
    void testPbkdf2HashGenerationAndMatching() {
        String raw = "SecurePass@2026";
        String hash1 = PasswordUtil.hash(raw);
        String hash2 = PasswordUtil.hash(raw);

        assertNotNull(hash1);
        assertTrue(hash1.startsWith("pbkdf2$210000$"));
        // Random salt ensures same password produces distinct hashes
        assertNotEquals(hash1, hash2);

        assertTrue(PasswordUtil.matches(raw, hash1));
        assertTrue(PasswordUtil.matches(raw, hash2));
        assertFalse(PasswordUtil.matches("WrongPass", hash1));
    }

    @Test
    @DisplayName("Legacy 64-character SHA-256 hash matches correctly for backward compatibility")
    void testLegacySha256Matching() {
        // SHA-256 hex of 'ResQHub@123'
        String legacyHash = "5ebe770734a0ab28c24609b04644806d099b3925b4c43a3f9d7cef1b06c10f39";

        assertTrue(PasswordUtil.matches("ResQHub@123", legacyHash));
        assertFalse(PasswordUtil.matches("WrongPassword", legacyHash));
    }

    @Test
    @DisplayName("NeedsUpgrade flags legacy SHA-256 hashes but returns false for modern PBKDF2 hashes")
    void testNeedsUpgrade() {
        String legacyHash = "5ebe770734a0ab28c24609b04644806d099b3925b4c43a3f9d7cef1b06c10f39";
        String modernHash = PasswordUtil.hash("AnyPassword123");

        assertTrue(PasswordUtil.needsUpgrade(legacyHash));
        assertFalse(PasswordUtil.needsUpgrade(modernHash));
    }
}
