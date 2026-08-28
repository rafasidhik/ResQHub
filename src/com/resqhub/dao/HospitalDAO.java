package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Hospital;
import com.resqhub.model.HospitalFacility;
import com.resqhub.model.HospitalStatus;

/**
 * JDBC data access for the hospitals table - hospital registration, location
 * / contact details, bed capacity and emergency facilities. Available bed
 * counts are always derived (total - occupied), never stored.
 */
public class HospitalDAO extends BaseDao implements Repository<Hospital> {

    @Override
    public Hospital save(Hospital hospital) throws DataAccessException {
        if (hospital.getId() == null) {
            return insert(hospital);
        }
        return update(hospital);
    }

    private Hospital insert(Hospital h) throws DataAccessException {
        String sql = "INSERT INTO hospitals (name, hospital_id, district, "
                + "city, area, address, phone, emergency_contact, email, "
                + "total_beds, occupied_beds, facilities, status, disaster_id, "
                + "created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            int idx = bindColumns(ps, h, 1);
            bindNullableLong(ps, idx, h.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for hospital");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate hospital: a hospital with id '"
                                + h.getHospitalId() + "' already exists", e);
            }
            throw new DataAccessException(
                    "Could not save hospital: " + h.getName(), e);
        }
    }

    private Hospital update(Hospital h) throws DataAccessException {
        String sql = "UPDATE hospitals SET name = ?, hospital_id = ?, "
                + "district = ?, city = ?, area = ?, address = ?, phone = ?, "
                + "emergency_contact = ?, email = ?, total_beds = ?, "
                + "occupied_beds = ?, facilities = ?, status = ?, "
                + "disaster_id = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = bindColumns(ps, h, 1);
            bindNullableLong(ps, idx, h.getDisasterId());
            ps.setLong(idx + 1, h.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Hospital update affected " + rows + " rows for id "
                                + h.getId());
            }
            return findById(h.getId());
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate hospital: a hospital with id '"
                                + h.getHospitalId() + "' already exists", e);
            }
            throw new DataAccessException(
                    "Could not update hospital " + h.getId(), e);
        }
    }

    /** Binds the shared (non-id, non-audit) columns; returns next index. */
    private int bindColumns(PreparedStatement ps, Hospital h, int start)
            throws SQLException {
        ps.setString(start, h.getName());
        ps.setString(start + 1, h.getHospitalId());
        ps.setString(start + 2, h.getDistrict());
        ps.setString(start + 3, h.getCity());
        ps.setString(start + 4, h.getArea());
        ps.setString(start + 5, h.getAddress());
        ps.setString(start + 6, h.getPhone());
        ps.setString(start + 7, h.getEmergencyContact());
        ps.setString(start + 8, h.getEmail());
        ps.setInt(start + 9, h.getTotalBeds());
        ps.setInt(start + 10, h.getOccupiedBeds());
        ps.setString(start + 11, encodeFacilities(h.getFacilities()));
        ps.setString(start + 12, enumOrNull(h.getStatus()));
        bindNullableLong(ps, start + 13, h.getDisasterId());
        return start + 14;
    }

    @Override
    public Hospital findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM hospitals WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load hospital " + id, e);
        }
    }

    @Override
    public List<Hospital> findAll() throws DataAccessException {
        String sql = "SELECT * FROM hospitals ORDER BY district, name";
        return query(sql, List.of());
    }

    public List<Hospital> findByStatus(HospitalStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM hospitals WHERE status = ? "
                + "ORDER BY name";
        return query(sql, List.of(enumOrNull(status)));
    }

    public List<Hospital> findAccepting() throws DataAccessException {
        String sql = "SELECT * FROM hospitals WHERE status IN ("
                + "'AVAILABLE','LIMITED_CAPACITY','EMERGENCY_ONLY') "
                + "ORDER BY district, name";
        return query(sql, List.of());
    }

    /** Hospitals at 90%+ occupancy (near capacity). */
    public List<Hospital> findNearCapacity() throws DataAccessException {
        String sql = "SELECT * FROM hospitals WHERE total_beds > 0 "
                + "AND (occupied_beds / total_beds) >= 0.9 "
                + "AND status <> 'INACTIVE' ORDER BY"
                + " (occupied_beds / total_beds) DESC";
        return query(sql, List.of());
    }

    public List<Hospital> findFull() throws DataAccessException {
        String sql = "SELECT * FROM hospitals WHERE status = 'FULL' "
                + "ORDER BY name";
        return query(sql, List.of());
    }

    public List<Hospital> search(String keyword) throws DataAccessException {
        String sql = "SELECT * FROM hospitals WHERE LOWER(name) LIKE ? "
                + "OR LOWER(hospital_id) LIKE ? OR LOWER(district) LIKE ? "
                + "OR LOWER(city) LIKE ? OR LOWER(address) LIKE ? "
                + "ORDER BY district, name";
        String like = "%" + (keyword == null ? "" : keyword.toLowerCase())
                + "%";
        return query(sql, List.of(like, like, like, like, like));
    }

    public List<Hospital> filter(String keyword, String district,
            HospitalStatus status, Integer minAvailable,
            HospitalFacility facility) throws DataAccessException {
        StringBuilder sql = new StringBuilder("SELECT * FROM hospitals "
                + "WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (LOWER(name) LIKE ? OR LOWER(hospital_id) LIKE ? "
                    + "OR LOWER(city) LIKE ? OR LOWER(address) LIKE ?)");
            String like = "%" + keyword.toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (district != null && !district.isEmpty()) {
            sql.append(" AND LOWER(district) LIKE ?");
            params.add("%" + district.toLowerCase() + "%");
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(enumOrNull(status));
        }
        if (minAvailable != null && minAvailable > 0) {
            sql.append(" AND (total_beds - occupied_beds) >= ?");
            params.add(minAvailable);
        }
        if (facility != null) {
            sql.append(" AND FIND_IN_SET(?, facilities)");
            params.add(facility.name());
        }
        sql.append(" ORDER BY district, name");
        return query(sql.toString(), params);
    }

    /** Beds currently free (derived) across all hospitals. */
    public int sumAvailableBeds() throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(total_beds - occupied_beds),0) "
                + "FROM hospitals";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not sum available hospital beds", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM hospitals WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete hospital " + id, e);
        }
    }

    private List<Hospital> query(String sql, List<Object> params)
            throws DataAccessException {
        List<Hospital> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                bindParam(ps, i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Hospital query failed", e);
        }
    }

    private void bindParam(PreparedStatement ps, int index, Object value)
            throws SQLException {
        if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else {
            ps.setString(index, String.valueOf(value));
        }
    }

    private Hospital mapRow(ResultSet rs) throws SQLException {
        Hospital h = new Hospital();
        h.setId(rs.getLong("id"));
        h.setName(rs.getString("name"));
        h.setHospitalId(rs.getString("hospital_id"));
        h.setDistrict(rs.getString("district"));
        h.setCity(rs.getString("city"));
        h.setArea(rs.getString("area"));
        h.setAddress(rs.getString("address"));
        h.setPhone(rs.getString("phone"));
        h.setEmergencyContact(rs.getString("emergency_contact"));
        h.setEmail(rs.getString("email"));
        h.setTotalBeds(rs.getInt("total_beds"));
        h.setOccupiedBeds(rs.getInt("occupied_beds"));
        h.setFacilities(decodeFacilities(rs.getString("facilities")));
        h.setStatus(readEnum(HospitalStatus.class, rs.getString("status")));
        h.setDisasterId(getObjectOrNull(rs, "disaster_id"));
        h.setCreatedBy(getObjectOrNull(rs, "created_by"));
        h.setCreatedAt(readLocalDateTime(rs, "created_at"));
        h.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return h;
    }

    static String encodeFacilities(Set<HospitalFacility> facilities) {
        if (facilities == null || facilities.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (HospitalFacility f : facilities) {
            if (f != null) {
                names.add(f.name());
            }
        }
        return String.join(",", names);
    }

    static Set<HospitalFacility> decodeFacilities(String value) {
        Set<HospitalFacility> set = new LinkedHashSet<>();
        if (value == null || value.isEmpty()) {
            return set;
        }
        for (String part : value.split(",")) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                set.add(HospitalFacility.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // unknown facility value - skip
            }
        }
        return set;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private boolean isDupe(SQLException e) {
        return e.getErrorCode() == 1062;
    }
}
