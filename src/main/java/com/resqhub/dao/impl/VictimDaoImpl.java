package com.resqhub.dao.impl;

import com.resqhub.dao.VictimDao;
import com.resqhub.model.RescueStatus;
import com.resqhub.model.Victim;
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
public class VictimDaoImpl implements VictimDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Victim> victimRowMapper = (rs, rowNum) -> {
        Victim v = new Victim();
        v.setId(rs.getLong("id"));
        v.setReferenceNumber(rs.getString("reference_number"));
        v.setDisasterId(rs.getLong("disaster_id"));
        v.setFullName(rs.getString("full_name"));
        
        int age = rs.getInt("age");
        if (!rs.wasNull()) {
            v.setAge(age);
        }
        v.setContactNumber(rs.getString("contact_number"));
        v.setLocation(rs.getString("location"));
        v.setAffectedFamilyMembers(rs.getInt("affected_family_members"));
        v.setAccessibilityNeeds(rs.getString("accessibility_needs"));
        v.setCurrentSituation(rs.getString("current_situation"));
        v.setRescueStatus(RescueStatus.valueOf(rs.getString("rescue_status")));
        v.setPriority(VictimPriority.valueOf(rs.getString("priority")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            v.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            v.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        try {
            v.setDisasterName(rs.getString("disaster_name"));
        } catch (Exception ignored) {
        }

        return v;
    };

    @Autowired
    public VictimDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Victim> findById(Long id) {
        String sql = "SELECT v.*, d.name AS disaster_name FROM victims v " +
                     "JOIN disasters d ON v.disaster_id = d.id " +
                     "WHERE v.id = ?";
        try {
            Victim victim = jdbcTemplate.queryForObject(sql, victimRowMapper, id);
            return Optional.ofNullable(victim);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Victim> findByReferenceNumber(String referenceNumber) {
        String sql = "SELECT v.*, d.name AS disaster_name FROM victims v " +
                     "JOIN disasters d ON v.disaster_id = d.id " +
                     "WHERE v.reference_number = ?";
        try {
            Victim victim = jdbcTemplate.queryForObject(sql, victimRowMapper, referenceNumber);
            return Optional.ofNullable(victim);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Long save(Victim victim) {
        String sql = "INSERT INTO victims (reference_number, disaster_id, full_name, age, contact_number, " +
                     "location, affected_family_members, accessibility_needs, current_situation, rescue_status, priority) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, victim.getReferenceNumber());
            ps.setLong(2, victim.getDisasterId());
            ps.setString(3, victim.getFullName());
            if (victim.getAge() != null) {
                ps.setInt(4, victim.getAge());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.setString(5, victim.getContactNumber());
            ps.setString(6, victim.getLocation());
            ps.setInt(7, victim.getAffectedFamilyMembers());
            ps.setString(8, victim.getAccessibilityNeeds());
            ps.setString(9, victim.getCurrentSituation());
            ps.setString(10, victim.getRescueStatus().name());
            ps.setString(11, victim.getPriority().name());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            return keyHolder.getKey().longValue();
        }
        return null;
    }

    @Override
    public void update(Victim victim) {
        String sql = "UPDATE victims SET disaster_id = ?, full_name = ?, age = ?, contact_number = ?, " +
                     "location = ?, affected_family_members = ?, accessibility_needs = ?, current_situation = ?, " +
                     "rescue_status = ?, priority = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                victim.getDisasterId(),
                victim.getFullName(),
                victim.getAge(),
                victim.getContactNumber(),
                victim.getLocation(),
                victim.getAffectedFamilyMembers(),
                victim.getAccessibilityNeeds(),
                victim.getCurrentSituation(),
                victim.getRescueStatus().name(),
                victim.getPriority().name(),
                victim.getId());
    }

    @Override
    public void updateStatus(Long id, RescueStatus status) {
        String sql = "UPDATE victims SET rescue_status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    @Override
    public List<Victim> findAll(int page, int size, String search, Long disasterId, String status, String priority) {
        StringBuilder sql = new StringBuilder(
                "SELECT v.*, d.name AS disaster_name FROM victims v " +
                "JOIN disasters d ON v.disaster_id = d.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (v.full_name LIKE ? OR v.reference_number LIKE ? OR v.location LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (disasterId != null && disasterId > 0) {
            sql.append("AND v.disaster_id = ? ");
            params.add(disasterId);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND v.rescue_status = ? ");
            params.add(status.trim());
        }

        if (priority != null && !priority.trim().isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append("AND v.priority = ? ");
            params.add(priority.trim());
        }

        sql.append("ORDER BY (v.rescue_status = 'AWAITING_RESCUE') DESC, v.created_at DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.query(sql.toString(), victimRowMapper, params.toArray());
    }

    @Override
    public int count(String search, Long disasterId, String status, String priority) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM victims v WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (v.full_name LIKE ? OR v.reference_number LIKE ? OR v.location LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (disasterId != null && disasterId > 0) {
            sql.append("AND v.disaster_id = ? ");
            params.add(disasterId);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND v.rescue_status = ? ");
            params.add(status.trim());
        }

        if (priority != null && !priority.trim().isEmpty() && !"ALL".equalsIgnoreCase(priority)) {
            sql.append("AND v.priority = ? ");
            params.add(priority.trim());
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    @Override
    public List<Victim> findAwaitingRescue(Long disasterId) {
        String sql = "SELECT v.*, d.name AS disaster_name FROM victims v " +
                     "JOIN disasters d ON v.disaster_id = d.id " +
                     "WHERE v.rescue_status = 'AWAITING_RESCUE' ";
        if (disasterId != null && disasterId > 0) {
            sql += "AND v.disaster_id = ? ORDER BY (v.priority = 'CRITICAL') DESC, (v.priority = 'HIGH') DESC, v.created_at ASC";
            return jdbcTemplate.query(sql, victimRowMapper, disasterId);
        } else {
            sql += "ORDER BY (v.priority = 'CRITICAL') DESC, (v.priority = 'HIGH') DESC, v.created_at ASC";
            return jdbcTemplate.query(sql, victimRowMapper);
        }
    }

    @Override
    public int getNextSequence() {
        String currentYearPrefix = "VIC-" + Year.now().getValue() + "-%";
        String sql = "SELECT COUNT(*) FROM victims WHERE reference_number LIKE ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, currentYearPrefix);
        return (count != null ? count : 0) + 1;
    }
}
