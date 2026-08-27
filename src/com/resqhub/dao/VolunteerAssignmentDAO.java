package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.VolunteerAssignment;
import com.resqhub.model.VolunteerTaskStatus;

/** JDBC data access for the volunteer_assignments table. */
public class VolunteerAssignmentDAO extends BaseDao
        implements Repository<VolunteerAssignment> {

    @Override
    public VolunteerAssignment save(VolunteerAssignment a)
            throws DataAccessException {
        if (a.getId() == null) {
            return insert(a);
        }
        return update(a);
    }

    private VolunteerAssignment insert(VolunteerAssignment a)
            throws DataAccessException {
        String sql = "INSERT INTO volunteer_assignments (volunteer_id, "
                + "task_name, description, location, priority, status) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, a.getVolunteerId());
            ps.setString(2, a.getTaskName());
            ps.setString(3, a.getDescription());
            ps.setString(4, a.getLocation());
            ps.setInt(5, a.getPriority());
            ps.setString(6, a.getStatus() == null
                    ? "ASSIGNED" : a.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            throw new DataAccessException("No generated assignment id");
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save volunteer assignment", e);
        }
    }

    private VolunteerAssignment update(VolunteerAssignment a)
            throws DataAccessException {
        String sql = "UPDATE volunteer_assignments SET task_name = ?, "
                + "description = ?, location = ?, priority = ?, status = ?, "
                + "completed_at = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, a.getTaskName());
            ps.setString(2, a.getDescription());
            ps.setString(3, a.getLocation());
            ps.setInt(4, a.getPriority());
            ps.setString(5, a.getStatus() == null
                    ? "ASSIGNED" : a.getStatus().name());
            if (a.getCompletedAt() == null) {
                ps.setNull(6, java.sql.Types.TIMESTAMP);
            } else {
                ps.setObject(6, a.getCompletedAt());
            }
            ps.setLong(7, a.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Assignment update affected " + rows + " rows");
            }
            return findById(a.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update volunteer assignment", e);
        }
    }

    @Override
    public VolunteerAssignment findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM volunteer_assignments WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load assignment " + id, e);
        }
    }

    public List<VolunteerAssignment> findByVolunteer(long volunteerId)
            throws DataAccessException {
        String sql = "SELECT * FROM volunteer_assignments "
                + "WHERE volunteer_id = ? ORDER BY assigned_at DESC";
        List<VolunteerAssignment> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, volunteerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list assignments for volunteer "
                            + volunteerId, e);
        }
    }

    /** Count of active (not completed) tasks for a volunteer. */
    public int countActive(long volunteerId) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM volunteer_assignments "
                + "WHERE volunteer_id = ? AND status <> 'COMPLETED'";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, volunteerId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Count active assignments failed", e);
        }
    }

    public boolean hasActiveAssignment(long volunteerId)
            throws DataAccessException {
        return countActive(volunteerId) > 0;
    }

    public List<VolunteerAssignment> findByStatus(VolunteerTaskStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM volunteer_assignments "
                + "WHERE status = ? ORDER BY assigned_at DESC";
        List<VolunteerAssignment> result = new ArrayList<>();
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
            throw new DataAccessException(
                    "Could not list assignments by status", e);
        }
    }

    @Override
    public List<VolunteerAssignment> findAll() throws DataAccessException {
        String sql = "SELECT * FROM volunteer_assignments "
                + "ORDER BY assigned_at DESC";
        List<VolunteerAssignment> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list assignments", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM volunteer_assignments WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete assignment " + id, e);
        }
    }

    private VolunteerAssignment mapRow(ResultSet rs) throws SQLException {
        VolunteerAssignment a = new VolunteerAssignment();
        a.setId(rs.getLong("id"));
        a.setVolunteerId(rs.getLong("volunteer_id"));
        a.setTaskName(rs.getString("task_name"));
        a.setDescription(rs.getString("description"));
        a.setLocation(rs.getString("location"));
        a.setPriority(rs.getInt("priority"));
        a.setStatus(readEnum(VolunteerTaskStatus.class,
                rs.getString("status")));
        a.setAssignedAt(readLocalDateTime(rs, "assigned_at"));
        a.setCompletedAt(readLocalDateTime(rs, "completed_at"));
        a.setCreatedAt(readLocalDateTime(rs, "created_at"));
        a.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return a;
    }
}
