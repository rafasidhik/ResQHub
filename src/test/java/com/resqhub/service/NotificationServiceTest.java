package com.resqhub.service;

import com.resqhub.dao.NotificationDao;
import com.resqhub.model.Notification;
import com.resqhub.model.NotificationType;
import com.resqhub.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationDao notificationDao;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUserId(5L);
        testNotification.setTitle("New Rescue Assignment");
        testNotification.setMessage("You have been assigned to Mission Alpha");
        testNotification.setLinkUrl("/rescue-requests/1");
        testNotification.setNotificationType(NotificationType.RESCUE);
        testNotification.setRead(false);
    }

    @Test
    void testSendUserNotification_Success() {
        when(notificationDao.save(any(Notification.class))).thenReturn(10L);

        notificationService.sendUserNotification(5L, "Emergency Alert", "Evacuation ordered", "/shelters", NotificationType.DISASTER);

        verify(notificationDao, times(1)).save(any(Notification.class));
    }

    @Test
    void testSendRoleNotification_Success() {
        when(notificationDao.save(any(Notification.class))).thenReturn(11L);

        notificationService.sendRoleNotification(Role.RESCUE_OFFICER, "Squad Briefing", "Report to Sector 2", "/rescue-teams", NotificationType.RESCUE);

        verify(notificationDao, times(1)).save(any(Notification.class));
    }

    @Test
    void testSendBroadcastNotification_Success() {
        when(notificationDao.save(any(Notification.class))).thenReturn(12L);

        notificationService.sendBroadcastNotification("System Maintenance", "Scheduled update tonight", null, NotificationType.SYSTEM);

        verify(notificationDao, times(1)).save(any(Notification.class));
    }

    @Test
    void testGetUserNotifications_Success() {
        when(notificationDao.findByUserAndRole(5L, Role.CITIZEN, 20)).thenReturn(Collections.singletonList(testNotification));

        List<Notification> result = notificationService.getUserNotifications(5L, Role.CITIZEN, 20);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("New Rescue Assignment", result.get(0).getTitle());
    }

    @Test
    void testGetUnreadCount_Success() {
        when(notificationDao.countUnread(5L, Role.CITIZEN)).thenReturn(3);

        int count = notificationService.getUnreadCount(5L, Role.CITIZEN);

        assertEquals(3, count);
    }

    @Test
    void testMarkAsRead_Success() {
        notificationService.markAsRead(1L);

        verify(notificationDao, times(1)).markAsRead(1L);
    }

    @Test
    void testMarkAllAsRead_Success() {
        notificationService.markAllAsRead(5L, Role.CITIZEN);

        verify(notificationDao, times(1)).markAllAsReadForUser(5L, Role.CITIZEN);
    }
}
