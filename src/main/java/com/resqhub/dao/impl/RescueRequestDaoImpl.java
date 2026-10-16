package com.resqhub.dao.impl;

import com.resqhub.dao.RescueRequestDao;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueRequestStatus;
import com.resqhub.model.Urgency;
import com.resqhub.model.VictimPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RescueRequestDaoImpl implements RescueRequestDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RescueRequest> requestRowMapper = (rs, rowNum) -> {
        RescueRequest r = new RescueRequest();
        r.setId(rs.getLong("id"));
        r.setRequestNumber(rs.getString("request_number"));
        
        long victimId = rs.getLong("victim_id");
        if (!rs.wasNull()) {
            r.setVictimId(victimId);
        }
        r.setDisasterId(rs.getLong("disaster_id"));
        
        long reportedBy = rs.getLong("reported_by_user_id");
        if (!rs.wasNull()) {
            r.setReportedByUserId(reportedBy);
        }
        
        r.setLocation(rs.getString("location"));
        r.setDescription(rs.getString("description"));
        r.setContactNumber(rs.getString("contact_number"));
        r.setPeopleCount(rs.getInt("people_count"));
        r.setUrgency(Urgency.valueOf(rs.getString("urgency")));
        r.setCalculatedPriority(VictimPriority.valueOf(rs.getString("calculated_priority")));
        r.setStatus(RescueRequestStatus.valueOf(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            r.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            r.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        try { r.setDisasterName(rs.getString("disaster_name")); } catch (Exception ignored) {}
        try { r.setVictimName(rs.getString("victim_name")); } catch (Exception ignored) {}
        try { r.setReportedByUsername(rs.getString("reported_by_username")); } catch (Exception ignored) {}
        try { r.setActiveTeamName(rs.getString("active_team_name")); } catch (Exception ignored) {}
        try {
            long tid = rs.getLong("active_team_id");
            if (!rs.wasNull()) r.setActiveTeamId(tid);
        } catch (Exception ignored) {}

        return r;
    };

    @Autowired
    public RescueRequestDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private String getBaseSelect() {
        return "SELECT r.*, d.name AS disaster_name, v.full_name AS victim_name, u.username AS reported_by_username, " +
               "t.name AS active_team_name, t.id AS active_team_id " +
               "FROM rescue_requests r " +
               "JOIN disasters d ON r.disaster_id = d.id " +
               "LEFT JOIN victims v ON r.victim_id = v.id " +
               "LEFT JOIN users u ON r.reported_by_user_id = u.id " +
               "LEFT JOIN rescue_assignments a ON (a.request_id = r.id AND a.status IN ('ASSIGNED', 'IN_PROGRESS')) " +
               "LEFT JOIN rescue_teams t ON a.team_id = t.id ";
    }

    @Override
    public Optional<RescueRequest> findById(Long id) {
        String sql = getBaseSelect() + "WHERE r.id = ?";
        try {
            RescueRequest req = jdbcTemplate.queryForObject(sql, requestRowMapper, id);
            return Optional.ofNullable(req);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<RescueRequest> findByRequestNumber(String requestNumber) {
        String sql = getBaseSelect() + "WHERE r.request_number = ?";
        try {
            RescueRequest req = jdbcTemplate.queryForObject(sql, requestRowMapper, requestNumber);
            return Optional.ofNullable(req);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Long save(RescueRequest request) {
        String sql = "INSERT INTO rescue_requests (request_number, victim_id, disaster_id, reported_by_user_id, " +
                     "location, description, contact_number, people_count, urgency, calculated_priority, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, request.getRequestNumber());
            if (request.getVictimId() != null) {
                ps.setLong(2, request.getVictimId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            ps.setLong(3, request.getDisasterId());
            if (request.getReportedByUserId() != null) {
                ps.setLong(4, request.getReportedByUserId());
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setString(5, request.getLocation());
            ps.setString(6, request.getDescription());
            ps.setString(7, request.getContactNumber());
            ps.setInt(8, request.getPeopleCount());
            ps.setString(9, request.getUrgency().name());
            ps.setString(10, request.getCalculatedPriority().name());
            ps.setString(11, request.getStatus().name());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            return keyHolder.getKey().longValue();
        }
        return null;
    }

    @Override
    public void update(RescueRequest request) {
        String sql = "UPDATE rescue_requests SET victim_id = ?, disaster_id = ?, location = ?, description = ?, " +
                     "contact_number = ?, people_count = ?, urgency = ?, calculated_priority = ?, status = ? " +
                     "WHERE id = ?";
        jdbcTemplate.update(sql,
                request.getVictimId(),
                request.getDisasterId(),
                request.getLocation(),
                request.getDescription(),
                request.getContactNumber(),
                request.getPeopleCount(),
                request.getUrgency().name(),
                request.getCalculatedPriority().name(),
                request.getStatus().name(),
                request.getId());
    }

    @Override
    public void updateStatus(Long id, RescueRequestStatus status) {
        String sql = "UPDATE rescue_requests SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    @Override
    public List<RescueRequest> findAll(int page, int size, String search, Long disasterId, String status, String priority) {
        StringBuilder sql = new StringBuilder(getBaseSelect() + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (r.request_number LIKE ? OR r.location LIKE ? OR r.description LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (disasterId != null && disasterId > 0) {
            sql.append("AND r.disaster_id = ? ");
            params.add(disasterId);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND r.status = ? ");
            params.add(status.trim());
        }

        if (priority != null && !priority.trim().isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append("AND r.calculated_priority = ? ");
            params.add(priority.trim());
        }

        sql.append("ORDER BY (r.status = 'PENDING') DESC, (r.calculated_priority = 'CRITICAL') DESC, r.created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.query(sql.toString(), requestRowMapper, params.toArray());
    }

    @Override
    public int count(String search, Long disasterId, String status, String priority) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM rescue_requests r WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (r.request_number LIKE ? OR r.location LIKE ? OR r.description LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (disasterId != null && disasterId > 0) {
            sql.append("AND r.disaster_id = ? ");
            params.add(disasterId);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND r.status = ? ");
            params.add(status.trim());
        }

        if (priority != null && !priority.trim().isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append("AND r.calculated_priority = ? ");
            params.add(priority.trim());
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    @Override
    public List<RescueRequest> findByReportedUserId(Long userId, int page, int size) {
        String sql = getBaseSelect() + "WHERE r.reported_by_user_id = ? ORDER BY r.created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, requestRowMapper, userId, size, page * size);
    }

    @Override
    public int countByReportedUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM rescue_requests WHERE reported_by_user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null ? count : 0;
    }

    @Override
    public int getNextSequence() {
        String currentYearPrefix = "REQ-" + Year.now().getValue() + "-%";
        String sql = "SELECT COUNT(*) FROM rescue_requests WHERE request_number LIKE ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, currentYearPrefix);
        return (count != null ? count : 0) + 1;
    }

    @Override
    public boolean hasActiveAssignment(Long requestId) {
        String sql = "SELECT COUNT(*) FROM rescue_assignments WHERE request_id = ? AND status IN ('ASSIGNED', 'IN_PROGRESS')";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, requestId);
        return count != null && count > 0;
    }
}
