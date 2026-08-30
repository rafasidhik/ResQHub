package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Shelter;
import com.resqhub.model.ShelterOperationalStatus;

/** JDBC data access for the shelters table. */
public class ShelterDAO extends BaseDao implements Repository<Shelter> {

    @Override
    public Shelter save(Shelter shelter) throws DataAccessException {
        if (shelter.getId() == null) {
            return insert(shelter);
        }
        return update(shelter);
    }

    private Shelter insert(Shelter s) throws DataAccessException {
        String sql = "INSERT INTO shelters (name, code, district, city, address, "
                + "location_description, max_capacity, current_occupancy, "
                + "contact_number, manager_name, disaster_id, wheelchair_accessible, "
                + "elderly_friendly, medical_accessible, special_assistance, "
                + "operational_status, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            int idx = bindColumns(ps, s, 1);
            bindNullableLong(ps, idx, s.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id returned for shelter");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate shelter: a shelter with code '"
                                + s.getCode() + "' already exists", e);
            }
            throw new DataAccessException("Could not save shelter: " + s.getName(), e);
        }
    }

    private Shelter update(Shelter s) throws DataAccessException {
        String sql = "UPDATE shelters SET name = ?, code = ?, district = ?, "
                + "city = ?, address = ?, location_description = ?, "
                + "max_capacity = ?, current_occupancy = ?, contact_number = ?, "
                + "manager_name = ?, disaster_id = ?, wheelchair_accessible = ?, "
                + "elderly_friendly = ?, medical_accessible = ?, "
                + "special_assistance = ?, operational_status = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = bindColumns(ps, s, 1);
            ps.setLong(idx, s.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Shelter update affected " + rows + " rows for id "
                                + s.getId());
            }
            return findById(s.getId());
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate shelter: code '" + s.getCode()
                                + "' is already in use", e);
            }
            throw new DataAccessException("Could not update shelter " + s.getId(), e);
        }
    }

    private int bindColumns(PreparedStatement ps, Shelter s, int start)
            throws SQLException {
        ps.setString(start, s.getName());
        ps.setString(start + 1, s.getCode());
        ps.setString(start + 2, s.getDistrict());
        ps.setString(start + 3, s.getCity());
        ps.setString(start + 4, s.getAddress());
        ps.setString(start + 5, s.getLocationDescription());
        ps.setInt(start + 6, s.getMaxCapacity());
        ps.setInt(start + 7, s.getCurrentOccupancy());
        ps.setString(start + 8, s.getContactNumber());
        ps.setString(start + 9, s.getManagerName());
        bindNullableLong(ps, start + 10, s.getDisasterId());
        ps.setBoolean(start + 11, s.isWheelchairAccessible());
        ps.setBoolean(start + 12, s.isElderlyFriendly());
        ps.setBoolean(start + 13, s.isMedicalAccessible());
        ps.setBoolean(start + 14, s.isSpecialAssistance());
        ps.setString(start + 15, enumOrNull(s.getOperationalStatus()));
        return start + 16;
    }

    @Override
    public Shelter findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM shelters WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load shelter " + id, e);
        }
    }

    @Override
    public List<Shelter> findAll() throws DataAccessException {
        String sql = "SELECT * FROM shelters ORDER BY district, name";
        List<Shelter> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list shelters", e);
        }
    }

    public List<Shelter> findByOperationalStatus(ShelterOperationalStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM shelters WHERE operational_status = ? "
                + "ORDER BY district, name";
        List<Shelter> result = new ArrayList<>();
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
                    "Could not filter shelters by status", e);
        }
    }

    public List<Shelter> findByDistrict(String district)
            throws DataAccessException {
        String sql = "SELECT * FROM shelters WHERE LOWER(district) = LOWER(?) "
                + "ORDER BY district, name";
        List<Shelter> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, district.trim());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not filter shelters by district", e);
        }
    }

    /** Uses the stored generated column available_capacity for SQL-side
     *  filtering - a clean demonstration of capacity-aware queries. */
    public List<Shelter> findByAvailableCapacity(int minAvailable)
            throws DataAccessException {
        String sql = "SELECT * FROM shelters WHERE available_capacity >= ? "
                + "ORDER BY available_capacity DESC";
        List<Shelter> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, minAvailable);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not filter shelters by available capacity", e);
        }
    }

    /** Nearing capacity: occupancy >= 90% of capacity, not already FULL
     *  or CLOSED. Used by the Reports & Alerts module and the
     *  notification generation hook. */
    public List<Shelter> findNearCapacity() throws DataAccessException {
        String sql = "SELECT * FROM shelters "
                + "WHERE max_capacity > 0 "
                + "AND available_capacity <= max_capacity * 0.10 "
                + "AND operational_status NOT IN ('FULL','CLOSED') "
                + "ORDER BY available_capacity";
        List<Shelter> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not query shelters near capacity", e);
        }
    }

    /** Case-insensitive keyword search across name, code, district,
     *  city and address. */
    public List<Shelter> search(String keyword) throws DataAccessException {
        String sql = "SELECT * FROM shelters WHERE LOWER(name) LIKE ? "
                + "OR LOWER(code) LIKE ? OR LOWER(district) LIKE ? "
                + "OR LOWER(city) LIKE ? OR LOWER(address) LIKE ? "
                + "ORDER BY district, name";
        String pattern = "%" + keyword.toLowerCase() + "%";
        List<Shelter> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 1; i <= 5; i++) {
                ps.setString(i, pattern);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Shelter search failed", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM shelters WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete shelter " + id, e);
        }
    }

    /** Re-computes current_occupancy from ACTIVE allocations so the value
     *  stays consistent even if allocations are released out of order. */
    public void refreshOccupancy(long shelterId) throws DataAccessException {
        String sql = "UPDATE shelters s SET current_occupancy = "
                + "COALESCE((SELECT SUM(people_count) FROM shelter_allocations a "
                + "WHERE a.shelter_id = s.id AND a.status = 'ACTIVE'), 0) "
                + "WHERE s.id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, shelterId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not refresh occupancy for shelter " + shelterId, e);
        }
    }

    private boolean isDupe(SQLException e) {
        return e.getErrorCode() == 1062;
    }

    private Shelter mapRow(ResultSet rs) throws SQLException {
        Shelter s = new Shelter();
        s.setId(rs.getLong("id"));
        s.setName(rs.getString("name"));
        s.setCode(rs.getString("code"));
        s.setDistrict(rs.getString("district"));
        s.setCity(rs.getString("city"));
        s.setAddress(rs.getString("address"));
        s.setLocationDescription(rs.getString("location_description"));
        s.setMaxCapacity(rs.getInt("max_capacity"));
        s.setCurrentOccupancy(rs.getInt("current_occupancy"));
        s.setContactNumber(rs.getString("contact_number"));
        s.setManagerName(rs.getString("manager_name"));
        s.setDisasterId(getObjectOrNull(rs, "disaster_id"));
        s.setWheelchairAccessible(rs.getBoolean("wheelchair_accessible"));
        s.setElderlyFriendly(rs.getBoolean("elderly_friendly"));
        s.setMedicalAccessible(rs.getBoolean("medical_accessible"));
        s.setSpecialAssistance(rs.getBoolean("special_assistance"));
        s.setOperationalStatus(readEnum(ShelterOperationalStatus.class,
                rs.getString("operational_status")));
        s.setCreatedBy(getObjectOrNull(rs, "created_by"));
        s.setCreatedAt(readLocalDateTime(rs, "created_at"));
        s.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return s;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
