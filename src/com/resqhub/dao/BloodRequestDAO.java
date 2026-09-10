package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.BloodGroup;
import com.resqhub.model.BloodRequest;
import com.resqhub.model.BloodRequestPriority;
import com.resqhub.model.BloodRequestStatus;

/**
 * JDBC data access for the blood_requests table (spec: Database Storage).
 * Creation, status / priority tracking, open-request queries and the
 * shortage / availability calculations.
 */
public class BloodRequestDAO extends BaseDao implements Repository<BloodRequest> {

    @Override
    public BloodRequest save(BloodRequest r) throws DataAccessException {
        if (r.getId() == null) {
            return insert(r);
        }
        return update(r);
    }

    private BloodRequest insert(BloodRequest r) throws DataAccessException {
        String sql = "INSERT INTO blood_requests (request_code, blood_group, "
                + "units_required, location, priority, status, "
                + "emergency_details, hospital_id, victim_id, disaster_id, "
                + "created_by, request_date, required_date, "
                + "fulfilled_by_match_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            int idx = bindColumns(ps, r, 1);
            bindNullableLong(ps, idx, r.getFulfilledByMatchId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for blood request");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save blood request " + r.getRequestCode(), e);
        }
    }

    private BloodRequest update(BloodRequest r) throws DataAccessException {
        String sql = "UPDATE blood_requests SET request_code = ?, "
                + "blood_group = ?, units_required = ?, location = ?, "
                + "priority = ?, status = ?, emergency_details = ?, "
                + "hospital_id = ?, victim_id = ?, disaster_id = ?, "
                + "created_by = ?, request_date = ?, required_date = ?, "
                + "fulfilled_by_match_id = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = bindColumns(ps, r, 1);
            bindNullableLong(ps, idx, r.getFulfilledByMatchId());
            ps.setLong(idx + 1, r.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Blood request update affected " + rows + " rows for id "
                                + r.getId());
            }
            return findById(r.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update blood request " + r.getId(), e);
        }
    }

    private int bindColumns(PreparedStatement ps, BloodRequest r, int start)
            throws SQLException {
        ps.setString(start, r.getRequestCode());
        ps.setString(start + 1, enumOrNull(r.getBloodGroup()));
        ps.setInt(start + 2, r.getUnitsRequired());
        ps.setString(start + 3, r.getLocation());
        ps.setString(start + 4, enumOrNull(r.getPriority()));
        ps.setString(start + 5, enumOrNull(r.getStatus()));
        ps.setString(start + 6, r.getEmergencyDetails());
        bindNullableLong(ps, start + 7, r.getHospitalId());
        bindNullableLong(ps, start + 8, r.getVictimId());
        bindNullableLong(ps, start + 9, r.getRequireForDisaster());
        bindNullableLong(ps, start + 10, r.getCreatedBy());
        bindLocalDateTime(ps, start + 11, r.getRequestDate());
        bindLocalDateTime(ps, start + 12, r.getRequiredDate());
        return start + 13;
    }

    @Override
    public BloodRequest findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM blood_requests WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load blood request " + id, e);
        }
    }

    public BloodRequest findByCode(String code) throws DataAccessException {
        if (code == null) {
            return null;
        }
        String sql = "SELECT * FROM blood_requests WHERE request_code = ? LIMIT 1";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load blood request by code", e);
        }
    }

    @Override
    public List<BloodRequest> findAll() throws DataAccessException {
        String sql = "SELECT * FROM blood_requests ORDER BY "
                + "CASE priority WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 "
                + "WHEN 'MEDIUM' THEN 2 ELSE 3 END, request_date DESC, id DESC";
        return query(sql, List.of());
    }

    public List<BloodRequest> findOpen() throws DataAccessException {
        String sql = "SELECT * FROM blood_requests WHERE status IN ("
                + "'PENDING','MATCHING_DONORS','DONOR_FOUND','BLOOD_COLLECTED') "
                + "ORDER BY "
                + "CASE priority WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 "
                + "WHEN 'MEDIUM' THEN 2 ELSE 3 END, request_date DESC, id DESC";
        return query(sql, List.of());
    }

    public List<BloodRequest> findByStatus(BloodRequestStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_requests WHERE status = ? "
                + "ORDER BY request_date DESC, id DESC";
        return query(sql, List.of(enumOrNull(status)));
    }

    public List<BloodRequest> findByBloodGroup(BloodGroup group)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_requests WHERE blood_group = ? "
                + "ORDER BY request_date DESC, id DESC";
        return query(sql, List.of(enumOrNull(group)));
    }

    public List<BloodRequest> findByPriority(BloodRequestPriority priority)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_requests WHERE priority = ? "
                + "ORDER BY request_date DESC, id DESC";
        return query(sql, List.of(enumOrNull(priority)));
    }

    /** Open requests for a given location (used by shortage checks). */
    public List<BloodRequest> findOpenByLocation(String location)
            throws DataAccessException {
        String like = "%" + (location == null ? "" : location.trim())
                + "%";
        String sql = "SELECT * FROM blood_requests WHERE "
                + "status IN ('PENDING','MATCHING_DONORS','DONOR_FOUND',"
                + "'BLOOD_COLLECTED') AND (LOWER(location) LIKE ? OR "
                + "LOWER(emergency_details) LIKE ?) ORDER BY "
                + "CASE priority WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 "
                + "WHEN 'MEDIUM' THEN 2 ELSE 3 END";
        return query(sql, List.of(like, like));
    }

    /**
     * Filtered search (spec 23: Request Search and Filtering). Accepts any
     * combination of blood group, priority, status, hospital and a free-text
     * keyword that matches code / location / emergency details.
     */
    public List<BloodRequest> search(BloodGroup group,
            BloodRequestPriority priority, BloodRequestStatus status,
            Long hospitalId, String keyword) throws DataAccessException {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM blood_requests WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (group != null) {
            sql.append("AND blood_group = ? ");
            params.add(enumOrNull(group));
        }
        if (priority != null) {
            sql.append("AND priority = ? ");
            params.add(enumOrNull(priority));
        }
        if (status != null) {
            sql.append("AND status = ? ");
            params.add(enumOrNull(status));
        }
        if (hospitalId != null) {
            sql.append("AND hospital_id = ? ");
            params.add(hospitalId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            String like = "%" + keyword.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(request_code) LIKE ? OR "
                    + "LOWER(location) LIKE ? OR "
                    + "LOWER(emergency_details) LIKE ?) ");
            params.add(like);
            params.add(like);
            params.add(like);
        }
        sql.append("ORDER BY "
                + "CASE priority WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 "
                + "WHEN 'MEDIUM' THEN 2 ELSE 3 END, request_date DESC, id DESC");
        return query(sql.toString(), params);
    }

    private List<BloodRequest> query(String sql, List<Object> params)
            throws DataAccessException {
        List<BloodRequest> result = new ArrayList<>();
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
            throw new DataAccessException("Blood request query failed", e);
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

    private BloodRequest mapRow(ResultSet rs) throws SQLException {
        BloodRequest r = new BloodRequest();
        r.setId(rs.getLong("id"));
        r.setRequestCode(rs.getString("request_code"));
        r.setBloodGroup(readEnum(BloodGroup.class, rs.getString("blood_group")));
        r.setUnitsRequired(rs.getInt("units_required"));
        r.setLocation(rs.getString("location"));
        r.setPriority(readEnum(BloodRequestPriority.class,
                rs.getString("priority")));
        r.setStatus(readEnum(BloodRequestStatus.class, rs.getString("status")));
        r.setEmergencyDetails(rs.getString("emergency_details"));
        r.setHospitalId(getObjectOrNull(rs, "hospital_id"));
        r.setVictimId(getObjectOrNull(rs, "victim_id"));
        r.setRequireForDisaster(getObjectOrNull(rs, "disaster_id"));
        r.setCreatedBy(getObjectOrNull(rs, "created_by"));
        r.setRequestDate(readLocalDateTime(rs, "request_date"));
        r.setRequiredDate(readLocalDateTime(rs, "required_date"));
        r.setFulfilledByMatchId(getObjectOrNull(rs, "fulfilled_by_match_id"));
        r.setCreatedAt(readLocalDateTime(rs, "created_at"));
        r.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return r;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM blood_requests WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete blood request " + id, e);
        }
    }
}
