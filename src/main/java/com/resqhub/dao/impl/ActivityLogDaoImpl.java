package com.resqhub.dao.impl;

import com.resqhub.dao.ActivityLogDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
public class ActivityLogDaoImpl implements ActivityLogDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ActivityLogDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void log(Long userId, String action, String module, String affectedRecordId, String description) {
        String sql = "INSERT INTO activity_logs (user_id, action, module, affected_record_id, description) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, action, module, affectedRecordId, description);
    }

    @Override
    public List<Map<String, Object>> findRecent(int limit) {
        String sql = "SELECT l.*, u.username, u.full_name, u.role FROM activity_logs l " +
                     "LEFT JOIN users u ON l.user_id = u.id " +
                     "ORDER BY l.created_at DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, limit);
    }

    @Override
    public List<Map<String, Object>> findByUserId(Long userId, int limit) {
        String sql = "SELECT l.*, u.username, u.full_name, u.role FROM activity_logs l " +
                     "LEFT JOIN users u ON l.user_id = u.id " +
                     "WHERE l.user_id = ? " +
                     "ORDER BY l.created_at DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, userId, limit);
    }

    @Override
    public List<Map<String, Object>> findAll(int page, int size, String moduleFilter, String searchQuery) {
        StringBuilder sql = new StringBuilder(
                "SELECT l.*, u.username, u.full_name, u.role FROM activity_logs l " +
                "LEFT JOIN users u ON l.user_id = u.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (moduleFilter != null && !moduleFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(moduleFilter)) {
            sql.append("AND l.module = ? ");
            params.add(moduleFilter.trim());
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            sql.append("AND (l.action LIKE ? OR l.description LIKE ? OR u.username LIKE ?) ");
            String term = "%" + searchQuery.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        sql.append("ORDER BY l.created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    @Override
    public int count(String moduleFilter, String searchQuery) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM activity_logs l " +
                "LEFT JOIN users u ON l.user_id = u.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (moduleFilter != null && !moduleFilter.trim().isEmpty() && !"ALL".equalsIgnoreCase(moduleFilter)) {
            sql.append("AND l.module = ? ");
            params.add(moduleFilter.trim());
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            sql.append("AND (l.action LIKE ? OR l.description LIKE ? OR u.username LIKE ?) ");
            String term = "%" + searchQuery.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }
}
