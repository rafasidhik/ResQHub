package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.DonationDistribution;

/** JDBC data access for the donation_distributions table. */
public class DonationDistributionDAO
        extends BaseDao implements Repository<DonationDistribution> {

    @Override
    public DonationDistribution save(DonationDistribution dist)
            throws DataAccessException {
        if (dist.getId() == null) {
            return insert(dist);
        }
        return update(dist);
    }

    private DonationDistribution insert(DonationDistribution d)
            throws DataAccessException {
        String sql = "INSERT INTO donation_distributions "
                + "(donation_id, distributed_to, quantity, distributed_at, "
                + "description) VALUES (?, ?, ?, ?, ?)";
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
            throw new DataAccessException(
                    "No generated distribution id");
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save distribution", e);
        }
    }

    private DonationDistribution update(DonationDistribution d)
            throws DataAccessException {
        String sql = "UPDATE donation_distributions SET donation_id = ?, "
                + "distributed_to = ?, quantity = ?, distributed_at = ?, "
                + "description = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindColumns(ps, d);
            ps.setLong(6, d.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Distribution update affected " + rows + " rows");
            }
            return findById(d.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update distribution " + d.getId(), e);
        }
    }

    private void bindColumns(PreparedStatement ps, DonationDistribution d)
            throws SQLException {
        ps.setLong(1, d.getDonationId());
        ps.setString(2, d.getDistributedTo());
        ps.setInt(3, d.getQuantity());
        bindLocalDateTime(ps, 4, d.getDistributedAt());
        ps.setString(5, d.getDescription());
    }

    @Override
    public DonationDistribution findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM donation_distributions WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load distribution " + id, e);
        }
    }

    public List<DonationDistribution> findByDonation(long donationId)
            throws DataAccessException {
        String sql = "SELECT * FROM donation_distributions "
                + "WHERE donation_id = ? ORDER BY distributed_at DESC";
        List<DonationDistribution> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, donationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list distributions for donation "
                            + donationId, e);
        }
    }

    @Override
    public List<DonationDistribution> findAll() throws DataAccessException {
        String sql = "SELECT * FROM donation_distributions "
                + "ORDER BY distributed_at DESC";
        List<DonationDistribution> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list distributions", e);
        }
    }

    /** Sum of distributed quantity for a donation (0 when none). */
    public int sumDistributedByDonation(long donationId)
            throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(quantity),0) AS total "
                + "FROM donation_distributions WHERE donation_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, donationId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not sum distributions for donation "
                            + donationId, e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM donation_distributions WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete distribution " + id, e);
        }
    }

    private DonationDistribution mapRow(ResultSet rs) throws SQLException {
        DonationDistribution d = new DonationDistribution();
        d.setId(rs.getLong("id"));
        d.setDonationId(rs.getLong("donation_id"));
        d.setDistributedTo(rs.getString("distributed_to"));
        d.setQuantity(rs.getInt("quantity"));
        d.setDistributedAt(readLocalDateTime(rs, "distributed_at"));
        d.setDescription(rs.getString("description"));
        d.setCreatedAt(readLocalDateTime(rs, "created_at"));
        d.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return d;
    }
}
