package com.resqhub.dao;

import com.resqhub.model.ResidentStatus;
import com.resqhub.model.ShelterResident;
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
public class ShelterResidentDaoImpl implements ShelterResidentDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ShelterResidentDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ShelterResident> rowMapper = (rs, rowNum) -> {
        ShelterResident sr = new ShelterResident();
        sr.setId(rs.getLong("id"));
        sr.setShelterId(rs.getLong("shelter_id"));
        
        long victimId = rs.getLong("victim_id");
        if (!rs.wasNull()) {
            sr.setVictimId(victimId);
        }
        
        sr.setDisasterId(rs.getLong("disaster_id"));
        sr.setResidentName(rs.getString("resident_name"));
        sr.setFamilySize(rs.getInt("family_size"));
        
        Timestamp checkIn = rs.getTimestamp("check_in_time");
        if (checkIn != null) {
            sr.setCheckInTime(checkIn.toLocalDateTime());
        }
        
        Timestamp checkOut = rs.getTimestamp("check_out_time");
        if (checkOut != null) {
            sr.setCheckOutTime(checkOut.toLocalDateTime());
        }
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            sr.setStatus(ResidentStatus.valueOf(statusStr));
        }
        
        sr.setRegisteredByUserId(rs.getLong("registered_by_user_id"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            sr.setCreatedAt(createdAt.toLocalDateTime());
        }

        // Optional JOIN fields
        try {
            sr.setShelterName(rs.getString("shelter_name"));
        } catch (Exception ignored) {}
        try {
            sr.setDisasterName(rs.getString("disaster_name"));
        } catch (Exception ignored) {}
        try {
            sr.setVictimReferenceNumber(rs.getString("victim_reference_number"));
        } catch (Exception ignored) {}
        try {
            sr.setRegisteredByUsername(rs.getString("registered_by_username"));
        } catch (Exception ignored) {}

        return sr;
    };

    private static final String BASE_SELECT = 
            "SELECT sr.*, s.name AS shelter_name, d.name AS disaster_name, " +
            "v.reference_number AS victim_reference_number, u.username AS registered_by_username " +
            "FROM shelter_residents sr " +
            "LEFT JOIN shelters s ON sr.shelter_id = s.id " +
            "LEFT JOIN disasters d ON sr.disaster_id = d.id " +
            "LEFT JOIN victims v ON sr.victim_id = v.id " +
            "LEFT JOIN users u ON sr.registered_by_user_id = u.id ";

    @Override
    public Optional<ShelterResident> findById(Long id) {
        String sql = BASE_SELECT + "WHERE sr.id = ?";
        List<ShelterResident> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<ShelterResident> findByShelterId(Long shelterId) {
        String sql = BASE_SELECT + "WHERE sr.shelter_id = ? ORDER BY sr.status ASC, sr.check_in_time DESC";
        return jdbcTemplate.query(sql, rowMapper, shelterId);
    }

    @Override
    public List<ShelterResident> findActiveByShelterId(Long shelterId) {
        String sql = BASE_SELECT + "WHERE sr.shelter_id = ? AND sr.status = 'ACTIVE' ORDER BY sr.check_in_time DESC";
        return jdbcTemplate.query(sql, rowMapper, shelterId);
    }

    @Override
    public List<ShelterResident> findByDisasterId(Long disasterId) {
        String sql = BASE_SELECT + "WHERE sr.disaster_id = ? ORDER BY sr.check_in_time DESC";
        return jdbcTemplate.query(sql, rowMapper, disasterId);
    }

    @Override
    public List<ShelterResident> findAll() {
        String sql = BASE_SELECT + "ORDER BY sr.check_in_time DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public Long save(ShelterResident resident) {
        String sql = "INSERT INTO shelter_residents (shelter_id, victim_id, disaster_id, resident_name, family_size, check_in_time, status, registered_by_user_id) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), 'ACTIVE', ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, resident.getShelterId());
            if (resident.getVictimId() != null) {
                ps.setLong(2, resident.getVictimId());
            } else {
                ps.setNull(2, java.sql.Types.BIGINT);
            }
            ps.setLong(3, resident.getDisasterId());
            ps.setString(4, resident.getResidentName());
            ps.setInt(5, resident.getFamilySize());
            ps.setLong(6, resident.getRegisteredByUserId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            resident.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public void checkOut(Long residentId) {
        String sql = "UPDATE shelter_residents SET status = 'CHECKED_OUT', check_out_time = NOW() WHERE id = ?";
        jdbcTemplate.update(sql, residentId);
    }

    @Override
    public int countActiveResidentsByShelterId(Long shelterId) {
        String sql = "SELECT COUNT(*) FROM shelter_residents WHERE shelter_id = ? AND status = 'ACTIVE'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, shelterId);
        return count != null ? count : 0;
    }

    @Override
    public int sumActiveFamilySizeByShelterId(Long shelterId) {
        String sql = "SELECT COALESCE(SUM(family_size), 0) FROM shelter_residents WHERE shelter_id = ? AND status = 'ACTIVE'";
        Integer sum = jdbcTemplate.queryForObject(sql, Integer.class, shelterId);
        return sum != null ? sum : 0;
    }
}
