package com.resqhub.dao;

import com.resqhub.model.Notification;
import com.resqhub.model.NotificationType;
import com.resqhub.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationDaoImpl implements NotificationDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public NotificationDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Notification> rowMapper = (rs, rowNum) -> {
        Notification n = new Notification();
        n.setId(rs.getLong("id"));
        
        long userId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            n.setUserId(userId);
        }
        
        String roleStr = rs.getString("target_role");
        if (roleStr != null) {
            n.setTargetRole(Role.valueOf(roleStr));
        }
        
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setLinkUrl(rs.getString("link_url"));
        
        String typeStr = rs.getString("notification_type");
        if (typeStr != null) {
            n.setNotificationType(NotificationType.valueOf(typeStr));
        }
        
        n.setRead(rs.getBoolean("is_read"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            n.setCreatedAt(createdAt.toLocalDateTime());
        }
        return n;
    };

    @Override
    public Optional<Notification> findById(Long id) {
        String sql = "SELECT * FROM notifications WHERE id = ?";
        List<Notification> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Notification> findByUserAndRole(Long userId, Role role, int limit) {
        String roleName = role != null ? role.name() : null;
        String sql = "SELECT * FROM notifications WHERE user_id = ? OR (user_id IS NULL AND (target_role = ? OR target_role IS NULL)) ORDER BY is_read ASC, created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, rowMapper, userId, roleName, limit);
    }

    @Override
    public int countUnread(Long userId, Role role) {
        String roleName = role != null ? role.name() : null;
        String sql = "SELECT COUNT(*) FROM notifications WHERE is_read = FALSE AND (user_id = ? OR (user_id IS NULL AND (target_role = ? OR target_role IS NULL)))";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, roleName);
        return count != null ? count : 0;
    }

    @Override
    public Long save(Notification notification) {
        String sql = "INSERT INTO notifications (user_id, target_role, title, message, link_url, notification_type, is_read) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (notification.getUserId() != null) {
                ps.setLong(1, notification.getUserId());
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            if (notification.getTargetRole() != null) {
                ps.setString(2, notification.getTargetRole().name());
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }
            ps.setString(3, notification.getTitle());
            ps.setString(4, notification.getMessage());
            ps.setString(5, notification.getLinkUrl());
            ps.setString(6, notification.getNotificationType().name());
            ps.setBoolean(7, notification.isRead());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            notification.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public void markAsRead(Long id) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void markAllAsReadForUser(Long userId, Role role) {
        String roleName = role != null ? role.name() : null;
        String sql = "UPDATE notifications SET is_read = TRUE WHERE is_read = FALSE AND (user_id = ? OR (user_id IS NULL AND (target_role = ? OR target_role IS NULL)))";
        jdbcTemplate.update(sql, userId, roleName);
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM notifications WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
