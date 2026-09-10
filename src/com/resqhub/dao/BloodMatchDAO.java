package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.BloodMatch;
import com.resqhub.model.BloodMatchStatus;

/**
 * JDBC data access for the blood_matches table (spec: Database Storage).
 * Tracks which donor was offered/confirmed against a blood request and the
 * match's lifecycle. One request may have several proposed donors.
 */
public class BloodMatchDAO extends BaseDao implements Repository<BloodMatch> {

    @Override
    public BloodMatch save(BloodMatch m) throws DataAccessException {
        if (m.getId() == null) {
            return insert(m);
        }
        return update(m);
    }

    private BloodMatch insert(BloodMatch m) throws DataAccessException {
        String sql = "INSERT INTO blood_matches (request_id, donor_id, status, "
                + "units_matched, location_matched, donor_distance_rank, notes, "
                + "matched_by, matched_at, confirmed_at, collected_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            int idx = bindColumns(ps, m, 1);
            bindLocalDateTime(ps, idx, m.getCollectedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for blood match");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save blood match", e);
        }
    }

    private BloodMatch update(BloodMatch m) throws DataAccessException {
        String sql = "UPDATE blood_matches SET request_id = ?, donor_id = ?, "
                + "status = ?, units_matched = ?, location_matched = ?, "
                + "donor_distance_rank = ?, notes = ?, matched_by = ?, "
                + "matched_at = ?, confirmed_at = ?, collected_at = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = bindColumns(ps, m, 1);
            bindLocalDateTime(ps, idx, m.getCollectedAt());
            ps.setLong(idx + 1, m.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Blood match update affected " + rows + " rows for id "
                                + m.getId());
            }
            return findById(m.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update blood match " + m.getId(), e);
        }
    }

    private int bindColumns(PreparedStatement ps, BloodMatch m, int start)
            throws SQLException {
        bindNullableLong(ps, start, m.getRequestId());
        bindNullableLong(ps, start + 1, m.getDonorId());
        ps.setString(start + 2, enumOrNull(m.getStatus()));
        ps.setInt(start + 3, m.getUnitsMatched());
        ps.setBoolean(start + 4, m.isLocationMatched());
        ps.setInt(start + 5, m.getDonorDistanceRank());
        ps.setString(start + 6, m.getNotes());
        bindNullableLong(ps, start + 7, m.getMatchedBy());
        bindLocalDateTime(ps, start + 8, m.getMatchedAt());
        bindLocalDateTime(ps, start + 9, m.getConfirmedAt());
        return start + 10;
    }

    @Override
    public BloodMatch findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM blood_matches WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load blood match " + id, e);
        }
    }

    @Override
    public List<BloodMatch> findAll() throws DataAccessException {
        String sql = "SELECT * FROM blood_matches ORDER BY matched_at DESC, id DESC";
        return query(sql, List.of());
    }

    public List<BloodMatch> findByRequest(long requestId)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_matches WHERE request_id = ? "
                + "ORDER BY donor_distance_rank, matched_at DESC, id DESC";
        return query(sql, List.of(requestId));
    }

    public List<BloodMatch> findByDonor(long donorId)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_matches WHERE donor_id = ? "
                + "ORDER BY matched_at DESC, id DESC";
        return query(sql, List.of(donorId));
    }

    public List<BloodMatch> findByStatus(BloodMatchStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_matches WHERE status = ? "
                + "ORDER BY matched_at DESC, id DESC";
        return query(sql, List.of(enumOrNull(status)));
    }

    public List<BloodMatch> findByRequestAndStatus(long requestId,
            BloodMatchStatus status) throws DataAccessException {
        String sql = "SELECT * FROM blood_matches WHERE request_id = ? "
                + "AND status = ? ORDER BY donor_distance_rank, id";
        return query(sql, List.of(requestId, enumOrNull(status)));
    }

    public BloodMatch findActiveMatchForRequest(long requestId)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_matches WHERE request_id = ? "
                + "AND status IN ('SUGGESTED','CONTACTED','CONFIRMED') "
                + "ORDER BY donor_distance_rank, id LIMIT 1";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load active blood match for request", e);
        }
    }

    /** Total units promised by confirmed/active matches on a request. */
    public int sumActiveUnits(long requestId) throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(units_matched),0) FROM blood_matches "
                + "WHERE request_id = ? AND status IN "
                + "('SUGGESTED','CONTACTED','CONFIRMED')";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not sum matched blood units", e);
        }
    }

    /** Total units actually collected for a request (from collected matches). */
    public int sumCollectedUnits(long requestId) throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(units_matched),0) FROM blood_matches "
                + "WHERE request_id = ? AND status = 'COLLECTED'";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not sum collected blood units", e);
        }
    }

    private List<BloodMatch> query(String sql, List<Object> params)
            throws DataAccessException {
        List<BloodMatch> result = new ArrayList<>();
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
            throw new DataAccessException("Blood match query failed", e);
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

    private BloodMatch mapRow(ResultSet rs) throws SQLException {
        BloodMatch m = new BloodMatch();
        m.setId(rs.getLong("id"));
        m.setRequestId(getObjectOrNull(rs, "request_id"));
        m.setDonorId(getObjectOrNull(rs, "donor_id"));
        m.setStatus(readEnum(BloodMatchStatus.class, rs.getString("status")));
        m.setUnitsMatched(rs.getInt("units_matched"));
        m.setLocationMatched(rs.getBoolean("location_matched"));
        m.setDonorDistanceRank(rs.getInt("donor_distance_rank"));
        m.setNotes(rs.getString("notes"));
        m.setMatchedBy(getObjectOrNull(rs, "matched_by"));
        m.setMatchedAt(readLocalDateTime(rs, "matched_at"));
        m.setConfirmedAt(readLocalDateTime(rs, "confirmed_at"));
        m.setCollectedAt(readLocalDateTime(rs, "collected_at"));
        m.setCreatedAt(readLocalDateTime(rs, "created_at"));
        m.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return m;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM blood_matches WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete blood match " + id, e);
        }
    }
}
