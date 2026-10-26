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
        updateStatus(allocationId, ShelterAllocationStatus.RELEASED);
    }

    /**
     * Generic status transition. When the target status no longer counts
     * the occupants (terminal: COMPLETED / CANCELLED / RELEASED) the
     * release timestamp is stamped so the shelter can free the space.
     */
    public void updateStatus(long allocationId,
                             ShelterAllocationStatus status)
            throws DataAccessException {
        String sql = "UPDATE shelter_allocations SET status = ?"
                + ", released_at = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setObject(2, status.isOccupying() ? null
                    : LocalDateTime.now());
            ps.setLong(3, allocationId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update status of "
                    + "allocation " + allocationId + " to " + status, e);
        }
    }

    /** All allocation records, newest first (for the management view). */
    public List<ShelterAllocation> findAll() throws DataAccessException {
        String sql = "SELECT * FROM shelter_allocations "
                + "ORDER BY allocated_at DESC";
        return query(sql, List.of());
    }

    public List<ShelterAllocation> findByStatus(ShelterAllocationStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM shelter_allocations WHERE status = ? "
                + "ORDER BY allocated_at DESC";
        return query(sql, List.of(status.name()));
    }

    /** Any allocation in an occupying state for the given victim. */
    public ShelterAllocation findActiveByVictim(long victimId)
            throws DataAccessException {
        String sql = "SELECT * FROM shelter_allocations WHERE victim_id = ? "
                + "AND status IN ('ACTIVE','CHECKED_IN','PENDING') "
                + "ORDER BY allocated_at DESC LIMIT 1";
        List<ShelterAllocation> rows = query(sql, List.of(victimId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<ShelterAllocation> query(String sql, List<Object> params)
            throws DataAccessException {
        List<ShelterAllocation> result = new ArrayList<>();
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
            throw new DataAccessException("Allocation query failed", e);
        }
    }

    /** Total people currently accommodated (SUM over occupying allocations). */
    public int sumActivePeople(long shelterId) throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(people_count), 0) FROM "
                + "shelter_allocations WHERE shelter_id = ? "
                + "AND status IN ('ACTIVE','CHECKED_IN')";
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
