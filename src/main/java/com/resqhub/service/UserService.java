package com.resqhub.service;

import com.resqhub.dto.UserEditDto;
import com.resqhub.dto.UserRegistrationDto;
import com.resqhub.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

public interface UserService extends UserDetailsService {

    Long registerUser(UserRegistrationDto dto);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    void updateUser(UserEditDto dto, Long currentAdminId);

    void toggleUserStatus(Long userId, Long currentAdminId);

    void changePassword(Long userId, String oldPassword, String newPassword);

    List<User> getUsers(int page, int size, String searchQuery, String roleFilter);

    int countUsers(String searchQuery, String roleFilter);
}
