package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.RequestHistory;

/**
 * JDBC data access for the blood_request_history table (spec: Request History).
 * Every event in a blood request's lifecycle is appended for traceability and
 * can be replayed newest-first when viewing a request.
 */
public class RequestHistoryDAO extends BaseDao implements Repository<RequestHistory> {

    @Override
    public RequestHistory save(RequestHistory h) throws DataAccessException {
        String sql = "INSERT INTO blood_request_history (request_id, event, "
                + "details, performed_by, performed_at) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            bindNullableLong(ps, 1, h.getRequestId());
            ps.setString(2, h.getEvent());
            ps.setString(3, h.getRemarks());
            bindNullableLong(ps, 4, h.getPerformedBy());
            bindLocalDateTime(ps, 5, h.getPerformedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for request history");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not record request history event", e);
        }
    }

    @Override
    public RequestHistory findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM blood_request_history WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load request history " + id, e);
        }
    }

    @Override
    public List<RequestHistory> findAll() throws DataAccessException {
        String sql = "SELECT * FROM blood_request_history "
                + "ORDER BY performed_at DESC, id DESC";
        return query(sql, List.of());
    }

    /** Full history for a single blood request, newest first (spec 25). */
    public List<RequestHistory> findByRequest(long requestId)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_request_history WHERE request_id = ? "
                + "ORDER BY performed_at DESC, id DESC";
        return query(sql, List.of(requestId));
    }

    private List<RequestHistory> query(String sql, List<Object> params)
            throws DataAccessException {
        List<RequestHistory> result = new ArrayList<>();
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
            throw new DataAccessException("Request history query failed", e);
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

    private RequestHistory mapRow(ResultSet rs) throws SQLException {
        RequestHistory h = new RequestHistory();
        h.setId(rs.getLong("id"));
        h.setRequestId(getObjectOrNull(rs, "request_id"));
        h.setEvent(rs.getString("event"));
        h.setRemarks(rs.getString("details"));
        h.setPerformedBy(getObjectOrNull(rs, "performed_by"));
        h.setPerformedAt(readLocalDateTime(rs, "performed_at"));
        h.setCreatedAt(readLocalDateTime(rs, "created_at"));
        h.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return h;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM blood_request_history WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete request history " + id, e);
        }
    }
}
