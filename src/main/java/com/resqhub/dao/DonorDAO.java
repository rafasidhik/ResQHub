package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Donor;
import com.resqhub.model.DonorType;

/** JDBC data access for the donors table. */
public class DonorDAO extends BaseDao implements Repository<Donor> {

    @Override
    public Donor save(Donor donor) throws DataAccessException {
        if (donor.getId() == null) {
            return insert(donor);
        }
        return update(donor);
    }

    private Donor insert(Donor donor) throws DataAccessException {
        String sql = "INSERT INTO donors (full_name, contact_number, email, "
                + "location, donor_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            bindColumns(ps, donor);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            throw new DataAccessException("No generated donor id");
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save donor: " + donor.getFullName(), e);
        }
    }

    private Donor update(Donor donor) throws DataAccessException {
        String sql = "UPDATE donors SET full_name = ?, contact_number = ?, "
                + "email = ?, location = ?, donor_type = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bindColumns(ps, donor);
            ps.setLong(6, donor.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Donor update affected " + rows + " rows");
            }
            return findById(donor.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update donor " + donor.getId(), e);
        }
    }

    private void bindColumns(PreparedStatement ps, Donor donor)
            throws SQLException {
        ps.setString(1, donor.getFullName());
        ps.setString(2, donor.getContactNumber());
        ps.setString(3, donor.getEmail());
        ps.setString(4, donor.getLocation());
        ps.setString(5, donor.getDonorType() == null
                ? "INDIVIDUAL" : donor.getDonorType().name());
    }

    @Override
    public Donor findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM donors WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load donor " + id, e);
        }
    }

    /** Keyword search across donor name, email, location, contact. */
    public List<Donor> search(String keyword) throws DataAccessException {
        String like = "%" + keyword + "%";
        String sql = "SELECT * FROM donors WHERE full_name LIKE ? "
                + "OR email LIKE ? OR location LIKE ? "
                + "OR contact_number LIKE ? ORDER BY full_name";
        List<Donor> result = new ArrayList<>();
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
            throw new DataAccessException("Donor search failed", e);
        }
    }

    public List<Donor> findByType(DonorType type) throws DataAccessException {
        String sql = "SELECT * FROM donors WHERE donor_type = ? "
                + "ORDER BY full_name";
        List<Donor> result = new ArrayList<>();
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
                    "Could not filter donors by type", e);
        }
    }

    @Override
    public List<Donor> findAll() throws DataAccessException {
        String sql = "SELECT * FROM donors ORDER BY full_name";
        List<Donor> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list donors", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM donors WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete donor " + id, e);
        }
    }

    private Donor mapRow(ResultSet rs) throws SQLException {
        Donor d = new Donor();
        d.setId(rs.getLong("id"));
        d.setFullName(rs.getString("full_name"));
        d.setContactNumber(rs.getString("contact_number"));
        d.setEmail(rs.getString("email"));
        d.setLocation(rs.getString("location"));
        d.setDonorType(readEnum(DonorType.class,
                rs.getString("donor_type")));
        d.setCreatedAt(readLocalDateTime(rs, "created_at"));
        d.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return d;
    }
}
