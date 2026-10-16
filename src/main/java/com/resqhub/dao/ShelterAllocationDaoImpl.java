package com.resqhub.dao;

import com.resqhub.model.AllocationStatus;
import com.resqhub.model.ShelterAllocation;
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
public class ShelterAllocationDaoImpl implements ShelterAllocationDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ShelterAllocationDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ShelterAllocation> rowMapper = (rs, rowNum) -> {
        ShelterAllocation sa = new ShelterAllocation();
        sa.setId(rs.getLong("id"));
        sa.setShelterId(rs.getLong("shelter_id"));
        sa.setDisasterId(rs.getLong("disaster_id"));
        sa.setRequestedPeopleCount(rs.getInt("requested_people_count"));
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            sa.setStatus(AllocationStatus.valueOf(statusStr));
        }
        sa.setRecommendationReason(rs.getString("recommendation_reason"));
        long decidedById = rs.getLong("decided_by_user_id");
        if (!rs.wasNull()) {
            sa.setDecidedByUserId(decidedById);
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            sa.setCreatedAt(createdAt.toLocalDateTime());
        }

        try {
            sa.setShelterName(rs.getString("shelter_name"));
        } catch (Exception ignored) {}
        try {
            sa.setDisasterName(rs.getString("disaster_name"));
        } catch (Exception ignored) {}
        try {
            sa.setDecidedByUsername(rs.getString("decided_by_username"));
        } catch (Exception ignored) {}

        return sa;
    };

    private static final String BASE_SELECT = 
            "SELECT sa.*, s.name AS shelter_name, d.name AS disaster_name, u.username AS decided_by_username " +
            "FROM shelter_allocations sa " +
            "LEFT JOIN shelters s ON sa.shelter_id = s.id " +
            "LEFT JOIN disasters d ON sa.disaster_id = d.id " +
            "LEFT JOIN users u ON sa.decided_by_user_id = u.id ";

    @Override
    public Optional<ShelterAllocation> findById(Long id) {
        String sql = BASE_SELECT + "WHERE sa.id = ?";
        List<ShelterAllocation> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<ShelterAllocation> findByShelterId(Long shelterId) {
        String sql = BASE_SELECT + "WHERE sa.shelter_id = ? ORDER BY sa.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, shelterId);
    }

    @Override
    public List<ShelterAllocation> findByDisasterId(Long disasterId) {
        String sql = BASE_SELECT + "WHERE sa.disaster_id = ? ORDER BY sa.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, disasterId);
    }

    @Override
    public List<ShelterAllocation> findAll() {
        String sql = BASE_SELECT + "ORDER BY sa.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public Long save(ShelterAllocation allocation) {
        String sql = "INSERT INTO shelter_allocations (shelter_id, disaster_id, requested_people_count, status, recommendation_reason, decided_by_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, allocation.getShelterId());
            ps.setLong(2, allocation.getDisasterId());
            ps.setInt(3, allocation.getRequestedPeopleCount());
            ps.setString(4, allocation.getStatus().name());
            ps.setString(5, allocation.getRecommendationReason());
            if (allocation.getDecidedByUserId() != null) {
                ps.setLong(6, allocation.getDecidedByUserId());
            } else {
                ps.setNull(6, java.sql.Types.BIGINT);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            allocation.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public void updateStatus(Long id, AllocationStatus status, Long decidedByUserId) {
        String sql = "UPDATE shelter_allocations SET status = ?, decided_by_user_id = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), decidedByUserId, id);
    }
}
