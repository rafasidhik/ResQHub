package com.resqhub.dao.impl;

import com.resqhub.dao.DisasterDao;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.model.Severity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class DisasterDaoImpl implements DisasterDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Disaster> disasterRowMapper = (rs, rowNum) -> {
        Disaster d = new Disaster();
        d.setId(rs.getLong("id"));
        d.setName(rs.getString("name"));
        d.setDisasterType(DisasterType.valueOf(rs.getString("disaster_type")));
        d.setDescription(rs.getString("description"));
        d.setLocation(rs.getString("location"));
        d.setSeverity(Severity.valueOf(rs.getString("severity")));
        d.setStatus(DisasterStatus.valueOf(rs.getString("status")));
        
        Date startDate = rs.getDate("start_date");
        if (startDate != null) {
            d.setStartDate(startDate.toLocalDate());
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            d.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            d.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return d;
    };

    @Autowired
    public DisasterDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Disaster> findById(Long id) {
        String sql = "SELECT * FROM disasters WHERE id = ?";
        try {
            Disaster disaster = jdbcTemplate.queryForObject(sql, disasterRowMapper, id);
            if (disaster != null) {
                disaster.setVictimCount(countRelatedVictims(id));
                disaster.setRescueRequestCount(countRelatedRequests(id));
                disaster.setShelterResidentCount(countRelatedShelterResidents(id));
            }
            return Optional.ofNullable(disaster);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Long save(Disaster disaster) {
        String sql = "INSERT INTO disasters (name, disaster_type, description, location, severity, status, start_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, disaster.getName());
            ps.setString(2, disaster.getDisasterType().name());
            ps.setString(3, disaster.getDescription());
            ps.setString(4, disaster.getLocation());
            ps.setString(5, disaster.getSeverity().name());
            ps.setString(6, disaster.getStatus().name());
            ps.setDate(7, Date.valueOf(disaster.getStartDate()));
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            return keyHolder.getKey().longValue();
        }
        return null;
    }

    @Override
    public void update(Disaster disaster) {
        String sql = "UPDATE disasters SET name = ?, disaster_type = ?, description = ?, location = ?, " +
                     "severity = ?, status = ?, start_date = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                disaster.getName(),
                disaster.getDisasterType().name(),
                disaster.getDescription(),
                disaster.getLocation(),
                disaster.getSeverity().name(),
                disaster.getStatus().name(),
                Date.valueOf(disaster.getStartDate()),
                disaster.getId());
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM disasters WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public List<Disaster> findAll(int page, int size, String search, String status, String severity, String type) {
        StringBuilder sql = new StringBuilder("SELECT d.*, " +
                "(SELECT COUNT(*) FROM victims v WHERE v.disaster_id = d.id) AS victim_count, " +
                "(SELECT COUNT(*) FROM rescue_requests r WHERE r.disaster_id = d.id) AS request_count " +
                "FROM disasters d WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (d.name LIKE ? OR d.location LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND d.status = ? ");
            params.add(status.trim());
        }

        if (severity != null && !severity.trim().isEmpty() && !"ALL".equalsIgnoreCase(severity)) {
            sql.append("AND d.severity = ? ");
            params.add(severity.trim());
        }

        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            sql.append("AND d.disaster_type = ? ");
            params.add(type.trim());
        }

        sql.append("ORDER BY (d.status = 'ACTIVE') DESC, d.start_date DESC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            Disaster d = disasterRowMapper.mapRow(rs, rowNum);
            if (d != null) {
                d.setVictimCount(rs.getInt("victim_count"));
                d.setRescueRequestCount(rs.getInt("request_count"));
            }
            return d;
        }, params.toArray());
    }

    @Override
    public int count(String search, String status, String severity, String type) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM disasters WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (name LIKE ? OR location LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND status = ? ");
            params.add(status.trim());
        }

        if (severity != null && !severity.trim().isEmpty() && !"ALL".equalsIgnoreCase(severity)) {
            sql.append("AND severity = ? ");
            params.add(severity.trim());
        }

        if (type != null && !type.trim().isEmpty() && !"ALL".equalsIgnoreCase(type)) {
            sql.append("AND disaster_type = ? ");
            params.add(type.trim());
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    @Override
    public List<Disaster> findActive() {
        String sql = "SELECT * FROM disasters WHERE status = 'ACTIVE' ORDER BY start_date DESC";
        return jdbcTemplate.query(sql, disasterRowMapper);
    }

    @Override
    public int countRelatedVictims(Long disasterId) {
        String sql = "SELECT COUNT(*) FROM victims WHERE disaster_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, disasterId);
        return count != null ? count : 0;
    }

    @Override
    public int countRelatedRequests(Long disasterId) {
        String sql = "SELECT COUNT(*) FROM rescue_requests WHERE disaster_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, disasterId);
        return count != null ? count : 0;
    }

    @Override
    public int countRelatedShelterResidents(Long disasterId) {
        String sql = "SELECT COUNT(*) FROM shelter_residents WHERE disaster_id = ? AND status = 'ACTIVE'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, disasterId);
        return count != null ? count : 0;
    }

    @Override
    public int countRelatedResourceAllocations(Long disasterId) {
        String sql = "SELECT COUNT(*) FROM resource_allocations WHERE disaster_id = ? AND status != 'CANCELLED'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, disasterId);
        return count != null ? count : 0;
    }
}
