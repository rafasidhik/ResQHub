package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.BloodDonor;
import com.resqhub.model.BloodGroup;
import com.resqhub.model.DonorAvailability;
import com.resqhub.model.DonorEligibility;

/**
 * JDBC data access for the blood_donors table (spec: Database Storage).
 * Supports registration, full profile reads, search/filter by blood group,
 * location, availability and eligibility, plus the tailored matching queries.
 */
public class BloodDonorDAO extends BaseDao implements Repository<BloodDonor> {

    @Override
    public BloodDonor save(BloodDonor donor) throws DataAccessException {
        if (donor.getId() == null) {
            return insert(donor);
        }
        return update(donor);
    }

    private BloodDonor insert(BloodDonor d) throws DataAccessException {
        String sql = "INSERT INTO blood_donors (full_name, blood_group, "
                + "location, phone, email, availability, last_donation_date, "
                + "eligibility, notes, registered_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            bindColumns(ps, d, 1);
            bindNullableLong(ps, 10, d.getRegisteredBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for blood donor");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate donor: a donor already registered with "
                                + thisContact(d), e);
            }
            throw new DataAccessException(
                    "Could not save blood donor: " + d.getFullName(), e);
        }
    }

    private String thisContact(BloodDonor d) {
        return d.getPhone() == null || d.getPhone().isEmpty()
                ? "this contact" : d.getPhone();
    }

    private BloodDonor update(BloodDonor d) throws DataAccessException {
        String sql = "UPDATE blood_donors SET full_name = ?, blood_group = ?, "
                + "location = ?, phone = ?, email = ?, availability = ?, "
                + "last_donation_date = ?, eligibility = ?, notes = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindColumns(ps, d, 1);
            ps.setLong(10, d.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Blood donor update affected " + rows + " rows for id "
                                + d.getId());
            }
            return findById(d.getId());
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate donor: a donor already registered with "
                                + thisContact(d), e);
            }
            throw new DataAccessException(
                    "Could not update blood donor " + d.getId(), e);
        }
    }

    private int bindColumns(PreparedStatement ps, BloodDonor d, int start)
            throws SQLException {
        ps.setString(start, d.getFullName());
        ps.setString(start + 1, enumOrNull(d.getBloodGroup()));
        ps.setString(start + 2, d.getLocation());
        ps.setString(start + 3, d.getPhone());
        ps.setString(start + 4, d.getEmail());
        ps.setString(start + 5, enumOrNull(d.getAvailability()));
        setDate(ps, start + 6, d.getLastDonationDate());
        ps.setString(start + 7, enumOrNull(d.getEligibility()));
        ps.setString(start + 8, d.getNotes());
        return start + 9;
    }

    private void setDate(PreparedStatement ps, int index, LocalDate date)
            throws SQLException {
        if (date == null) {
            ps.setObject(index, null);
        } else {
            ps.setObject(index, date);
        }
    }

    @Override
    public BloodDonor findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM blood_donors WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load blood donor " + id, e);
        }
    }

    @Override
    public List<BloodDonor> findAll() throws DataAccessException {
        String sql = "SELECT * FROM blood_donors ORDER BY full_name";
        return query(sql, List.of());
    }

    public List<BloodDonor> search(String keyword) throws DataAccessException {
        String like = "%" + (keyword == null ? "" : keyword.toLowerCase())
                + "%";
        String sql = "SELECT * FROM blood_donors WHERE LOWER(full_name) LIKE ? "
                + "OR LOWER(location) LIKE ? OR LOWER(phone) LIKE ? "
                + "OR LOWER(email) LIKE ? OR LOWER(blood_group) LIKE ? "
                + "ORDER BY full_name";
        return query(sql, List.of(like, like, like, like, like));
    }

    public List<BloodDonor> findByBloodGroup(BloodGroup group)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_donors WHERE blood_group = ? "
                + "ORDER BY full_name";
        return query(sql, List.of(enumOrNull(group)));
    }

    public List<BloodDonor> findByAvailability(DonorAvailability availability)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_donors WHERE availability = ? "
                + "ORDER BY full_name";
        return query(sql, List.of(enumOrNull(availability)));
    }

    public List<BloodDonor> findByEligibility(DonorEligibility eligibility)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_donors WHERE eligibility = ? "
                + "ORDER BY full_name";
        return query(sql, List.of(enumOrNull(eligibility)));
    }

    public List<BloodDonor> findSuitableByGroup(BloodGroup group)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_donors WHERE "
                + "availability = 'AVAILABLE' AND eligibility = 'ELIGIBLE' "
                + "AND blood_group = ? ORDER BY full_name";
        return query(sql, List.of(enumOrNull(group)));
    }

    /**
     * Donors whose blood group can donate to the requested group AND who are
     * currently available and eligible (spec: Donor Matching).
     */
    public List<BloodDonor> findPotentialDonors(BloodGroup requestedGroup)
            throws DataAccessException {
        StringBuilder sql = new StringBuilder("SELECT * FROM blood_donors "
                + "WHERE availability = 'AVAILABLE' "
                + "AND eligibility = 'ELIGIBLE' ");
        List<Object> params = new ArrayList<>();
        StringBuilder groups = new StringBuilder();
        for (BloodGroup g : BloodGroup.values()) {
            if (g.canDonateTo(requestedGroup)) {
                if (groups.length() > 0) {
                    groups.append(",");
                }
                groups.append("'").append(g.name()).append("'");
            }
        }
        if (groups.length() > 0) {
            sql.append("AND blood_group IN (").append(groups).append(") ");
        }
        sql.append("ORDER BY "
                + "CASE blood_group WHEN 'O_NEGATIVE' THEN 0 "
                + "WHEN 'O_POSITIVE' THEN 1 WHEN 'A_NEGATIVE' THEN 2 "
                + "WHEN 'A_POSITIVE' THEN 3 WHEN 'B_NEGATIVE' THEN 4 "
                + "WHEN 'B_POSITIVE' THEN 5 WHEN 'AB_NEGATIVE' THEN 6 "
                + "WHEN 'AB_POSITIVE' THEN 7 ELSE 8 END, full_name");
        return query(sql.toString(), params);
    }

    /** Total confirmable units currently available for a blood group. */
    public int countAvailableFor(BloodGroup group) throws DataAccessException {
        return findSuitableByGroup(group).size();
    }

    /** Total donors in a matching group pool (for availability reporting). */
    public int countInMatchingPool(BloodGroup requested)
            throws DataAccessException {
        return findPotentialDonors(requested).size();
    }

    public BloodDonor findByPhone(String phone) throws DataAccessException {
        if (phone == null) {
            return null;
        }
        String sql = "SELECT * FROM blood_donors WHERE phone = ? LIMIT 1";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, phone.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load blood donor by phone", e);
        }
    }

    private List<BloodDonor> query(String sql, List<Object> params)
            throws DataAccessException {
        List<BloodDonor> result = new ArrayList<>();
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
            throw new DataAccessException("Blood donor query failed", e);
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

    private BloodDonor mapRow(ResultSet rs) throws SQLException {
        BloodDonor d = new BloodDonor();
        d.setId(rs.getLong("id"));
        d.setFullName(rs.getString("full_name"));
        d.setBloodGroup(readEnum(BloodGroup.class, rs.getString("blood_group")));
        d.setLocation(rs.getString("location"));
        d.setPhone(rs.getString("phone"));
        d.setEmail(rs.getString("email"));
        d.setAvailability(readEnum(DonorAvailability.class,
                rs.getString("availability")));
        d.setLastDonationDate(readDate(rs, "last_donation_date"));
        d.setEligibility(readEnum(DonorEligibility.class,
                rs.getString("eligibility")));
        d.setNotes(rs.getString("notes"));
        d.setRegisteredBy(getObjectOrNull(rs, "registered_by"));
        d.setCreatedAt(readLocalDateTime(rs, "created_at"));
        d.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return d;
    }

    private LocalDate readDate(ResultSet rs, String column)
            throws SQLException {
        java.sql.Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM blood_donors WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete blood donor " + id, e);
        }
    }

    private boolean isDupe(SQLException e) {
        return e.getErrorCode() == 1062;
    }
}
