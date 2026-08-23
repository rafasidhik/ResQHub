package com.resqhub.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.resqhub.config.DatabaseConnectionManager;
import com.resqhub.exception.AuthenticationException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.util.PasswordUtil;
import com.resqhub.util.ValidationUtil;

/**
 * Phase 2 smoke test - foundation layer only.
 * Run after compile.bat:
 *   java -cp "out;lib\*;resources" com.resqhub.test.Phase2Test
 */
public class Phase2Test {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testSingletonIdentity();
        testJdbcConnection();
        testPasswordHashing();
        testValidationRules();
        testCustomExceptions();

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testSingletonIdentity() {
        try {
            DatabaseConnectionManager first = DatabaseConnectionManager.getInstance();
            DatabaseConnectionManager second = DatabaseConnectionManager.getInstance();
            check("Singleton returns same instance", first == second);
        } catch (ResQHubException e) {
            check("Singleton returns same instance", false);
            System.out.println("   reason: " + e.getMessage());
        }
    }

    private static void testJdbcConnection() {
        String sql = "SELECT COUNT(*) FROM roles";
        try {
            DatabaseConnectionManager manager = DatabaseConnectionManager.getInstance();
            try (Connection con = manager.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                rs.next();
                int roleCount = rs.getInt(1);
                check("JDBC query works, roles table has 7 rows",
                        roleCount == 7);
            }
        } catch (SQLException e) {
            check("JDBC query works, roles table has 7 rows", false);
            System.out.println("   reason: " + e.getMessage());
        } catch (ResQHubException e) {
            check("JDBC query works, roles table has 7 rows", false);
            System.out.println("   reason: " + e.getMessage());
        }
    }

    private static void testPasswordHashing() {
        String seedHash =
                "e86f78a8a3caf0b60d8e74e5942aa6d86dc150cd3c03338aef25b7d2d7e3acc7";
        check("SHA-256('Admin@123') equals seeded users.password_hash",
                PasswordUtil.hash("Admin@123").equals(seedHash));
        check("Wrong password rejected by matches()",
                !PasswordUtil.matches("wrongpass", seedHash));
    }

    private static void testValidationRules() {
        check("requireNonBlank varargs accepts valid values",
                ValidationUtil.requireNonBlank("a", "b c", "d"));
        check("requireNonBlank varargs rejects blank value",
                !ValidationUtil.requireNonBlank("a", "  ", "c"));
        check("Name validation", ValidationUtil.isValidName("Anand Menon")
                && !ValidationUtil.isValidName("123Bad"));
        check("Phone validation", ValidationUtil.isValidPhone("9847000001")
                && !ValidationUtil.isValidPhone("12345"));
        check("Email validation", ValidationUtil.isValidEmail("rafa@resqhub.org")
                && !ValidationUtil.isValidEmail("no-at-sign"));
        check("Age bounds", ValidationUtil.isValidAge(34)
                && !ValidationUtil.isValidAge(500));
        check("Chronology rule",
                ValidationUtil.isChronological(java.time.LocalDateTime.now(),
                        java.time.LocalDateTime.now().plusDays(1)));
    }

    private static void testCustomExceptions() {
        try {
            simulateLoginFailure();
            check("AuthenticationException thrown and caught", false);
        } catch (AuthenticationException e) {
            check("AuthenticationException thrown and caught",
                    e.getMessage().contains("locked"));
        } catch (ResQHubException e) {
            check("AuthenticationException thrown and caught", false);
        } finally {
            System.out.println("   exception handling block executed");
        }
    }

    private static void simulateLoginFailure() throws ResQHubException {
        throw new AuthenticationException(
                "Account locked after too many failed logins");
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + label);
        } else {
            failed++;
            System.out.println("[FAIL] " + label);
        }
    }
}
