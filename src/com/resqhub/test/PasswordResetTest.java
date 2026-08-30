package com.resqhub.test;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.AuthController;

/**
 * Forgot-password self-service test.
 * Registers a disposable citizen, resets its password through the
 * username + email verification flow (no session), then confirms the
 * new password logs in and the old one no longer works.
 */
public class PasswordResetTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        try {
            preClean();
            runScenario();
        } catch (Exception e) {
            System.out.println("[FATAL] " + e);
            e.printStackTrace();
            failed++;
        } finally {
            cleanUp();
        }
        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runScenario() throws Exception {
        System.out.println("--- Forgot-password flow -------------------------------");

        AuthController auth = new AuthController();
        ActionResult signup = auth.registerCitizen("zzfp_citizen",
                "Original@123", "ZZFP Citizen", "zzfp@resqhub.local", null);
        check("disposable citizen account created", signup.isSuccess());

        // Wrong email must be rejected.
        ActionResult wrongEmail = auth.resetForgottenPassword(
                "zzfp_citizen", "wrong@email.com", "Newpass@123", "Newpass@123");
        check("wrong email rejected", !wrongEmail.isSuccess());

        // Unknown username rejected.
        ActionResult wrongUser = auth.resetForgottenPassword(
                "no_such_user", "zzfp@resqhub.local", "Newpass@123", "Newpass@123");
        check("unknown username rejected", !wrongUser.isSuccess());

        // Mismatched confirm rejected.
        ActionResult mismatch = auth.resetForgottenPassword(
                "zzfp_citizen", "zzfp@resqhub.local", "Newpass@123", "Different@1");
        check("mismatched confirmation rejected", !mismatch.isSuccess());

        // Weak password rejected.
        ActionResult weak = auth.resetForgottenPassword(
                "zzfp_citizen", "zzfp@resqhub.local", "short", "short");
        check("weak password rejected", !weak.isSuccess());

        // Correct username + email resets the password.
        ActionResult reset = auth.resetForgottenPassword(
                "zzfp_citizen", "zzfp@resqhub.local", "Newpass@123", "Newpass@123");
        check("valid username + email resets password", reset.isSuccess());

        check("new password logs in",
                auth.login("zzfp_citizen", "Newpass@123").isSuccess());
        auth.logout();
        check("old password no longer works",
                !auth.login("zzfp_citizen", "Original@123").isSuccess());
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("[PASS] " + label);
        } else {
            failed++;
            System.out.println("[FAIL] " + label);
        }
    }

    private static void preClean() throws Exception {
        cleanUp();
    }

    private static void cleanUp() {
        try {
            java.sql.Connection connection =
                    com.resqhub.config.DatabaseConnectionManager.getInstance()
                            .getConnection();
            try (java.sql.PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM users WHERE username LIKE 'zzfp_%'")) {
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException(
                    "password-reset cleanup failed: " + e.getMessage(), e);
        }
    }
}
