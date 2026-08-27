package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.VolunteerSkill;

/** JDBC data access for the volunteer_skills table. */
public class VolunteerSkillDAO extends BaseDao
        implements Repository<VolunteerSkill> {

    @Override
    public VolunteerSkill save(VolunteerSkill s) throws DataAccessException {
        String sql = "INSERT INTO volunteer_skills (volunteer_id, skill_name) "
                + "VALUES (?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, s.getVolunteerId());
            ps.setString(2, s.getSkillName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    VolunteerSkill saved = new VolunteerSkill(
                            s.getVolunteerId(), s.getSkillName());
                    saved.setId(keys.getLong(1));
                    return saved;
                }
            }
            throw new DataAccessException("No generated skill id");
        } catch (SQLException e) {
            throw new DataAccessException("Could not save volunteer skill", e);
        }
    }

    public List<VolunteerSkill> findByVolunteer(long volunteerId)
            throws DataAccessException {
        String sql = "SELECT * FROM volunteer_skills "
                + "WHERE volunteer_id = ? ORDER BY skill_name";
        List<VolunteerSkill> result = new ArrayList<>();
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
                    "Could not list skills for volunteer " + volunteerId, e);
        }
    }

    /** Find volunteer ids that possess a matching skill. */
    public List<Long> findVolunteerIdsBySkill(String skillName)
            throws DataAccessException {
        String sql = "SELECT DISTINCT volunteer_id FROM volunteer_skills "
                + "WHERE LOWER(skill_name) LIKE ?";
        List<Long> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + skillName.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong("volunteer_id"));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not find volunteers by skill", e);
        }
    }

    public void deleteByVolunteer(long volunteerId) throws DataAccessException {
        String sql = "DELETE FROM volunteer_skills WHERE volunteer_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, volunteerId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete skills for volunteer " + volunteerId, e);
        }
    }

    @Override
    public VolunteerSkill findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM volunteer_skills WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load skill " + id, e);
        }
    }

    @Override
    public List<VolunteerSkill> findAll() throws DataAccessException {
        String sql = "SELECT * FROM volunteer_skills ORDER BY skill_name";
        List<VolunteerSkill> result = new ArrayList<>();
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
        String sql = "DELETE FROM volunteer_skills WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete skill " + id, e);
        }
    }

    private VolunteerSkill mapRow(ResultSet rs) throws SQLException {
        VolunteerSkill s = new VolunteerSkill();
        s.setId(rs.getLong("id"));
        s.setVolunteerId(rs.getLong("volunteer_id"));
        s.setSkillName(rs.getString("skill_name"));
        s.setCreatedAt(readLocalDateTime(rs, "created_at"));
        s.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return s;
    }
}
