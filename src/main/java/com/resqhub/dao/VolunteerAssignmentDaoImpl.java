package com.resqhub.dao;

import com.resqhub.model.TaskPriority;
import com.resqhub.model.VolunteerAssignment;
import com.resqhub.model.VolunteerAssignmentStatus;
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
public class VolunteerAssignmentDaoImpl implements VolunteerAssignmentDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public VolunteerAssignmentDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<VolunteerAssignment> rowMapper = (rs, rowNum) -> {
        VolunteerAssignment va = new VolunteerAssignment();
        va.setId(rs.getLong("id"));
        va.setVolunteerId(rs.getLong("volunteer_id"));
        va.setDisasterId(rs.getLong("disaster_id"));
        va.setTaskTitle(rs.getString("task_title"));
        va.setDescription(rs.getString("description"));
        va.setLocation(rs.getString("location"));
        
        String prioStr = rs.getString("priority");
        if (prioStr != null) {
            va.setPriority(TaskPriority.valueOf(prioStr));
        }
        
        Timestamp startTime = rs.getTimestamp("start_time");
        if (startTime != null) {
            va.setStartTime(startTime.toLocalDateTime());
        }
        
        Timestamp expComp = rs.getTimestamp("expected_completion");
        if (expComp != null) {
            va.setExpectedCompletion(expComp.toLocalDateTime());
        }
        
        String statStr = rs.getString("status");
        if (statStr != null) {
            va.setStatus(VolunteerAssignmentStatus.valueOf(statStr));
        }
        
        va.setProgressNotes(rs.getString("progress_notes"));
        va.setAssignedByUserId(rs.getLong("assigned_by_user_id"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            va.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            va.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        try {
            va.setVolunteerName(rs.getString("volunteer_name"));
        } catch (Exception ignored) {}
        try {
            va.setVolunteerIdentifier(rs.getString("volunteer_identifier"));
        } catch (Exception ignored) {}
        try {
            va.setDisasterName(rs.getString("disaster_name"));
        } catch (Exception ignored) {}
        try {
            va.setAssignedByUsername(rs.getString("assigned_by_username"));
        } catch (Exception ignored) {}

        return va;
    };

    private static final String BASE_SELECT =
            "SELECT va.*, v.full_name AS volunteer_name, v.volunteer_identifier, " +
            "d.name AS disaster_name, u.username AS assigned_by_username " +
            "FROM volunteer_assignments va " +
            "JOIN volunteers v ON va.volunteer_id = v.id " +
            "JOIN disasters d ON va.disaster_id = d.id " +
            "JOIN users u ON va.assigned_by_user_id = u.id ";

    @Override
    public Optional<VolunteerAssignment> findById(Long id) {
        String sql = BASE_SELECT + "WHERE va.id = ?";
        List<VolunteerAssignment> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<VolunteerAssignment> findAll() {
        String sql = BASE_SELECT + "ORDER BY va.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<VolunteerAssignment> findByVolunteerId(Long volunteerId) {
        String sql = BASE_SELECT + "WHERE va.volunteer_id = ? ORDER BY va.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, volunteerId);
    }

    @Override
    public List<VolunteerAssignment> findByDisasterId(Long disasterId) {
        String sql = BASE_SELECT + "WHERE va.disaster_id = ? ORDER BY va.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, disasterId);
    }

    @Override
    public List<VolunteerAssignment> findByStatus(VolunteerAssignmentStatus status) {
        String sql = BASE_SELECT + "WHERE va.status = ? ORDER BY va.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    @Override
    public Long save(VolunteerAssignment assignment) {
        String sql = "INSERT INTO volunteer_assignments (volunteer_id, disaster_id, task_title, description, location, priority, start_time, expected_completion, status, progress_notes, assigned_by_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, assignment.getVolunteerId());
            ps.setLong(2, assignment.getDisasterId());
            ps.setString(3, assignment.getTaskTitle());
            ps.setString(4, assignment.getDescription());
            ps.setString(5, assignment.getLocation());
            ps.setString(6, assignment.getPriority().name());
            ps.setTimestamp(7, assignment.getStartTime() != null ? Timestamp.valueOf(assignment.getStartTime()) : new Timestamp(System.currentTimeMillis()));
            if (assignment.getExpectedCompletion() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(assignment.getExpectedCompletion()));
            } else {
                ps.setNull(8, java.sql.Types.TIMESTAMP);
            }
            ps.setString(9, assignment.getStatus().name());
            ps.setString(10, assignment.getProgressNotes());
            ps.setLong(11, assignment.getAssignedByUserId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            assignment.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public void updateStatus(Long id, VolunteerAssignmentStatus status) {
        String sql = "UPDATE volunteer_assignments SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    @Override
    public void updateProgressNotes(Long id, String notes) {
        String sql = "UPDATE volunteer_assignments SET progress_notes = ? WHERE id = ?";
        jdbcTemplate.update(sql, notes, id);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM volunteer_assignments";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
