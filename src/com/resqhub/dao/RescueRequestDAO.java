package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RequestStatus;
import com.resqhub.model.RescueRequest;

/** JDBC data access for the rescue_requests table. */
public class RescueRequestDAO extends BaseDao implements Repository<RescueRequest> {

    @Override
    public RescueRequest save(RescueRequest request) throws DataAccessException {
        if (request.getId() == null) {
            return insert(request);
        }
        return update(request);
    }

    private RescueRequest insert(RescueRequest r) throws DataAccessException {
        String sql = "INSERT INTO rescue_requests (disaster_id, victim_id, requester_name, "
                + "contact_number, location, people_count, children_count, elderly_count, "
                + "life_threatening, medical_emergency, trapped_under_debris, "
                + "required_assistance, priority, status, requested_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            bindColumns(ps, r);
            if (r.getRequestedAt() == null) {
                ps.setObject(15, java.time.LocalDateTime.now());
            }

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("No generated id returned for request");
                }
                long newId = keys.getLong(1);
                return findById(newId);
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save rescue request: " + r.getRequesterName(), e);
        }
    }

    private RescueRequest update(RescueRequest r) throws DataAccessException {
        String sql = "UPDATE rescue_requests SET disaster_id = ?, victim_id = ?, "
                + "requester_name = ?, contact_number = ?, location = ?, "
                + "people_count = ?, children_count = ?, elderly_count = ?, "
                + "life_threatening = ?, medical_emergency = ?, trapped_under_debris = ?, "
                + "required_assistance = ?, priority = ?, status = ?, requested_at = ? "
                + "WHERE id = ?";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            bindColumns(ps, r);
            ps.setLong(16, r.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Request update affected " + rows + " rows for id " + r.getId());
            }
            return findById(r.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Could not update request " + r.getId(), e);
        }
    }

    private void bindColumns(PreparedStatement ps, RescueRequest r) throws SQLException {
        ps.setLong(1, r.getDisasterId());
        bindNullableLong(ps, 2, r.getVictimId());
        ps.setString(3, r.getRequesterName());
        ps.setString(4, r.getContactNumber());
        ps.setString(5, r.getLocation());
        ps.setInt(6, r.getPeopleCount());
        ps.setInt(7, r.getChildrenCount());
        ps.setInt(8, r.getElderlyCount());
        ps.setBoolean(9, r.isLifeThreatening());
        ps.setBoolean(10, r.isMedicalEmergency());
        ps.setBoolean(11, r.isTrappedUnderDebris());
        ps.setString(12, r.getRequiredAssistance());
        ps.setString(13, enumOrNull(r.getPriority()));
        ps.setString(14, enumOrNull(r.getStatus()));
        bindLocalDateTime(ps, 15, r.getRequestedAt());
    }

    @Override
    public RescueRequest findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM rescue_requests WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load rescue request " + id, e);
        }
    }

    /**
     * Pending queue sorted by computed priority (CRITICAL first),
     * oldest request first within the same priority.
     */
    public List<RescueRequest> findPendingByPriority() throws DataAccessException {
        String sql = "SELECT * FROM rescue_requests WHERE status = 'PENDING' "
                + "ORDER BY CASE priority WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 "
                + "WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END DESC, requested_at ASC";
        List<RescueRequest> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list pending requests", e);
        }
    }

    public List<RescueRequest> findByStatus(RequestStatus status) throws DataAccessException {
        String sql = "SELECT * FROM rescue_requests WHERE status = ? ORDER BY requested_at";
        List<RescueRequest> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list requests by status", e);
        }
    }

    /** Targeted update used by the service layer after priority computation. */
    public void updatePriorityAndStatus(long id, PriorityLevel priority,
                                        RequestStatus status)
            throws DataAccessException {
        String sql = "UPDATE rescue_requests SET priority = ?, status = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, enumOrNull(priority));
            ps.setString(2, enumOrNull(status));
            ps.setLong(3, id);
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException("Priority update affected " + rows + " rows");
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not update request state", e);
        }
    }

    public int countByStatus(RequestStatus status) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM rescue_requests WHERE status = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Count by status failed", e);
        }
    }

    @Override
    public List<RescueRequest> findAll() throws DataAccessException {
        String sql = "SELECT * FROM rescue_requests ORDER BY requested_at DESC";
        List<RescueRequest> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list rescue requests", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM rescue_requests WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete rescue request " + id, e);
        }
    }

    private RescueRequest mapRow(ResultSet rs) throws SQLException {
        RescueRequest r = new RescueRequest();
        r.setId(rs.getLong("id"));
        r.setDisasterId(rs.getLong("disaster_id"));
        long victimId = rs.getLong("victim_id");
        if (!rs.wasNull()) {
            r.setVictimId(victimId);
        }
        r.setRequesterName(rs.getString("requester_name"));
        r.setContactNumber(rs.getString("contact_number"));
        r.setLocation(rs.getString("location"));
        r.setPeopleCount(rs.getInt("people_count"));
        r.setChildrenCount(rs.getInt("children_count"));
        r.setElderlyCount(rs.getInt("elderly_count"));
        r.setLifeThreatening(rs.getBoolean("life_threatening"));
        r.setMedicalEmergency(rs.getBoolean("medical_emergency"));
        r.setTrappedUnderDebris(rs.getBoolean("trapped_under_debris"));
        r.setRequiredAssistance(rs.getString("required_assistance"));
        r.setPriority(readEnum(PriorityLevel.class, rs.getString("priority")));
        r.setStatus(readEnum(RequestStatus.class, rs.getString("status")));
        r.setRequestedAt(readLocalDateTime(rs, "requested_at"));
        r.setCreatedAt(readLocalDateTime(rs, "created_at"));
        r.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return r;
    }
}
