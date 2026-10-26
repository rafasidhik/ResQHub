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
import com.resqhub.model.TeamMember;

/** JDBC data access for the team_members table. */
public class TeamMemberDAO extends BaseDao implements Repository<TeamMember> {

    @Override
    public TeamMember save(TeamMember m) throws DataAccessException {
        if (m.getId() == null) {
            return insert(m);
        }
        return update(m);
    }

    private TeamMember insert(TeamMember m) throws DataAccessException {
        String sql = "INSERT INTO team_members (team_id, member_name, role, "
                + "contact_number, special_skills, availability) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, m.getTeamId());
            ps.setString(2, m.getMemberName());
            ps.setString(3, m.getRole());
            ps.setString(4, m.getContactNumber());
            ps.setString(5, m.getSpecialSkills());
            ps.setString(6, enumOrNull(m.getAvailability()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            throw new DataAccessException("No generated member id");
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save team member", e);
        }
    }

    private TeamMember update(TeamMember m) throws DataAccessException {
        String sql = "UPDATE team_members SET member_name = ?, role = ?, "
                + "contact_number = ?, special_skills = ?, availability = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, m.getMemberName());
            ps.setString(2, m.getRole());
            ps.setString(3, m.getContactNumber());
            ps.setString(4, m.getSpecialSkills());
            ps.setString(5, enumOrNull(m.getAvailability()));
            ps.setLong(6, m.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Member update affected " + rows + " rows");
            }
            return findById(m.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update team member", e);
        }
    }

    @Override
    public TeamMember findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM team_members WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load team member " + id, e);
        }
    }

    public List<TeamMember> findByTeam(long teamId) throws DataAccessException {
        String sql = "SELECT * FROM team_members WHERE team_id = ? "
                + "ORDER BY member_name";
        List<TeamMember> result = new ArrayList<>();
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
                    "Could not list members for team " + teamId, e);
        }
    }

    public void deleteByTeam(long teamId) throws DataAccessException {
        String sql = "DELETE FROM team_members WHERE team_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete members for team " + teamId, e);
        }
    }

    @Override
    public List<TeamMember> findAll() throws DataAccessException {
        String sql = "SELECT * FROM team_members ORDER BY member_name";
        List<TeamMember> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list members", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM team_members WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete member " + id, e);
        }
    }

    private TeamMember mapRow(ResultSet rs) throws SQLException {
        TeamMember m = new TeamMember();
        m.setId(rs.getLong("id"));
        m.setTeamId(rs.getLong("team_id"));
        m.setMemberName(rs.getString("member_name"));
        m.setRole(rs.getString("role"));
        m.setContactNumber(rs.getString("contact_number"));
        m.setSpecialSkills(rs.getString("special_skills"));
        m.setAvailability(readEnum(AvailabilityStatus.class,
                rs.getString("availability")));
        m.setCreatedAt(readLocalDateTime(rs, "created_at"));
        m.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return m;
    }
}
