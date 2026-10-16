package com.resqhub.service;

import com.resqhub.model.Notification;
import com.resqhub.model.NotificationType;
import com.resqhub.model.Role;

import java.util.List;

public interface NotificationService {
    void sendUserNotification(Long userId, String title, String message, String linkUrl, NotificationType type);
    void sendRoleNotification(Role targetRole, String title, String message, String linkUrl, NotificationType type);
    void sendBroadcastNotification(String title, String message, String linkUrl, NotificationType type);
    List<Notification> getUserNotifications(Long userId, Role role, int limit);
    int getUnreadCount(Long userId, Role role);
    void markAsRead(Long notificationId);
    void markAllAsRead(Long userId, Role role);
}
