package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.ShelterFacility;

/** JDBC data access for the shelter_facilities table. */
public class ShelterFacilityDAO extends BaseDao {

    public ShelterFacility save(ShelterFacility f) throws DataAccessException {
        if (f.getId() == null) {
            return insert(f);
        }
        return update(f);
    }

    private ShelterFacility insert(ShelterFacility f) throws DataAccessException {
        String sql = "INSERT INTO shelter_facilities (shelter_id, facility_name, "
                + "available) VALUES (?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, f.getShelterId());
            ps.setString(2, f.getFacilityName());
            ps.setBoolean(3, f.isAvailable());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for facility");
                }
                long newId = keys.getLong(1);
                return findByShelter(f.getShelterId()).stream()
                        .filter(x -> x.getId().equals(newId))
                        .findFirst().orElse(null);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new DataAccessException(
                        "Facility '" + f.getFacilityName()
                                + "' already exists on this shelter", e);
            }
            throw new DataAccessException("Could not add facility "
                    + f.getFacilityName(), e);
        }
    }

    private ShelterFacility update(ShelterFacility f) throws DataAccessException {
        String sql = "UPDATE shelter_facilities SET facility_name = ?, "
                + "available = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, f.getFacilityName());
            ps.setBoolean(2, f.isAvailable());
            ps.setLong(3, f.getId());
            ps.executeUpdate();
            return findById(f.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Could not update facility "
                    + f.getId(), e);
        }
    }

    public ShelterFacility findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM shelter_facilities WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load facility " + id, e);
        }
    }

    public List<ShelterFacility> findByShelter(long shelterId)
            throws DataAccessException {
        String sql = "SELECT * FROM shelter_facilities WHERE shelter_id = ? "
                + "ORDER BY facility_name";
        List<ShelterFacility> result = new ArrayList<>();
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
                    "Could not list facilities for shelter " + shelterId, e);
        }
    }

    public List<ShelterFacility> findByFacilityName(String name)
            throws DataAccessException {
        String sql = "SELECT * FROM shelter_facilities WHERE "
                + "LOWER(facility_name) = LOWER(?) AND available = 1 "
                + "ORDER BY shelter_id";
        List<ShelterFacility> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not query facilities by name", e);
        }
    }

    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM shelter_facilities WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete facility " + id, e);
        }
    }

    public boolean deleteByShelterAndName(long shelterId, String name)
            throws DataAccessException {
        String sql = "DELETE FROM shelter_facilities WHERE shelter_id = ? "
                + "AND LOWER(facility_name) = LOWER(?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, shelterId);
            ps.setString(2, name.trim());
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not remove facility from shelter " + shelterId, e);
        }
    }

    private ShelterFacility mapRow(ResultSet rs) throws SQLException {
        ShelterFacility f = new ShelterFacility();
        f.setId(rs.getLong("id"));
        f.setShelterId(rs.getLong("shelter_id"));
        f.setFacilityName(rs.getString("facility_name"));
        f.setAvailable(rs.getBoolean("available"));
        f.setCreatedAt(readLocalDateTime(rs, "created_at"));
        f.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return f;
    }
}
