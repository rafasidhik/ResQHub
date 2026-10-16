package com.resqhub.dao;

import com.resqhub.model.User;

import java.util.List;
import java.util.Optional;

public interface UserDao {

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Long save(User user);

    void update(User user);

    void updateStatus(Long id, String status);

    void updatePassword(Long id, String passwordHash);

    List<User> findAll(int page, int size, String searchQuery, String roleFilter);

    int count(String searchQuery, String roleFilter);
}
