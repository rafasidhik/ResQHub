package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.FoodDistribution;

/**
 * JDBC data access for the food_distributions table - the actual
 * distribution events that form the food distribution history
 * (spec section 15) and drive the request's distributed / remaining
 * quantity tracking.
 */
public class FoodDistributionDAO extends BaseDao
        implements Repository<FoodDistribution> {

    @Override
    public FoodDistribution save(FoodDistribution d)
            throws DataAccessException {
        if (d.getId() == null) {
            return insert(d);
        }
        return update(d);
    }

    private FoodDistribution insert(FoodDistribution d)
            throws DataAccessException {
        String sql = "INSERT INTO food_distributions (request_id, resource_id, "
                + "quantity, beneficiaries_served, distributed_to, location, "
                + "distributed_at, distributed_by, note) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, d.getRequestId());
            bindNullableLong(ps, 2, d.getResourceId());
            ps.setInt(3, d.getQuantity());
            ps.setInt(4, d.getBeneficiariesServed());
            ps.setString(5, d.getDistributedTo());
            ps.setString(6, d.getLocation());
            bindLocalDateTime(ps, 7, d.getDistributedAt());
            bindNullableLong(ps, 8, d.getDistributedBy());
            ps.setString(9, d.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for food distribution");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save food distribution", e);
        }
    }

    private FoodDistribution update(FoodDistribution d)
            throws DataAccessException {
        String sql = "UPDATE food_distributions SET request_id = ?, "
                + "resource_id = ?, quantity = ?, beneficiaries_served = ?, "
                + "distributed_to = ?, location = ?, distributed_at = ?, "
                + "distributed_by = ?, note = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, d.getRequestId());
            bindNullableLong(ps, 2, d.getResourceId());
            ps.setInt(3, d.getQuantity());
            ps.setInt(4, d.getBeneficiariesServed());
            ps.setString(5, d.getDistributedTo());
            ps.setString(6, d.getLocation());
            bindLocalDateTime(ps, 7, d.getDistributedAt());
            bindNullableLong(ps, 8, d.getDistributedBy());
            ps.setString(9, d.getNote());
            ps.setLong(10, d.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Food distribution update affected " + rows + " rows "
                                + "for id " + d.getId());
            }
            return findById(d.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update food distribution " + d.getId(), e);
        }
    }

    @Override
    public FoodDistribution findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM food_distributions WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load food distribution " + id, e);
        }
    }

    @Override
    public List<FoodDistribution> findAll() throws DataAccessException {
        String sql = "SELECT * FROM food_distributions "
                + "ORDER BY distributed_at DESC, id DESC";
        return query(sql, List.of());
    }

    public List<FoodDistribution> findByRequest(long requestId)
            throws DataAccessException {
        String sql = "SELECT * FROM food_distributions WHERE request_id = ? "
                + "ORDER BY distributed_at DESC";
        return query(sql, List.of(requestId));
    }

    public List<FoodDistribution> findByLocation(String location)
            throws DataAccessException {
        String sql = "SELECT * FROM food_distributions WHERE LOWER(location) "
                + "LIKE ? ORDER BY distributed_at DESC";
        return query(sql, List.of("%" + location.toLowerCase() + "%"));
    }

    public List<FoodDistribution> findByDisaster(long disasterId)
            throws DataAccessException {
        String sql = "SELECT d.* FROM food_distributions d "
                + "JOIN food_requests r ON r.id = d.request_id "
                + "WHERE r.disaster_id = ? ORDER BY d.distributed_at DESC";
        return query(sql, List.of(disasterId));
    }

    /** Combined filter by request / location / disaster. */
    public List<FoodDistribution> filter(Long requestId, String location,
            Long disasterId) throws DataAccessException {
        StringBuilder sql = new StringBuilder(
                "SELECT d.* FROM food_distributions d WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (requestId != null) {
            sql.append(" AND d.request_id = ?");
            params.add(requestId);
        }
        if (location != null && !location.isEmpty()) {
            sql.append(" AND LOWER(d.location) LIKE ?");
            params.add("%" + location.toLowerCase() + "%");
        }
        if (disasterId != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM food_requests r "
                    + "WHERE r.id = d.request_id AND r.disaster_id = ?)");
            params.add(disasterId);
        }
        sql.append(" ORDER BY d.distributed_at DESC");
        return query(sql.toString(), params);
    }

    /** Total food handed out against a request. */
    public int sumByRequest(long requestId) throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(quantity),0) FROM food_distributions "
                + "WHERE request_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not sum food distribution for request "
                            + requestId, e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM food_distributions WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete food distribution " + id, e);
        }
    }

    private List<FoodDistribution> query(String sql, List<Object> params)
            throws DataAccessException {
        List<FoodDistribution> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                bindParam(ps, i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Food distribution query failed", e);
        }
    }

    private void bindParam(PreparedStatement ps, int index, Object value)
            throws SQLException {
        if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else {
            ps.setString(index, String.valueOf(value));
        }
    }

    private FoodDistribution mapRow(ResultSet rs) throws SQLException {
        FoodDistribution d = new FoodDistribution();
        d.setId(rs.getLong("id"));
        d.setRequestId(rs.getLong("request_id"));
        d.setResourceId(getObjectOrNull(rs, "resource_id"));
        d.setQuantity(rs.getInt("quantity"));
        d.setBeneficiariesServed(rs.getInt("beneficiaries_served"));
        d.setDistributedTo(rs.getString("distributed_to"));
        d.setLocation(rs.getString("location"));
        d.setDistributedAt(readLocalDateTime(rs, "distributed_at"));
        d.setDistributedBy(getObjectOrNull(rs, "distributed_by"));
        d.setNote(rs.getString("note"));
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
