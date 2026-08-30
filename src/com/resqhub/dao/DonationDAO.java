package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Donation;
import com.resqhub.model.DonationStatus;
import com.resqhub.model.DonationType;

/** JDBC data access for the donations table. */
public class DonationDAO extends BaseDao implements Repository<Donation> {

    @Override
    public Donation save(Donation donation) throws DataAccessException {
        if (donation.getId() == null) {
            return insert(donation);
        }
        return update(donation);
    }

    private Donation insert(Donation d) throws DataAccessException {
        String sql = "INSERT INTO donations (donor_id, donation_type, amount, "
                + "material_name, quantity, description, status, donated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            bindColumns(ps, d);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            throw new DataAccessException("No generated donation id");
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save donation", e);
        }
    }

    private Donation update(Donation d) throws DataAccessException {
        String sql = "UPDATE donations SET donor_id = ?, donation_type = ?, "
                + "amount = ?, material_name = ?, quantity = ?, description = ?, "
                + "status = ?, donated_at = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindColumns(ps, d);
            ps.setLong(9, d.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Donation update affected " + rows + " rows");
            }
            return findById(d.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update donation " + d.getId(), e);
        }
    }

    private void bindColumns(PreparedStatement ps, Donation d)
            throws SQLException {
        ps.setLong(1, d.getDonorId());
        ps.setString(2, d.getDonationType() == null
                ? "CASH" : d.getDonationType().name());
        if (d.getAmount() == null) {
            ps.setNull(3, java.sql.Types.DECIMAL);
        } else {
            ps.setBigDecimal(3, d.getAmount());
        }
        ps.setString(4, d.getMaterialName());
        if (d.getQuantity() == null) {
            ps.setNull(5, java.sql.Types.INTEGER);
        } else {
            ps.setInt(5, d.getQuantity());
        }
        ps.setString(6, d.getDescription());
        ps.setString(7, d.getStatus() == null
                ? "RECEIVED" : d.getStatus().name());
        bindLocalDateTime(ps, 8, d.getDonatedAt());
    }

    @Override
    public Donation findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM donations WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load donation " + id, e);
        }
    }

    public List<Donation> findByDonor(long donorId)
            throws DataAccessException {
        String sql = "SELECT * FROM donations WHERE donor_id = ? "
                + "ORDER BY donated_at DESC";
        List<Donation> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, donorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list donations for donor " + donorId, e);
        }
    }

    public List<Donation> findByType(DonationType type)
            throws DataAccessException {
        String sql = "SELECT * FROM donations WHERE donation_type = ? "
                + "ORDER BY donated_at DESC";
        List<Donation> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list donations by type", e);
        }
    }

    public List<Donation> findByStatus(DonationStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM donations WHERE status = ? "
                + "ORDER BY donated_at DESC";
        List<Donation> result = new ArrayList<>();
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
                    "Could not list donations by status", e);
        }
    }

    /** Donations that still have remaining quantity (not fully distributed). */
    public List<Donation> findUndistributed() throws DataAccessException {
        String sql = "SELECT * FROM donations WHERE status != 'DISTRIBUTED' "
                + "ORDER BY donated_at DESC";
        List<Donation> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list undistributed donations", e);
        }
    }

    /** Keyword search across material name, description, type, status. */
    public List<Donation> search(String keyword) throws DataAccessException {
        String like = "%" + keyword + "%";
        String sql = "SELECT * FROM donations WHERE material_name LIKE ? "
                + "OR description LIKE ? OR donation_type LIKE ? "
                + "OR status LIKE ? ORDER BY donated_at DESC";
        List<Donation> result = new ArrayList<>();
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
            throw new DataAccessException("Donation search failed", e);
        }
    }

    @Override
    public List<Donation> findAll() throws DataAccessException {
        String sql = "SELECT * FROM donations ORDER BY donated_at DESC";
        List<Donation> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list donations", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM donations WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete donation " + id, e);
        }
    }

    private Donation mapRow(ResultSet rs) throws SQLException {
        Donation d = new Donation();
        d.setId(rs.getLong("id"));
        d.setDonorId(rs.getLong("donor_id"));
        d.setDonationType(readEnum(DonationType.class,
                rs.getString("donation_type")));
        java.math.BigDecimal amount = rs.getBigDecimal("amount");
        d.setAmount(amount);
        d.setMaterialName(rs.getString("material_name"));
        int qty = rs.getInt("quantity");
        if (!rs.wasNull()) {
            d.setQuantity(qty);
        }
        d.setDescription(rs.getString("description"));
        d.setStatus(readEnum(DonationStatus.class,
                rs.getString("status")));
        d.setDonatedAt(readLocalDateTime(rs, "donated_at"));
        d.setCreatedAt(readLocalDateTime(rs, "created_at"));
        d.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return d;
    }
}
