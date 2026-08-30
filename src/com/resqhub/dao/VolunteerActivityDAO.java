package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.VolunteerActivity;

/** JDBC data access for the volunteer_activity table. */
public class VolunteerActivityDAO extends BaseDao
        implements Repository<VolunteerActivity> {

    @Override
    public VolunteerActivity save(VolunteerActivity a)
            throws DataAccessException {
        String sql = "INSERT INTO volunteer_activity "
                + "(volunteer_id, activity_type, description) "
                + "VALUES (?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, a.getVolunteerId());
            ps.setString(2, a.getActivityType());
            ps.setString(3, a.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            throw new DataAccessException("No generated activity id");
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not record volunteer activity", e);
        }
    }

    public List<VolunteerActivity> findByVolunteer(long volunteerId)
            throws DataAccessException {
        String sql = "SELECT * FROM volunteer_activity "
                + "WHERE volunteer_id = ? ORDER BY activity_time DESC";
        List<VolunteerActivity> result = new ArrayList<>();
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
                    "Could not list activity for volunteer "
                            + volunteerId, e);
        }
    }

    @Override
    public VolunteerActivity findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM volunteer_activity WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load activity " + id, e);
        }
    }

    @Override
    public List<VolunteerActivity> findAll() throws DataAccessException {
        String sql = "SELECT * FROM volunteer_activity "
                + "ORDER BY activity_time DESC";
        List<VolunteerActivity> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list activity", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM volunteer_activity WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete activity " + id, e);
        }
    }

    private VolunteerActivity mapRow(ResultSet rs) throws SQLException {
        VolunteerActivity a = new VolunteerActivity();
        a.setId(rs.getLong("id"));
        a.setVolunteerId(rs.getLong("volunteer_id"));
        a.setActivityType(rs.getString("activity_type"));
        a.setDescription(rs.getString("description"));
        a.setActivityTime(readLocalDateTime(rs, "activity_time"));
        a.setCreatedAt(readLocalDateTime(rs, "created_at"));
        a.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return a;
    }
}
