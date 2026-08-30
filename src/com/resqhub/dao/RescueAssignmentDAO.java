package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.RequestStatus;
import com.resqhub.model.RescueAssignment;

/**
 * JDBC data access for the rescue_assignments junction table.
 * Contains the project's TRANSACTION demonstrations: assigning a team
 * and completing an assignment each touch three tables atomically.
 */
public class RescueAssignmentDAO extends BaseDao
        implements Repository<RescueAssignment> {

    /**
     * TRANSACTION: insert the assignment, flip the request to ASSIGNED and
     * mark the team DEPLOYED - all three succeed or all three roll back.
     */
    public long assignTeam(long requestId, long teamId, Long assignedBy)
            throws DataAccessException {
        String insertSql = "INSERT INTO rescue_assignments (rescue_request_id, "
                + "rescue_team_id, assigned_by, assignment_status) VALUES (?, ?, ?, 'ASSIGNED')";
        String requestSql = "UPDATE rescue_requests SET status = ? WHERE id = ?";
        String teamSql = "UPDATE rescue_teams SET availability_status = ? WHERE id = ?";

        Connection con = null;
        try {
            con = openConnection();
            con.setAutoCommit(false);
            long assignmentId;

            try (PreparedStatement ps = con.prepareStatement(insertSql,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, requestId);
                ps.setLong(2, teamId);
                bindNullableLong(ps, 3, assignedBy);
                if (ps.executeUpdate() != 1) {
                    throw new DataAccessException("Assignment insert failed");
                }
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new DataAccessException("No generated assignment id");
                    }
                    assignmentId = keys.getLong(1);
                }
            }

            try (PreparedStatement ps = con.prepareStatement(requestSql)) {
                ps.setString(1, RequestStatus.ASSIGNED.name());
                ps.setLong(2, requestId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = con.prepareStatement(teamSql)) {
                ps.setString(1,
                        com.resqhub.model.AvailabilityStatus.DEPLOYED.name());
                ps.setLong(2, teamId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE rescue_teams SET operational_status = 'ASSIGNED' "
                            + "WHERE id = ?")) {
                ps.setLong(1, teamId);
                ps.executeUpdate();
            }

            con.commit();
            return assignmentId;
        } catch (SQLException e) {
            rollbackQuietly(con);
            throw new DataAccessException(
                    "Assignment transaction rolled back", e);
        } catch (DataAccessException e) {
            rollbackQuietly(con);
            throw e;
        } finally {
            restoreAutoCommitAndClose(con);
        }
    }

    /**
     * TRANSACTION: mark the assignment COMPLETED, close the request as
     * RESCUED and release the team back to AVAILABLE.
     */
    public void completeAssignment(long assignmentId) throws DataAccessException {
        String updateSql = "UPDATE rescue_assignments SET assignment_status = ?, "
                + "completed_at = CURRENT_TIMESTAMP WHERE id = ?";
        String requestSql = "UPDATE rescue_requests r JOIN rescue_assignments a "
                + "ON a.rescue_request_id = r.id SET r.status = ? WHERE a.id = ?";
        String teamSql = "UPDATE rescue_teams t JOIN rescue_assignments a "
                + "ON a.rescue_team_id = t.id SET t.availability_status = ? WHERE a.id = ?";

        Connection con = null;
        try {
            con = openConnection();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setString(1, AssignmentStatus.COMPLETED.name());
                ps.setLong(2, assignmentId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(requestSql)) {
                ps.setString(1, RequestStatus.RESCUED.name());
                ps.setLong(2, assignmentId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(teamSql)) {
                ps.setString(1,
                        com.resqhub.model.AvailabilityStatus.AVAILABLE.name());
                ps.setLong(2, assignmentId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE rescue_teams t JOIN rescue_assignments a "
                            + "ON a.rescue_team_id = t.id "
                            + "SET t.operational_status = 'STANDBY' "
                            + "WHERE a.id = ?")) {
                ps.setLong(1, assignmentId);
                ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            rollbackQuietly(con);
            throw new DataAccessException(
                    "Completion transaction rolled back", e);
        } finally {
            restoreAutoCommitAndClose(con);
        }
    }

    /**
     * TRANSACTION: mark the assignment ABORTED, release the team and
     * return the request to PENDING so a different team can take over.
     */
    public void abortAssignment(long assignmentId, String notes)
            throws DataAccessException {
        String updateSql = "UPDATE rescue_assignments SET assignment_status = ?, "
                + "notes = ? WHERE id = ?";
        String requestSql = "UPDATE rescue_requests r JOIN rescue_assignments a "
                + "ON a.rescue_request_id = r.id SET r.status = ? WHERE a.id = ?";
        String teamSql = "UPDATE rescue_teams t JOIN rescue_assignments a "
                + "ON a.rescue_team_id = t.id SET t.availability_status = ? WHERE a.id = ?";

        Connection con = null;
        try {
            con = openConnection();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(updateSql)) {
                ps.setString(1, AssignmentStatus.ABORTED.name());
                ps.setString(2, notes);
                ps.setLong(3, assignmentId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(requestSql)) {
                ps.setString(1, RequestStatus.PENDING.name());
                ps.setLong(2, assignmentId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(teamSql)) {
                ps.setString(1,
                        com.resqhub.model.AvailabilityStatus.AVAILABLE.name());
                ps.setLong(2, assignmentId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE rescue_teams t JOIN rescue_assignments a "
                            + "ON a.rescue_team_id = t.id "
                            + "SET t.operational_status = 'STANDBY' "
                            + "WHERE a.id = ?")) {
                ps.setLong(1, assignmentId);
                ps.executeUpdate();
            }

            con.commit();
        } catch (SQLException e) {
            rollbackQuietly(con);
            throw new DataAccessException(
                    "Abort transaction rolled back", e);
        } finally {
            restoreAutoCommitAndClose(con);
        }
    }

    /** Simple status progression (EN_ROUTE / ON_SITE) with optional notes.
     *  Also updates the team's operational_status to match. */
    public void updateStatus(long assignmentId, AssignmentStatus status,
                             String notes) throws DataAccessException {
        String sql = "UPDATE rescue_assignments SET assignment_status = ?, "
                + "notes = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, enumOrNull(status));
            ps.setString(2, notes);
            ps.setLong(3, assignmentId);
            ps.executeUpdate();

            String opStatus = switch (status) {
                case EN_ROUTE -> "EN_ROUTE";
                case ON_SITE  -> "ON_MISSION";
                default       -> null;
            };
            if (opStatus != null) {
                String teamSql = "UPDATE rescue_teams t "
                        + "JOIN rescue_assignments a "
                        + "ON a.rescue_team_id = t.id "
                        + "SET t.operational_status = ? WHERE a.id = ?";
                try (PreparedStatement tps =
                             con.prepareStatement(teamSql)) {
                    tps.setString(1, opStatus);
                    tps.setLong(2, assignmentId);
                    tps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update assignment status", e);
        }
    }

    public List<RescueAssignment> findByRequest(long requestId)
            throws DataAccessException {
        String sql = "SELECT * FROM rescue_assignments "
                + "WHERE rescue_request_id = ? ORDER BY created_at DESC";
        List<RescueAssignment> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list assignments for request", e);
        }
    }

    /** All assignments for a given team. */
    public List<RescueAssignment> findByTeam(long teamId)
            throws DataAccessException {
        String sql = "SELECT * FROM rescue_assignments "
                + "WHERE rescue_team_id = ? ORDER BY created_at DESC";
        List<RescueAssignment> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list assignments for team", e);
        }
    }

    /** All assignments with a given status. */
    public List<RescueAssignment> findByStatus(AssignmentStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM rescue_assignments "
                + "WHERE assignment_status = ? ORDER BY created_at DESC";
        List<RescueAssignment> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, enumOrNull(status));
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

    /** Count assignments for a given team. */
    public int countByTeam(long teamId) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM rescue_assignments "
                + "WHERE rescue_team_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Count by team failed", e);
        }
    }

    /** Count completed assignments for a given team. */
    public int countCompletedByTeam(long teamId) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM rescue_assignments "
                + "WHERE rescue_team_id = ? AND assignment_status = 'COMPLETED'";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Count completed by team failed", e);
        }
    }

    @Override
    public RescueAssignment save(RescueAssignment entity) throws DataAccessException {
        throw new UnsupportedOperationException(
                "Use assignTeam() / updateStatus() - assignments are workflow rows");
    }

    @Override
    public RescueAssignment findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM rescue_assignments WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load assignment " + id, e);
        }
    }

    @Override
    public List<RescueAssignment> findAll() throws DataAccessException {
        String sql = "SELECT * FROM rescue_assignments ORDER BY created_at DESC";
        List<RescueAssignment> result = new ArrayList<>();
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
        String sql = "DELETE FROM rescue_assignments WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete assignment " + id, e);
        }
    }

    private void rollbackQuietly(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ignored) {
                // original exception matters more
            }
        }
    }

    private void restoreAutoCommitAndClose(Connection con) {
        if (con != null) {
            try {
                con.setAutoCommit(true);
            } catch (SQLException ignored) {
                // pool-less connection is being discarded anyway
            } finally {
                closeQuietly(con);
            }
        }
    }

    private RescueAssignment mapRow(ResultSet rs) throws SQLException {
        RescueAssignment a = new RescueAssignment();
        a.setId(rs.getLong("id"));
        a.setRescueRequestId(rs.getLong("rescue_request_id"));
        a.setRescueTeamId(rs.getLong("rescue_team_id"));
        long assignedBy = rs.getLong("assigned_by");
        if (!rs.wasNull()) {
            a.setAssignedBy(assignedBy);
        }
        a.setAssignmentStatus(readEnum(AssignmentStatus.class,
                rs.getString("assignment_status")));
        a.setNotes(rs.getString("notes"));
        a.setCompletedAt(readLocalDateTime(rs, "completed_at"));
        a.setCreatedAt(readLocalDateTime(rs, "created_at"));
        a.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return a;
    }
}
