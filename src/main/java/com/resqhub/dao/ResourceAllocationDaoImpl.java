package com.resqhub.dao;

import com.resqhub.model.ResourceAllocation;
import com.resqhub.model.ResourceAllocationStatus;
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
public class ResourceAllocationDaoImpl implements ResourceAllocationDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public ResourceAllocationDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<ResourceAllocation> rowMapper = (rs, rowNum) -> {
        ResourceAllocation ra = new ResourceAllocation();
        ra.setId(rs.getLong("id"));
        ra.setItemId(rs.getLong("item_id"));
        ra.setQuantity(rs.getInt("quantity"));
        ra.setDisasterId(rs.getLong("disaster_id"));
        
        long shelterId = rs.getLong("shelter_id");
        if (!rs.wasNull()) {
            ra.setShelterId(shelterId);
        }
        
        ra.setDestinationLocation(rs.getString("destination_location"));
        ra.setPurpose(rs.getString("purpose"));
        ra.setResponsiblePerson(rs.getString("responsible_person"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            ra.setStatus(ResourceAllocationStatus.valueOf(statusStr));
        }
        
        ra.setAllocatedByUserId(rs.getLong("allocated_by_user_id"));
        ra.setRemarks(rs.getString("remarks"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            ra.setCreatedAt(createdAt.toLocalDateTime());
        }
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            ra.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        try {
            ra.setItemName(rs.getString("item_name"));
        } catch (Exception ignored) {}
        try {
            ra.setDisasterName(rs.getString("disaster_name"));
        } catch (Exception ignored) {}
        try {
            ra.setShelterName(rs.getString("shelter_name"));
        } catch (Exception ignored) {}
        try {
            ra.setAllocatedByUsername(rs.getString("allocated_by_username"));
        } catch (Exception ignored) {}

        return ra;
    };

    private static final String BASE_SELECT =
            "SELECT ra.*, i.item_name, d.name AS disaster_name, s.name AS shelter_name, u.username AS allocated_by_username " +
            "FROM resource_allocations ra " +
            "JOIN inventory_items i ON ra.item_id = i.id " +
            "JOIN disasters d ON ra.disaster_id = d.id " +
            "LEFT JOIN shelters s ON ra.shelter_id = s.id " +
            "JOIN users u ON ra.allocated_by_user_id = u.id ";

    @Override
    public Optional<ResourceAllocation> findById(Long id) {
        String sql = BASE_SELECT + "WHERE ra.id = ?";
        List<ResourceAllocation> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<ResourceAllocation> findAll() {
        String sql = BASE_SELECT + "ORDER BY ra.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<ResourceAllocation> findByDisasterId(Long disasterId) {
        String sql = BASE_SELECT + "WHERE ra.disaster_id = ? ORDER BY ra.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, disasterId);
    }

    @Override
    public List<ResourceAllocation> findByShelterId(Long shelterId) {
        String sql = BASE_SELECT + "WHERE ra.shelter_id = ? ORDER BY ra.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, shelterId);
    }

    @Override
    public List<ResourceAllocation> findByStatus(ResourceAllocationStatus status) {
        String sql = BASE_SELECT + "WHERE ra.status = ? ORDER BY ra.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    @Override
    public Long save(ResourceAllocation allocation) {
        String sql = "INSERT INTO resource_allocations (item_id, quantity, disaster_id, shelter_id, destination_location, purpose, responsible_person, status, allocated_by_user_id, remarks) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, allocation.getItemId());
            ps.setInt(2, allocation.getQuantity());
            ps.setLong(3, allocation.getDisasterId());
            if (allocation.getShelterId() != null) {
                ps.setLong(4, allocation.getShelterId());
            } else {
                ps.setNull(4, java.sql.Types.BIGINT);
            }
            ps.setString(5, allocation.getDestinationLocation());
            ps.setString(6, allocation.getPurpose());
            ps.setString(7, allocation.getResponsiblePerson());
            ps.setString(8, allocation.getStatus().name());
            ps.setLong(9, allocation.getAllocatedByUserId());
            ps.setString(10, allocation.getRemarks());
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
    public void updateStatus(Long id, ResourceAllocationStatus status) {
        String sql = "UPDATE resource_allocations SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM resource_allocations";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
