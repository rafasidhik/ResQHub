package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.HospitalCapacityLog;

/**
 * JDBC data access for the hospital_capacity_logs table - append-only
 * history of every hospital bed-capacity change (spec section 19).
 */
public class HospitalCapacityLogDAO extends BaseDao
        implements Repository<HospitalCapacityLog> {

    @Override
    public HospitalCapacityLog save(HospitalCapacityLog log)
            throws DataAccessException {
        if (log.getId() == null) {
            return insert(log);
        }
        return update(log);
    }

    private HospitalCapacityLog insert(HospitalCapacityLog log)
            throws DataAccessException {
        String sql = "INSERT INTO hospital_capacity_logs (hospital_id, "
                + "previous_occupied, updated_occupied, available_beds, "
                + "reason, changed_by, changed_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            bindNullableLong(ps, 1, log.getHospitalId());
            ps.setInt(2, log.getPreviousOccupied());
            ps.setInt(3, log.getUpdatedOccupied());
            ps.setInt(4, log.getAvailableBeds());
            ps.setString(5, log.getReason());
            bindNullableLong(ps, 6, log.getChangedBy());
            bindLocalDateTime(ps, 7, log.getChangedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for capacity log");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save hospital capacity log", e);
        }
    }

    private HospitalCapacityLog update(HospitalCapacityLog log)
            throws DataAccessException {
        String sql = "UPDATE hospital_capacity_logs SET hospital_id = ?, "
                + "previous_occupied = ?, updated_occupied = ?, "
                + "available_beds = ?, reason = ?, changed_by = ?, "
                + "changed_at = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindNullableLong(ps, 1, log.getHospitalId());
            ps.setInt(2, log.getPreviousOccupied());
            ps.setInt(3, log.getUpdatedOccupied());
            ps.setInt(4, log.getAvailableBeds());
            ps.setString(5, log.getReason());
            bindNullableLong(ps, 6, log.getChangedBy());
            bindLocalDateTime(ps, 7, log.getChangedAt());
            ps.setLong(8, log.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Capacity log update affected " + rows + " rows "
                                + "for id " + log.getId());
            }
            return findById(log.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update hospital capacity log " + log.getId(), e);
        }
    }

    @Override
    public HospitalCapacityLog findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM hospital_capacity_logs WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load hospital capacity log " + id, e);
        }
    }

    @Override
    public List<HospitalCapacityLog> findAll() throws DataAccessException {
        String sql = "SELECT * FROM hospital_capacity_logs "
                + "ORDER BY changed_at DESC, id DESC";
        return query(sql, List.of());
    }

    public List<HospitalCapacityLog> findByHospital(long hospitalId)
            throws DataAccessException {
        String sql = "SELECT * FROM hospital_capacity_logs "
                + "WHERE hospital_id = ? ORDER BY changed_at DESC, id DESC";
        return query(sql, List.of(hospitalId));
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM hospital_capacity_logs WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete hospital capacity log " + id, e);
        }
    }

    private List<HospitalCapacityLog> query(String sql, List<Object> params)
            throws DataAccessException {
        List<HospitalCapacityLog> result = new ArrayList<>();
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
            throw new DataAccessException(
                    "Hospital capacity log query failed", e);
        }
    }

    private void bindParam(PreparedStatement ps, int index, Object value)
            throws SQLException {
        if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else {
            ps.setString(index, String.valueOf(value));
        }
    }

    private HospitalCapacityLog mapRow(ResultSet rs) throws SQLException {
        HospitalCapacityLog log = new HospitalCapacityLog();
        log.setId(rs.getLong("id"));
        log.setHospitalId(getObjectOrNull(rs, "hospital_id"));
        log.setPreviousOccupied(rs.getInt("previous_occupied"));
        log.setUpdatedOccupied(rs.getInt("updated_occupied"));
        log.setAvailableBeds(rs.getInt("available_beds"));
        log.setReason(rs.getString("reason"));
        log.setChangedBy(getObjectOrNull(rs, "changed_by"));
        log.setChangedAt(readLocalDateTime(rs, "changed_at"));
        log.setCreatedAt(readLocalDateTime(rs, "created_at"));
        log.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return log;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
