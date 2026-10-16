package com.resqhub.dao.impl;

import com.resqhub.dao.RescueAssignmentDao;
import com.resqhub.model.RescueAssignment;
import com.resqhub.model.RescueAssignmentStatus;
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
import java.util.List;
import java.util.Optional;

@Repository
public class RescueAssignmentDaoImpl implements RescueAssignmentDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RescueAssignment> assignmentRowMapper = (rs, rowNum) -> {
        RescueAssignment a = new RescueAssignment();
        a.setId(rs.getLong("id"));
        a.setRequestId(rs.getLong("request_id"));
        a.setTeamId(rs.getLong("team_id"));
        a.setAssignedByUserId(rs.getLong("assigned_by_user_id"));
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) a.setStartTime(startTime.toLocalDateTime());
        Timestamp expected = rs.getTimestamp("expected_completion");
        if (expected != null) a.setExpectedCompletion(expected.toLocalDateTime());
        Timestamp actual = rs.getTimestamp("actual_completion");
        if (actual != null) a.setActualCompletion(actual.toLocalDateTime());

        a.setStatus(RescueAssignmentStatus.valueOf(rs.getString("status")));
        a.setProgressNotes(rs.getString("progress_notes"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) a.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) a.setUpdatedAt(updatedAt.toLocalDateTime());

        try { a.setRequestNumber(rs.getString("request_number")); } catch (Exception ignored) {}
        try { a.setTeamName(rs.getString("team_name")); } catch (Exception ignored) {}
        try { a.setTeamLeader(rs.getString("team_leader")); } catch (Exception ignored) {}
        try { a.setAssignedByUsername(rs.getString("assigned_by_username")); } catch (Exception ignored) {}

        return a;
    };

    @Autowired
    public RescueAssignmentDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private String getBaseSelect() {
        return "SELECT a.*, req.request_number, t.name AS team_name, t.team_leader, u.username AS assigned_by_username " +
               "FROM rescue_assignments a " +
               "JOIN rescue_requests req ON a.request_id = req.id " +
               "JOIN rescue_teams t ON a.team_id = t.id " +
               "JOIN users u ON a.assigned_by_user_id = u.id ";
    }

    @Override
    public Optional<RescueAssignment> findById(Long id) {
        String sql = getBaseSelect() + "WHERE a.id = ?";
        try {
            RescueAssignment a = jdbcTemplate.queryForObject(sql, assignmentRowMapper, id);
            return Optional.ofNullable(a);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<RescueAssignment> findActiveByRequestId(Long requestId) {
        String sql = getBaseSelect() + "WHERE a.request_id = ? AND a.status IN ('ASSIGNED', 'IN_PROGRESS')";
        try {
            RescueAssignment a = jdbcTemplate.queryForObject(sql, assignmentRowMapper, requestId);
            return Optional.ofNullable(a);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<RescueAssignment> findActiveByTeamId(Long teamId) {
        String sql = getBaseSelect() + "WHERE a.team_id = ? AND a.status IN ('ASSIGNED', 'IN_PROGRESS')";
        try {
            RescueAssignment a = jdbcTemplate.queryForObject(sql, assignmentRowMapper, teamId);
            return Optional.ofNullable(a);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Long save(RescueAssignment assignment) {
        String sql = "INSERT INTO rescue_assignments (request_id, team_id, assigned_by_user_id, status, progress_notes) " +
                     "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, assignment.getRequestId());
            ps.setLong(2, assignment.getTeamId());
            ps.setLong(3, assignment.getAssignedByUserId());
            ps.setString(4, assignment.getStatus().name());
            ps.setString(5, assignment.getProgressNotes());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            return keyHolder.getKey().longValue();
        }
        return null;
    }

    @Override
    public void updateStatus(Long id, RescueAssignmentStatus status, String progressNotes) {
        String sql = "UPDATE rescue_assignments SET status = ?, progress_notes = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), progressNotes, id);
    }

    @Override
    public void completeAssignment(Long id, String progressNotes) {
        String sql = "UPDATE rescue_assignments SET status = 'COMPLETED', actual_completion = CURRENT_TIMESTAMP, " +
                     "progress_notes = ? WHERE id = ?";
        jdbcTemplate.update(sql, progressNotes, id);
    }

    @Override
    public List<RescueAssignment> findHistoryByTeamId(Long teamId) {
        String sql = getBaseSelect() + "WHERE a.team_id = ? ORDER BY a.created_at DESC";
        return jdbcTemplate.query(sql, assignmentRowMapper, teamId);
    }

    @Override
    public List<RescueAssignment> findHistoryByRequestId(Long requestId) {
        String sql = getBaseSelect() + "WHERE a.request_id = ? ORDER BY a.created_at DESC";
        return jdbcTemplate.query(sql, assignmentRowMapper, requestId);
    }
}
