package com.resqhub.service.impl;

import com.resqhub.dao.UserDao;
import com.resqhub.dto.UserEditDto;
import com.resqhub.dto.UserRegistrationDto;
import com.resqhub.model.Role;
import com.resqhub.model.User;
import com.resqhub.service.ActivityLogService;
import com.resqhub.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;

    @Autowired
    public UserServiceImpl(UserDao userDao, PasswordEncoder passwordEncoder, ActivityLogService activityLogService) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        boolean enabled = user.isActive();
        boolean accountNonExpired = true;
        boolean credentialsNonExpired = true;
        boolean accountNonLocked = true;

        String authority = "ROLE_" + user.getRole().name();
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                enabled,
                accountNonExpired,
                credentialsNonExpired,
                accountNonLocked,
                Collections.singletonList(new SimpleGrantedAuthority(authority))
        );
    }

    @Override
    @Transactional
    public Long registerUser(UserRegistrationDto dto) {
        if (userDao.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken.");
        }
        if (userDao.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email address '" + dto.getEmail() + "' is already registered.");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setFullName(dto.getFullName().trim());
        user.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);

        // Security rule: public registration never allows ADMIN or operational officers
        Role assignedRole = (dto.getRequestedRole() == Role.VOLUNTEER) ? Role.VOLUNTEER : Role.CITIZEN;
        user.setRole(assignedRole);
        user.setStatus("ACTIVE");

        Long newUserId = userDao.save(user);

        activityLogService.log(newUserId, "REGISTER", "AUTH", String.valueOf(newUserId),
                "New user registered with role " + assignedRole.name());

        return newUserId;
    }

    @Override
    public Optional<User> findById(Long id) {
        return userDao.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userDao.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userDao.findByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userDao.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userDao.existsByEmail(email);
    }

    @Override
    @Transactional
    public void updateUser(UserEditDto dto, Long currentAdminId) {
        User existing = userDao.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + dto.getId()));

        // Validate email uniqueness if changed
        if (!existing.getEmail().equalsIgnoreCase(dto.getEmail()) && userDao.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already used by another account.");
        }

        existing.setEmail(dto.getEmail().trim().toLowerCase());
        existing.setFullName(dto.getFullName().trim());
        existing.setPhone(dto.getPhone() != null ? dto.getPhone().trim() : null);
        existing.setRole(dto.getRole());
        existing.setStatus(dto.getStatus());

        if (dto.getNewPassword() != null && !dto.getNewPassword().trim().isEmpty()) {
            if (dto.getNewPassword().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters.");
            }
            existing.setPasswordHash(passwordEncoder.encode(dto.getNewPassword().trim()));
            userDao.updatePassword(existing.getId(), existing.getPasswordHash());
        }

        userDao.update(existing);

        activityLogService.log(currentAdminId, "UPDATE_USER", "USER_MGMT", String.valueOf(existing.getId()),
                "Updated user account " + existing.getUsername() + " (Role: " + existing.getRole().name() + ", Status: " + existing.getStatus() + ")");
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long userId, Long currentAdminId) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        // Prevent admin from disabling own account
        if (userId.equals(currentAdminId)) {
            throw new IllegalArgumentException("You cannot deactivate your own administrative account.");
        }

        String newStatus = "ACTIVE".equalsIgnoreCase(user.getStatus()) ? "INACTIVE" : "ACTIVE";
        userDao.updateStatus(userId, newStatus);

        activityLogService.log(currentAdminId, "TOGGLE_STATUS", "USER_MGMT", String.valueOf(userId),
                "Changed user " + user.getUsername() + " status to " + newStatus);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userDao.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password does not match.");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters.");
        }

        String newHash = passwordEncoder.encode(newPassword);
        userDao.updatePassword(userId, newHash);

        activityLogService.log(userId, "CHANGE_PASSWORD", "PROFILE", String.valueOf(userId),
                "User changed account password successfully.");
    }

    @Override
    public List<User> getUsers(int page, int size, String searchQuery, String roleFilter) {
        return userDao.findAll(page, size, searchQuery, roleFilter);
    }

    @Override
    public int countUsers(String searchQuery, String roleFilter) {
        return userDao.count(searchQuery, roleFilter);
    }
}
