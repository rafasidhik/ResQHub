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
import com.resqhub.model.StockMovement;
import com.resqhub.model.StockMovementType;

/** JDBC data access for the stock_movements history table. */
public class StockMovementDAO extends BaseDao {

    public StockMovement save(StockMovement m) throws DataAccessException {
        String sql = "INSERT INTO stock_movements (resource_id, movement_type, "
                + "quantity, previous_quantity, new_quantity, source, destination, "
                + "reason, disaster_id, moved_at, recorded_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, m.getResourceId());
            ps.setString(2, enumOrNull(m.getType()));
            ps.setInt(3, m.getQuantity());
            ps.setInt(4, m.getPreviousQuantity());
            ps.setInt(5, m.getNewQuantity());
            ps.setString(6, m.getSource());
            ps.setString(7, m.getDestination());
            ps.setString(8, m.getReason());
            bindNullableLong(ps, 9, m.getDisasterId());
            bindLocalDateTime(ps, 10, m.getMovedAt() == null
                    ? LocalDateTime.now() : m.getMovedAt());
            bindNullableLong(ps, 11, m.getRecordedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for stock movement");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save stock movement", e);
        }
    }

    public StockMovement findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM stock_movements WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load stock movement " + id, e);
        }
    }

    public List<StockMovement> findByResource(long resourceId)
            throws DataAccessException {
        String sql = "SELECT * FROM stock_movements WHERE resource_id = ? "
                + "ORDER BY moved_at DESC";
        return query(sql, List.of(resourceId));
    }

    public List<StockMovement> findAll() throws DataAccessException {
        String sql = "SELECT * FROM stock_movements ORDER BY moved_at DESC";
        return query(sql, List.of());
    }

    public List<StockMovement> findByType(StockMovementType type)
            throws DataAccessException {
        String sql = "SELECT * FROM stock_movements WHERE movement_type = ? "
                + "ORDER BY moved_at DESC";
        return query(sql, List.of(type.name()));
    }

    public List<StockMovement> findByDisaster(long disasterId)
            throws DataAccessException {
        String sql = "SELECT * FROM stock_movements WHERE disaster_id = ? "
                + "ORDER BY moved_at DESC";
        return query(sql, List.of(disasterId));
    }

    private List<StockMovement> query(String sql, List<Object> params)
            throws DataAccessException {
        List<StockMovement> result = new ArrayList<>();
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
            throw new DataAccessException("Stock movement query failed", e);
        }
    }

    private StockMovement mapRow(ResultSet rs) throws SQLException {
        StockMovement m = new StockMovement();
        m.setId(rs.getLong("id"));
        m.setResourceId(rs.getLong("resource_id"));
        m.setType(readEnum(StockMovementType.class,
                rs.getString("movement_type")));
        m.setQuantity(rs.getInt("quantity"));
        m.setPreviousQuantity(rs.getInt("previous_quantity"));
        m.setNewQuantity(rs.getInt("new_quantity"));
        m.setSource(rs.getString("source"));
        m.setDestination(rs.getString("destination"));
        m.setReason(rs.getString("reason"));
        m.setDisasterId(getObjectOrNull(rs, "disaster_id"));
        m.setMovedAt(readLocalDateTime(rs, "moved_at"));
        m.setRecordedBy(getObjectOrNull(rs, "recorded_by"));
        m.setCreatedAt(readLocalDateTime(rs, "created_at"));
        m.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return m;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
