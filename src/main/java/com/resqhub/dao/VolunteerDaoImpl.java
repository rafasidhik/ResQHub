package com.resqhub.dao;

import com.resqhub.model.Volunteer;
import com.resqhub.model.VolunteerApprovalStatus;
import com.resqhub.model.VolunteerAvailability;
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
public class VolunteerDaoImpl implements VolunteerDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public VolunteerDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Volunteer> rowMapper = (rs, rowNum) -> {
        Volunteer v = new Volunteer();
        v.setId(rs.getLong("id"));
        v.setVolunteerIdentifier(rs.getString("volunteer_identifier"));
        
        long userId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            v.setUserId(userId);
        }
        
        v.setFullName(rs.getString("full_name"));
        v.setEmail(rs.getString("email"));
        v.setPhone(rs.getString("phone"));
        v.setOperatingArea(rs.getString("operating_area"));
        v.setSkills(rs.getString("skills"));
        v.setExperienceYears(rs.getInt("experience_years"));
        
        String availStr = rs.getString("availability_status");
        if (availStr != null) {
            v.setAvailabilityStatus(VolunteerAvailability.valueOf(availStr));
        }
        
        String appStr = rs.getString("approval_status");
        if (appStr != null) {
            v.setApprovalStatus(VolunteerApprovalStatus.valueOf(appStr));
        }
        
        v.setEmergencyContact(rs.getString("emergency_contact"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            v.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            v.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return v;
    };

    @Override
    public Optional<Volunteer> findById(Long id) {
        String sql = "SELECT * FROM volunteers WHERE id = ?";
        List<Volunteer> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Volunteer> findByIdentifier(String identifier) {
        String sql = "SELECT * FROM volunteers WHERE volunteer_identifier = ?";
        List<Volunteer> list = jdbcTemplate.query(sql, rowMapper, identifier);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Volunteer> findByUserId(Long userId) {
        String sql = "SELECT * FROM volunteers WHERE user_id = ?";
        List<Volunteer> list = jdbcTemplate.query(sql, rowMapper, userId);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Volunteer> findAll() {
        String sql = "SELECT * FROM volunteers ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<Volunteer> findByApprovalStatus(VolunteerApprovalStatus status) {
        String sql = "SELECT * FROM volunteers WHERE approval_status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    @Override
    public List<Volunteer> findAvailableVolunteers() {
        String sql = "SELECT * FROM volunteers WHERE approval_status = 'APPROVED' AND availability_status = 'AVAILABLE' ORDER BY experience_years DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public Long save(Volunteer volunteer) {
        String sql = "INSERT INTO volunteers (volunteer_identifier, user_id, full_name, email, phone, operating_area, skills, experience_years, availability_status, approval_status, emergency_contact) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, volunteer.getVolunteerIdentifier());
            if (volunteer.getUserId() != null) {
                ps.setLong(2, volunteer.getUserId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            ps.setString(3, volunteer.getFullName());
            ps.setString(4, volunteer.getEmail());
            ps.setString(5, volunteer.getPhone());
            ps.setString(6, volunteer.getOperatingArea());
            ps.setString(7, volunteer.getSkills());
            ps.setInt(8, volunteer.getExperienceYears());
            ps.setString(9, volunteer.getAvailabilityStatus().name());
            ps.setString(10, volunteer.getApprovalStatus().name());
            ps.setString(11, volunteer.getEmergencyContact());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            volunteer.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public void update(Volunteer volunteer) {
        String sql = "UPDATE volunteers SET full_name = ?, email = ?, phone = ?, operating_area = ?, skills = ?, experience_years = ?, emergency_contact = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                volunteer.getFullName(),
                volunteer.getEmail(),
                volunteer.getPhone(),
                volunteer.getOperatingArea(),
                volunteer.getSkills(),
                volunteer.getExperienceYears(),
                volunteer.getEmergencyContact(),
                volunteer.getId()
        );
    }

    @Override
    public void updateApprovalStatus(Long id, VolunteerApprovalStatus status) {
        String sql = "UPDATE volunteers SET approval_status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    @Override
    public void updateAvailabilityStatus(Long id, VolunteerAvailability status) {
        String sql = "UPDATE volunteers SET availability_status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM volunteers WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM volunteers";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public int countPendingApproval() {
        String sql = "SELECT COUNT(*) FROM volunteers WHERE approval_status = 'PENDING'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public int countAvailable() {
        String sql = "SELECT COUNT(*) FROM volunteers WHERE approval_status = 'APPROVED' AND availability_status = 'AVAILABLE'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public String generateNextIdentifier() {
        String sql = "SELECT MAX(id) FROM volunteers";
        Long maxId = jdbcTemplate.queryForObject(sql, Long.class);
        long next = (maxId != null ? maxId : 0) + 1;
        return String.format("VOL-%04d", next);
    }
}
