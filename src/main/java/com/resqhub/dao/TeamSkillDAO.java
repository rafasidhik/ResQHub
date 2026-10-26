package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.TeamSkill;

/** JDBC data access for the team_skills table. */
public class TeamSkillDAO extends BaseDao implements Repository<TeamSkill> {

    @Override
    public TeamSkill save(TeamSkill s) throws DataAccessException {
        if (s.getId() == null) {
            return insert(s);
        }
        return update(s);
    }

    private TeamSkill insert(TeamSkill s) throws DataAccessException {
        String sql = "INSERT INTO team_skills (team_id, skill_name, description) "
                + "VALUES (?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, s.getTeamId());
            ps.setString(2, s.getSkillName());
            ps.setString(3, s.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            throw new DataAccessException("No generated skill id");
        } catch (SQLException e) {
            throw new DataAccessException("Could not save team skill", e);
        }
    }

    private TeamSkill update(TeamSkill s) throws DataAccessException {
        String sql = "UPDATE team_skills SET skill_name = ?, description = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, s.getSkillName());
            ps.setString(2, s.getDescription());
            ps.setLong(3, s.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Skill update affected " + rows + " rows");
            }
            return findById(s.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update team skill", e);
        }
    }

    @Override
    public TeamSkill findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM team_skills WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load skill " + id, e);
        }
    }

    public List<TeamSkill> findByTeam(long teamId) throws DataAccessException {
        String sql = "SELECT * FROM team_skills WHERE team_id = ? "
                + "ORDER BY skill_name";
        List<TeamSkill> result = new ArrayList<>();
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
                    "Could not list skills for team " + teamId, e);
        }
    }

    /** Find all teams that have a skill whose name matches (case-insensitive). */
    public List<Long> findTeamIdsBySkill(String skillName)
            throws DataAccessException {
        String sql = "SELECT DISTINCT team_id FROM team_skills "
                + "WHERE LOWER(skill_name) LIKE ? ";
        List<Long> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + skillName.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong("team_id"));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not find teams by skill", e);
        }
    }

    public void deleteByTeam(long teamId) throws DataAccessException {
        String sql = "DELETE FROM team_skills WHERE team_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete skills for team " + teamId, e);
        }
    }

    @Override
    public List<TeamSkill> findAll() throws DataAccessException {
        String sql = "SELECT * FROM team_skills ORDER BY skill_name";
        List<TeamSkill> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list skills", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM team_skills WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete skill " + id, e);
        }
    }

    private TeamSkill mapRow(ResultSet rs) throws SQLException {
        TeamSkill s = new TeamSkill();
        s.setId(rs.getLong("id"));
        s.setTeamId(rs.getLong("team_id"));
        s.setSkillName(rs.getString("skill_name"));
        s.setDescription(rs.getString("description"));
        s.setCreatedAt(readLocalDateTime(rs, "created_at"));
        s.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return s;
    }
}
