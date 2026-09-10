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
import com.resqhub.model.BloodDonation;
import com.resqhub.model.BloodGroup;

/**
 * JDBC data access for the blood_donations table - the donor's donation
 * history and per-request donation records (spec: Blood Donation Recording /
 * Donation History).
 */
public class BloodDonationDAO extends BaseDao implements Repository<BloodDonation> {

    @Override
    public BloodDonation save(BloodDonation d) throws DataAccessException {
        if (d.getId() == null) {
            return insert(d);
        }
        return update(d);
    }

    private BloodDonation insert(BloodDonation d) throws DataAccessException {
        String sql = "INSERT INTO blood_donations (donor_id, blood_group, "
                + "donation_date, request_id, units_donated, donation_status, "
                + "notes, recorded_by, recorded_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            int idx = bindColumns(ps, d, 1);
            bindLocalDateTime(ps, idx, d.getRecordedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for blood donation");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save blood donation record", e);
        }
    }

    private BloodDonation update(BloodDonation d) throws DataAccessException {
        String sql = "UPDATE blood_donations SET donor_id = ?, blood_group = ?, "
                + "donation_date = ?, request_id = ?, units_donated = ?, "
                + "donation_status = ?, notes = ?, recorded_by = ?, "
                + "recorded_at = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = bindColumns(ps, d, 1);
            bindLocalDateTime(ps, idx, d.getRecordedAt());
            ps.setLong(idx + 1, d.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Blood donation update affected " + rows + " rows for id "
                                + d.getId());
            }
            return findById(d.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update blood donation record " + d.getId(), e);
        }
    }

    private int bindColumns(PreparedStatement ps, BloodDonation d, int start)
            throws SQLException {
        bindNullableLong(ps, start, d.getDonorId());
        ps.setString(start + 1, enumOrNull(d.getBloodGroup()));
        setDate(ps, start + 2, d.getDonationDate());
        bindNullableLong(ps, start + 3, d.getRequestId());
        ps.setInt(start + 4, d.getUnitsDonated());
        ps.setString(start + 5, d.getDonationStatus());
        ps.setString(start + 6, d.getNotes());
        bindNullableLong(ps, start + 7, d.getRecordedBy());
        return start + 8;
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
    public BloodDonation findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM blood_donations WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load blood donation record " + id, e);
        }
    }

    @Override
    public List<BloodDonation> findAll() throws DataAccessException {
        String sql = "SELECT * FROM blood_donations "
                + "ORDER BY donation_date DESC, id DESC";
        return query(sql, List.of());
    }

    public List<BloodDonation> findByDonor(long donorId)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_donations WHERE donor_id = ? "
                + "ORDER BY donation_date DESC, id DESC";
        return query(sql, List.of(donorId));
    }

    public List<BloodDonation> findByRequest(long requestId)
            throws DataAccessException {
        String sql = "SELECT * FROM blood_donations WHERE request_id = ? "
                + "ORDER BY donation_date DESC, id DESC";
        return query(sql, List.of(requestId));
    }

    public LocalDate lastDonationForDonor(long donorId)
            throws DataAccessException {
        String sql = "SELECT MAX(donation_date) FROM blood_donations "
                + "WHERE donor_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                java.sql.Date date = rs.getDate(1);
                return date == null ? null : date.toLocalDate();
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not read last donation date", e);
        }
    }

    public int countDonationsForDonor(long donorId) throws DataAccessException {
        String sql = "SELECT COUNT(*) FROM blood_donations WHERE donor_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not count donation records", e);
        }
    }

    public int totalUnits(int donorId) throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(units_donated),0) "
                + "FROM blood_donations WHERE donor_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not sum donation units", e);
        }
    }

    private List<BloodDonation> query(String sql, List<Object> params)
            throws DataAccessException {
        List<BloodDonation> result = new ArrayList<>();
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
            throw new DataAccessException("Blood donation query failed", e);
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

    private BloodDonation mapRow(ResultSet rs) throws SQLException {
        BloodDonation d = new BloodDonation();
        d.setId(rs.getLong("id"));
        d.setDonorId(getObjectOrNull(rs, "donor_id"));
        d.setBloodGroup(readEnum(BloodGroup.class, rs.getString("blood_group")));
        d.setDonationDate(readDate(rs, "donation_date"));
        d.setRequestId(getObjectOrNull(rs, "request_id"));
        d.setUnitsDonated(rs.getInt("units_donated"));
        d.setDonationStatus(rs.getString("donation_status"));
        d.setNotes(rs.getString("notes"));
        d.setRecordedBy(getObjectOrNull(rs, "recorded_by"));
        d.setRecordedAt(readLocalDateTime(rs, "recorded_at"));
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
        String sql = "DELETE FROM blood_donations WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete blood donation record " + id, e);
        }
    }
}
