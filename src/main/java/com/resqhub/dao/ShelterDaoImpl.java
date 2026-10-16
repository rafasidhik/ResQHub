package com.resqhub.dao;

import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class ShelterDaoImpl implements ShelterDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ShelterDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Shelter> rowMapper = (rs, rowNum) -> {
        Shelter s = new Shelter();
        s.setId(rs.getLong("id"));
        s.setShelterIdentifier(rs.getString("shelter_identifier"));
        s.setName(rs.getString("name"));
        s.setAddress(rs.getString("address"));
        s.setLocationDescription(rs.getString("location_description"));
        s.setContactPerson(rs.getString("contact_person"));
        s.setContactNumber(rs.getString("contact_number"));
        s.setTotalCapacity(rs.getInt("total_capacity"));
        s.setCurrentOccupancy(rs.getInt("current_occupancy"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            s.setStatus(ShelterStatus.valueOf(statusStr));
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            s.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            s.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        return s;
    };

    @Override
    public Optional<Shelter> findById(Long id) {
        String sql = "SELECT * FROM shelters WHERE id = ?";
        List<Shelter> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Shelter> findByIdentifier(String identifier) {
        String sql = "SELECT * FROM shelters WHERE shelter_identifier = ?";
        List<Shelter> list = jdbcTemplate.query(sql, rowMapper, identifier);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Shelter> findAll() {
        String sql = "SELECT * FROM shelters ORDER BY id ASC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<Shelter> findByStatus(ShelterStatus status) {
        String sql = "SELECT * FROM shelters WHERE status = ? ORDER BY id ASC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    @Override
    public List<Shelter> findAvailableShelters() {
        String sql = "SELECT * FROM shelters WHERE status IN ('AVAILABLE', 'NEARLY_FULL') AND current_occupancy < total_capacity ORDER BY (total_capacity - current_occupancy) DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public Long save(Shelter shelter) {
        String sql = "INSERT INTO shelters (shelter_identifier, name, address, location_description, contact_person, contact_number, total_capacity, current_occupancy, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, shelter.getShelterIdentifier());
            ps.setString(2, shelter.getName());
            ps.setString(3, shelter.getAddress());
            ps.setString(4, shelter.getLocationDescription());
            ps.setString(5, shelter.getContactPerson());
            ps.setString(6, shelter.getContactNumber());
            ps.setInt(7, shelter.getTotalCapacity());
            ps.setInt(8, shelter.getCurrentOccupancy());
            ps.setString(9, shelter.getStatus().name());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            shelter.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public void update(Shelter shelter) {
        String sql = "UPDATE shelters SET name = ?, address = ?, location_description = ?, contact_person = ?, contact_number = ?, total_capacity = ?, current_occupancy = ?, status = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                shelter.getName(),
                shelter.getAddress(),
                shelter.getLocationDescription(),
                shelter.getContactPerson(),
                shelter.getContactNumber(),
                shelter.getTotalCapacity(),
                shelter.getCurrentOccupancy(),
                shelter.getStatus().name(),
                shelter.getId()
        );
    }

    @Override
    public void updateOccupancy(Long shelterId, int newOccupancy, ShelterStatus newStatus) {
        String sql = "UPDATE shelters SET current_occupancy = ?, status = ? WHERE id = ?";
        jdbcTemplate.update(sql, newOccupancy, newStatus.name(), shelterId);
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM shelters WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM shelters";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public int getTotalCapacitySum() {
        String sql = "SELECT COALESCE(SUM(total_capacity), 0) FROM shelters";
        Integer sum = jdbcTemplate.queryForObject(sql, Integer.class);
        return sum != null ? sum : 0;
    }

    @Override
    public int getCurrentOccupancySum() {
        String sql = "SELECT COALESCE(SUM(current_occupancy), 0) FROM shelters";
        Integer sum = jdbcTemplate.queryForObject(sql, Integer.class);
        return sum != null ? sum : 0;
    }

    @Override
    public String generateNextIdentifier() {
        String sql = "SELECT MAX(id) FROM shelters";
        Long maxId = jdbcTemplate.queryForObject(sql, Long.class);
        long next = (maxId != null ? maxId : 0) + 1;
        return String.format("SHT-%04d", next);
    }
}
