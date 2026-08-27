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
import com.resqhub.model.ShelterAllocation;
import com.resqhub.model.ShelterAllocationStatus;

/** JDBC data access for the shelter_allocations table. */
public class ShelterAllocationDAO extends BaseDao {

    public ShelterAllocation save(ShelterAllocation a) throws DataAccessException {
        String sql = "INSERT INTO shelter_allocations (shelter_id, victim_id, "
                + "family_name, people_count, notes, allocated_at, status, "
                + "allocated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.getShelterId());
            bindNullableLong(ps, 2, a.getVictimId());
            ps.setString(3, a.getFamilyName());
            ps.setInt(4, a.getPeopleCount());
            ps.setString(5, a.getNotes());
            bindLocalDateTime(ps, 6, a.getAllocatedAt() == null
                    ? LocalDateTime.now() : a.getAllocatedAt());
            ps.setString(7, enumOrNull(a.getStatus()));
            bindNullableLong(ps, 8, a.getAllocatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for allocation");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save allocation", e);
        }
    }

    public ShelterAllocation findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM shelter_allocations WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load allocation " + id, e);
        }
    }

    public List<ShelterAllocation> findByShelter(long shelterId)
            throws DataAccessException {
        String sql = "SELECT * FROM shelter_allocations WHERE shelter_id = ? "
                + "ORDER BY status, allocated_at DESC";
        List<ShelterAllocation> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, shelterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list allocations for shelter " + shelterId, e);
        }
    }

    /** Marks an allocation released, stamping the release time. */
    public void release(long allocationId) throws DataAccessException {
        String sql = "UPDATE shelter_allocations SET status = 'RELEASED', "
                + "released_at = ? WHERE id = ? AND status = 'ACTIVE'";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, LocalDateTime.now());
            ps.setLong(2, allocationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not release allocation "
                    + allocationId, e);
        }
    }

    /** Total people currently accommodated (SUM over ACTIVE allocations). */
    public int sumActivePeople(long shelterId) throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(people_count), 0) FROM "
                + "shelter_allocations WHERE shelter_id = ? AND status = 'ACTIVE'";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, shelterId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not compute occupancy for shelter " + shelterId, e);
        }
    }

    private ShelterAllocation mapRow(ResultSet rs) throws SQLException {
        ShelterAllocation a = new ShelterAllocation();
        a.setId(rs.getLong("id"));
        a.setShelterId(rs.getLong("shelter_id"));
        a.setVictimId(getObjectOrNull(rs, "victim_id"));
        a.setFamilyName(rs.getString("family_name"));
        a.setPeopleCount(rs.getInt("people_count"));
        a.setNotes(rs.getString("notes"));
        a.setAllocatedAt(readLocalDateTime(rs, "allocated_at"));
        a.setReleasedAt(readLocalDateTime(rs, "released_at"));
        a.setStatus(readEnum(ShelterAllocationStatus.class, rs.getString("status")));
        a.setAllocatedBy(getObjectOrNull(rs, "allocated_by"));
        a.setCreatedAt(readLocalDateTime(rs, "created_at"));
        a.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return a;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
