package com.resqhub.service;

import com.resqhub.dto.UserRegistrationDto;
import com.resqhub.model.Role;
import com.resqhub.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Load existing user by username and verify authority")
    void testLoadUserByUsername() {
        UserDetails adminDetails = userService.loadUserByUsername("admin");
        assertNotNull(adminDetails);
        assertEquals("admin", adminDetails.getUsername());
        assertTrue(adminDetails.isEnabled());
        assertTrue(adminDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Verify duplicate username rejection")
    void testDuplicateUsernameRejected() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("admin"); // existing
        dto.setEmail("unique." + UUID.randomUUID() + "@example.com");
        dto.setFullName("Test User");
        dto.setPassword("Password@123");
        dto.setConfirmPassword("Password@123");

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
    }

    @Test
    @DisplayName("Verify successful citizen registration with BCrypt hashing")
    void testSuccessfulRegistration() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "citizen_" + uniqueSuffix;
        String email = "citizen_" + uniqueSuffix + "@example.com";

        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFullName("Citizen Test");
        dto.setPassword("Secret@123");
        dto.setConfirmPassword("Secret@123");
        dto.setRequestedRole(Role.CITIZEN);

        Long newId = userService.registerUser(dto);
        assertNotNull(newId);

        Optional<User> savedUser = userService.findById(newId);
        assertTrue(savedUser.isPresent());
        assertEquals(username, savedUser.get().getUsername());
        assertEquals(email, savedUser.get().getEmail());
        assertEquals(Role.CITIZEN, savedUser.get().getRole());
        assertTrue(passwordEncoder.matches("Secret@123", savedUser.get().getPasswordHash()));
    }

    @Test
    @DisplayName("Verify public registration prevents ADMIN role escalation")
    void testRoleEscalationPrevention() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "attacker_" + uniqueSuffix;
        String email = "attacker_" + uniqueSuffix + "@example.com";

        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername(username);
        dto.setEmail(email);
        dto.setFullName("Attacker Test");
        dto.setPassword("Secret@123");
        dto.setConfirmPassword("Secret@123");
        // Attempt to request ADMIN role via public registration
        dto.setRequestedRole(Role.ADMIN);

        Long newId = userService.registerUser(dto);
        Optional<User> savedUser = userService.findById(newId);
        assertTrue(savedUser.isPresent());
        // Must be clamped to CITIZEN
        assertEquals(Role.CITIZEN, savedUser.get().getRole());
    }

    @Test
    @DisplayName("Verify non-existent username throws UsernameNotFoundException")
    void testUnknownUserThrowsException() {
        assertThrows(UsernameNotFoundException.class, () ->
                userService.loadUserByUsername("non_existent_user_99999"));
    }
}
