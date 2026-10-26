package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.DistributionDestination;
import com.resqhub.model.ResourceDistribution;

/** JDBC data access for the resource_distributions table. */
public class ResourceDistributionDAO extends BaseDao {

    public ResourceDistribution save(ResourceDistribution d)
            throws DataAccessException {
        String sql = "INSERT INTO resource_distributions (resource_id, quantity, "
                + "distributed_to, destination, disaster_id, shelter_id, "
                + "victim_id, reason, distributed_at, distributed_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, d.getResourceId());
            ps.setInt(2, d.getQuantity());
            ps.setString(3, d.getDistributedTo());
            ps.setString(4, enumOrNull(d.getDestination()));
            bindNullableLong(ps, 5, d.getDisasterId());
            bindNullableLong(ps, 6, d.getShelterId());
            bindNullableLong(ps, 7, d.getVictimId());
            ps.setString(8, d.getReason());
            bindLocalDateTime(ps, 9, d.getDistributedAt() == null
                    ? LocalDateTime.now() : d.getDistributedAt());
            bindNullableLong(ps, 10, d.getDistributedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for resource distribution");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save resource distribution", e);
        }
    }

    public ResourceDistribution findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM resource_distributions WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load resource distribution " + id, e);
        }
    }

    public List<ResourceDistribution> findByResource(long resourceId)
            throws DataAccessException {
        String sql = "SELECT * FROM resource_distributions WHERE resource_id = ? "
                + "ORDER BY distributed_at DESC";
        return query(sql, List.of(resourceId));
    }

    public List<ResourceDistribution> findAll() throws DataAccessException {
        String sql = "SELECT * FROM resource_distributions "
                + "ORDER BY distributed_at DESC";
        return query(sql, List.of());
    }

    public List<ResourceDistribution> findByDestination(
            DistributionDestination destination) throws DataAccessException {
        String sql = "SELECT * FROM resource_distributions WHERE destination = ? "
                + "ORDER BY distributed_at DESC";
        return query(sql, List.of(destination.name()));
    }

    public List<ResourceDistribution> findByDisaster(long disasterId)
            throws DataAccessException {
        String sql = "SELECT * FROM resource_distributions WHERE disaster_id = ? "
                + "ORDER BY distributed_at DESC";
        return query(sql, List.of(disasterId));
    }

    public List<ResourceDistribution> findByShelter(long shelterId)
            throws DataAccessException {
        String sql = "SELECT * FROM resource_distributions WHERE shelter_id = ? "
                + "ORDER BY distributed_at DESC";
        return query(sql, List.of(shelterId));
    }

    public List<ResourceDistribution> findByVictim(long victimId)
            throws DataAccessException {
        String sql = "SELECT * FROM resource_distributions WHERE victim_id = ? "
                + "ORDER BY distributed_at DESC";
        return query(sql, List.of(victimId));
    }

    private List<ResourceDistribution> query(String sql, List<Object> params)
            throws DataAccessException {
        List<ResourceDistribution> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) {
                    ps.setLong(i + 1, (Long) p);
                } else {
                    ps.setString(i + 1, String.valueOf(p));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Resource distribution query failed", e);
        }
    }

    private ResourceDistribution mapRow(ResultSet rs) throws SQLException {
        ResourceDistribution d = new ResourceDistribution();
        d.setId(rs.getLong("id"));
        d.setResourceId(rs.getLong("resource_id"));
        d.setQuantity(rs.getInt("quantity"));
        d.setDistributedTo(rs.getString("distributed_to"));
        d.setDestination(readEnum(DistributionDestination.class,
                rs.getString("destination")));
        d.setDisasterId(getObjectOrNull(rs, "disaster_id"));
        d.setShelterId(getObjectOrNull(rs, "shelter_id"));
        d.setVictimId(getObjectOrNull(rs, "victim_id"));
        d.setReason(rs.getString("reason"));
        d.setDistributedAt(readLocalDateTime(rs, "distributed_at"));
        d.setDistributedBy(getObjectOrNull(rs, "distributed_by"));
        d.setCreatedAt(readLocalDateTime(rs, "created_at"));
        d.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return d;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
