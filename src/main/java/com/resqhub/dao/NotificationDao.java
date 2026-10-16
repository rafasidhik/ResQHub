package com.resqhub.dao;

import com.resqhub.model.Notification;
import com.resqhub.model.Role;

import java.util.List;
import java.util.Optional;

public interface NotificationDao {
    Optional<Notification> findById(Long id);
    List<Notification> findByUserAndRole(Long userId, Role role, int limit);
    int countUnread(Long userId, Role role);
    Long save(Notification notification);
    void markAsRead(Long id);
    void markAllAsReadForUser(Long userId, Role role);
    void delete(Long id);
}
