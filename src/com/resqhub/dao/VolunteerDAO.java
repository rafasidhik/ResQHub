package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.EmergencyRole;
import com.resqhub.model.Volunteer;
import com.resqhub.model.VolunteerAvailability;

/** JDBC data access for the volunteers table. */
public class VolunteerDAO extends BaseDao implements Repository<Volunteer> {

    @Override
    public Volunteer save(Volunteer v) throws DataAccessException {
        if (v.getId() == null) {
            return insert(v);
        }
        return update(v);
    }

    private Volunteer insert(Volunteer v) throws DataAccessException {
        String sql = "INSERT INTO volunteers (full_name, contact_number, "
                + "email, user_id, location, skills, availability, "
                + "emergency_role, max_workload) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            bindColumns(ps, v);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            throw new DataAccessException("No generated volunteer id");
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save volunteer: " + v.getFullName(), e);
        }
    }

    private Volunteer update(Volunteer v) throws DataAccessException {
        String sql = "UPDATE volunteers SET full_name = ?, contact_number = ?, "
                + "email = ?, user_id = ?, location = ?, skills = ?, "
                + "availability = ?, emergency_role = ?, max_workload = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            bindColumns(ps, v);
            ps.setLong(10, v.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Volunteer update affected " + rows + " rows");
            }
            return findById(v.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update volunteer " + v.getId(), e);
        }
    }

    private void bindColumns(PreparedStatement ps, Volunteer v)
            throws SQLException {
        ps.setString(1, v.getFullName());
        ps.setString(2, v.getContactNumber());
        ps.setString(3, v.getEmail());
        bindNullableLong(ps, 4, v.getUserId());
        ps.setString(5, v.getLocation());
        ps.setString(6, v.getSkills());
        ps.setString(7, v.getAvailability() == null
                ? "AVAILABLE" : v.getAvailability().name());
        ps.setString(8, enumOrNull(v.getEmergencyRole()));
        ps.setInt(9, v.getMaxWorkload());
    }

    @Override
    public Volunteer findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM volunteers WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load volunteer " + id, e);
        }
    }

    public List<Volunteer> findAvailable() throws DataAccessException {
        String sql = "SELECT * FROM volunteers "
                + "WHERE availability = 'AVAILABLE' ORDER BY full_name";
        List<Volunteer> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list available volunteers", e);
        }
    }

    public List<Volunteer> findByAvailability(VolunteerAvailability a)
            throws DataAccessException {
        String sql = "SELECT * FROM volunteers WHERE availability = ? "
                + "ORDER BY full_name";
        List<Volunteer> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, a.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not filter volunteers by availability", e);
        }
    }

    public List<Volunteer> findByEmergencyRole(EmergencyRole role)
            throws DataAccessException {
        String sql = "SELECT * FROM volunteers WHERE emergency_role = ? "
                + "ORDER BY full_name";
        List<Volunteer> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not filter volunteers by role", e);
        }
    }

    /** Keyword search across name, location, skills, contact. */
    public List<Volunteer> search(String keyword) throws DataAccessException {
        String like = "%" + keyword + "%";
        String sql = "SELECT * FROM volunteers WHERE full_name LIKE ? "
                + "OR location LIKE ? OR contact_number LIKE ? "
                + "OR skills LIKE ? ORDER BY full_name";
        List<Volunteer> result = new ArrayList<>();
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
            throw new DataAccessException("Volunteer search failed", e);
        }
    }

    /** Queries for id; returns null when not found (used by services). */
    public Volunteer findOptionalById(long id) throws DataAccessException {
        return findById(id);
    }

    /** Finds the volunteer linked to a given auth user id (nullable). */
    public Volunteer findByUserId(long userId) throws DataAccessException {
        String sql = "SELECT * FROM volunteers WHERE user_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not find volunteer by user id", e);
        }
    }

    @Override
    public List<Volunteer> findAll() throws DataAccessException {
        String sql = "SELECT * FROM volunteers ORDER BY full_name";
        List<Volunteer> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list volunteers", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM volunteers WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete volunteer " + id, e);
        }
    }

    private Volunteer mapRow(ResultSet rs) throws SQLException {
        Volunteer v = new Volunteer();
        v.setId(rs.getLong("id"));
        v.setFullName(rs.getString("full_name"));
        v.setContactNumber(rs.getString("contact_number"));
        v.setEmail(rs.getString("email"));
        long userId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            v.setUserId(userId);
        }
        v.setLocation(rs.getString("location"));
        v.setSkills(rs.getString("skills"));
        v.setAvailability(readEnum(VolunteerAvailability.class,
                rs.getString("availability")));
        v.setEmergencyRole(readEnum(EmergencyRole.class,
                rs.getString("emergency_role")));
        v.setMaxWorkload(rs.getInt("max_workload"));
        v.setCreatedAt(readLocalDateTime(rs, "created_at"));
        v.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return v;
    }
}
