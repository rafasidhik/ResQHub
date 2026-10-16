package com.resqhub.service;

import com.resqhub.dao.NotificationDao;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationType;
import com.resqhub.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationDao notificationDao;

    @Autowired
    public NotificationServiceImpl(NotificationDao notificationDao) {
        this.notificationDao = notificationDao;
    }

    @Override
    public void sendUserNotification(Long userId, String title, String message, String linkUrl, NotificationType type) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setTitle(title);
        n.setMessage(message);
        n.setLinkUrl(linkUrl);
        n.setNotificationType(type != null ? type : NotificationType.SYSTEM);
        n.setRead(false);
        notificationDao.save(n);
    }

    @Override
    public void sendRoleNotification(Role targetRole, String title, String message, String linkUrl, NotificationType type) {
        Notification n = new Notification();
        n.setTargetRole(targetRole);
        n.setTitle(title);
        n.setMessage(message);
        n.setLinkUrl(linkUrl);
        n.setNotificationType(type != null ? type : NotificationType.SYSTEM);
        n.setRead(false);
        notificationDao.save(n);
    }

    @Override
    public void sendBroadcastNotification(String title, String message, String linkUrl, NotificationType type) {
        Notification n = new Notification();
        n.setTitle(title);
        n.setMessage(message);
        n.setLinkUrl(linkUrl);
        n.setNotificationType(type != null ? type : NotificationType.SYSTEM);
        n.setRead(false);
        notificationDao.save(n);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId, Role role, int limit) {
        return notificationDao.findByUserAndRole(userId, role, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId, Role role) {
        return notificationDao.countUnread(userId, role);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationDao.markAsRead(notificationId);
    }

    @Override
    public void markAllAsRead(Long userId, Role role) {
        notificationDao.markAllAsReadForUser(userId, role);
    }
}
