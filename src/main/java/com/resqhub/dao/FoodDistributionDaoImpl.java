package com.resqhub.dao;

import com.resqhub.model.DistributionStatus;
import com.resqhub.model.FoodDistribution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class FoodDistributionDaoImpl implements FoodDistributionDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FoodDistributionDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<FoodDistribution> rowMapper = (rs, rowNum) -> {
        FoodDistribution fd = new FoodDistribution();
        fd.setId(rs.getLong("id"));
        
        long shelterId = rs.getLong("shelter_id");
        if (!rs.wasNull()) {
            fd.setShelterId(shelterId);
        }
        
        fd.setLocation(rs.getString("location"));
        fd.setItemId(rs.getLong("item_id"));
        fd.setQuantity(rs.getInt("quantity"));
        fd.setRecipientCount(rs.getInt("recipient_count"));
        
        Date distDate = rs.getDate("distribution_date");
        if (distDate != null) {
            fd.setDistributionDate(distDate.toLocalDate());
        }
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            fd.setStatus(DistributionStatus.valueOf(statusStr));
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            fd.setCreatedAt(createdAt.toLocalDateTime());
        }

        try {
            fd.setItemName(rs.getString("item_name"));
        } catch (Exception ignored) {}
        try {
            fd.setShelterName(rs.getString("shelter_name"));
        } catch (Exception ignored) {}

        return fd;
    };

    private static final String BASE_SELECT =
            "SELECT fd.*, i.item_name, s.name AS shelter_name " +
            "FROM food_distributions fd " +
            "JOIN inventory_items i ON fd.item_id = i.id " +
            "LEFT JOIN shelters s ON fd.shelter_id = s.id ";

    @Override
    public Optional<FoodDistribution> findById(Long id) {
        String sql = BASE_SELECT + "WHERE fd.id = ?";
        List<FoodDistribution> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<FoodDistribution> findAll() {
        String sql = BASE_SELECT + "ORDER BY fd.distribution_date DESC, fd.id DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<FoodDistribution> findByShelterId(Long shelterId) {
        String sql = BASE_SELECT + "WHERE fd.shelter_id = ? ORDER BY fd.distribution_date DESC";
        return jdbcTemplate.query(sql, rowMapper, shelterId);
    }

    @Override
    public List<FoodDistribution> findByStatus(DistributionStatus status) {
        String sql = BASE_SELECT + "WHERE fd.status = ? ORDER BY fd.distribution_date DESC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    @Override
    public Long save(FoodDistribution distribution) {
        String sql = "INSERT INTO food_distributions (shelter_id, location, item_id, quantity, recipient_count, distribution_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if (distribution.getShelterId() != null) {
                ps.setLong(1, distribution.getShelterId());
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            ps.setString(2, distribution.getLocation());
            ps.setLong(3, distribution.getItemId());
            ps.setInt(4, distribution.getQuantity());
            ps.setInt(5, distribution.getRecipientCount());
            ps.setDate(6, Date.valueOf(distribution.getDistributionDate()));
            ps.setString(7, distribution.getStatus().name());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            distribution.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public void completeDistribution(Long id) {
        String sql = "UPDATE food_distributions SET status = 'COMPLETED' WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM food_distributions";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public int sumRecipientsServed() {
        String sql = "SELECT COALESCE(SUM(recipient_count), 0) FROM food_distributions WHERE status = 'COMPLETED'";
        Integer sum = jdbcTemplate.queryForObject(sql, Integer.class);
        return sum != null ? sum : 0;
    }
}
