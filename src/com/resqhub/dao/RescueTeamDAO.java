package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.TeamOperationalStatus;
import com.resqhub.model.TeamType;

/** JDBC data access for the rescue_teams table. */
public class RescueTeamDAO extends BaseDao implements Repository<RescueTeam> {

    @Override
    public RescueTeam save(RescueTeam team) throws DataAccessException {
        if (team.getId() == null) {
            return insert(team);
        }
        return update(team);
    }

    private RescueTeam insert(RescueTeam t) throws DataAccessException {
        String sql = "INSERT INTO rescue_teams (team_name, team_type, leader_name, "
                + "contact_number, member_count, skills, equipment, "
                + "availability_status, operational_status, base_location) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            bindColumns(ps, t);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id returned for team");
                }
                long newId = keys.getLong(1);
                return findById(newId);
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save team: " + t.getTeamName(), e);
        }
    }

    private RescueTeam update(RescueTeam t) throws DataAccessException {
        String sql = "UPDATE rescue_teams SET team_name = ?, team_type = ?, "
                + "leader_name = ?, contact_number = ?, member_count = ?, "
                + "skills = ?, equipment = ?, availability_status = ?, "
                + "operational_status = ?, base_location = ? WHERE id = ?";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            bindColumns(ps, t);
            ps.setLong(11, t.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Team update affected " + rows
                                + " rows for id " + t.getId());
            }
            return findById(t.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update team " + t.getId(), e);
        }
    }

    private void bindColumns(PreparedStatement ps, RescueTeam t)
            throws SQLException {
        ps.setString(1, t.getTeamName());
        ps.setString(2, enumOrNull(t.getTeamType()));
        ps.setString(3, t.getLeaderName());
        ps.setString(4, t.getContactNumber());
        ps.setInt(5, t.getMemberCount());
        ps.setString(6, t.getSkills());
        ps.setString(7, t.getEquipment());
        ps.setString(8, enumOrNull(t.getAvailabilityStatus()));
        ps.setString(9, enumOrNull(t.getOperationalStatus()));
        ps.setString(10, t.getBaseLocation());
    }

    @Override
    public RescueTeam findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM rescue_teams WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load team " + id, e);
        }
    }

    public List<RescueTeam> findAvailable() throws DataAccessException {
        String sql = "SELECT * FROM rescue_teams "
                + "WHERE availability_status = 'AVAILABLE' "
                + "ORDER BY member_count DESC";
        List<RescueTeam> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list available teams", e);
        }
    }

    public void updateAvailability(long teamId, AvailabilityStatus status)
            throws DataAccessException {
        String sql = "UPDATE rescue_teams SET availability_status = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, enumOrNull(status));
            ps.setLong(2, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update availability", e);
        }
    }

    public void updateOperationalStatus(long teamId,
            TeamOperationalStatus status) throws DataAccessException {
        String sql = "UPDATE rescue_teams SET operational_status = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, enumOrNull(status));
            ps.setLong(2, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update operational status", e);
        }
    }

    /** Keyword search across team_name, leader_name, base_location, skills. */
    public List<RescueTeam> search(String keyword) throws DataAccessException {
        String like = "%" + keyword + "%";
        String sql = "SELECT * FROM rescue_teams "
                + "WHERE team_name LIKE ? OR leader_name LIKE ? "
                + "OR base_location LIKE ? OR skills LIKE ? "
                + "ORDER BY team_name";
        List<RescueTeam> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Team search failed", e);
        }
    }

    /** Filter by availability status. */
    public List<RescueTeam> findByAvailability(
            AvailabilityStatus status) throws DataAccessException {
        String sql = "SELECT * FROM rescue_teams "
                + "WHERE availability_status = ? ORDER BY team_name";
        List<RescueTeam> result = new ArrayList<>();
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
            throw new DataAccessException("Find by availability failed", e);
        }
    }

    /** Filter by operational status. */
    public List<RescueTeam> findByOperationalStatus(
            TeamOperationalStatus status) throws DataAccessException {
        String sql = "SELECT * FROM rescue_teams "
                + "WHERE operational_status = ? ORDER BY team_name";
        List<RescueTeam> result = new ArrayList<>();
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
                    "Find by operational status failed", e);
        }
    }

    @Override
    public List<RescueTeam> findAll() throws DataAccessException {
        String sql = "SELECT * FROM rescue_teams ORDER BY team_name";
        List<RescueTeam> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list teams", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM rescue_teams WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete team " + id, e);
        }
    }

    private RescueTeam mapRow(ResultSet rs) throws SQLException {
        RescueTeam t = new RescueTeam();
        t.setId(rs.getLong("id"));
        t.setTeamName(rs.getString("team_name"));
        t.setTeamType(readEnum(TeamType.class, rs.getString("team_type")));
        t.setLeaderName(rs.getString("leader_name"));
        t.setContactNumber(rs.getString("contact_number"));
        t.setMemberCount(rs.getInt("member_count"));
        t.setSkills(rs.getString("skills"));
        t.setEquipment(rs.getString("equipment"));
        t.setAvailabilityStatus(readEnum(AvailabilityStatus.class,
                rs.getString("availability_status")));
        t.setOperationalStatus(readEnum(TeamOperationalStatus.class,
                rs.getString("operational_status")));
        t.setBaseLocation(rs.getString("base_location"));
        t.setCreatedAt(readLocalDateTime(rs, "created_at"));
        t.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return t;
    }
}
