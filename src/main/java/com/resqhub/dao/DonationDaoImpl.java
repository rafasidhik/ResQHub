package com.resqhub.dao;

import com.resqhub.model.Donation;
import com.resqhub.model.DonationStatus;
import com.resqhub.model.DonationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class DonationDaoImpl implements DonationDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DonationDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Donation> rowMapper = (rs, rowNum) -> {
        Donation d = new Donation();
        d.setId(rs.getLong("id"));
        d.setDonationReference(rs.getString("donation_reference"));
        d.setDonorName(rs.getString("donor_name"));
        d.setDonorEmail(rs.getString("donor_email"));
        d.setDonorPhone(rs.getString("donor_phone"));
        
        String typeStr = rs.getString("donation_type");
        if (typeStr != null) {
            d.setDonationType(DonationType.valueOf(typeStr));
        }
        
        d.setAmount(rs.getBigDecimal("amount"));
        
        long itemId = rs.getLong("item_id");
        if (!rs.wasNull()) {
            d.setItemId(itemId);
        }
        
        int itemQty = rs.getInt("item_quantity");
        if (!rs.wasNull()) {
            d.setItemQuantity(itemQty);
        }
        
        Date donDate = rs.getDate("donation_date");
        if (donDate != null) {
            d.setDonationDate(donDate.toLocalDate());
        }
        
        String statStr = rs.getString("status");
        if (statStr != null) {
            d.setStatus(DonationStatus.valueOf(statStr));
        }
        
        d.setNotes(rs.getString("notes"));
        
        long verifiedById = rs.getLong("verified_by_user_id");
        if (!rs.wasNull()) {
            d.setVerifiedByUserId(verifiedById);
        }
        
        d.setInventoryAdded(rs.getBoolean("inventory_added"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            d.setCreatedAt(createdAt.toLocalDateTime());
        }

        try {
            d.setItemName(rs.getString("item_name"));
        } catch (Exception ignored) {}
        try {
            d.setItemUnit(rs.getString("item_unit"));
        } catch (Exception ignored) {}
        try {
            d.setVerifiedByUsername(rs.getString("verified_by_username"));
        } catch (Exception ignored) {}

        return d;
    };

    private static final String BASE_SELECT =
            "SELECT d.*, i.item_name, i.unit AS item_unit, u.username AS verified_by_username " +
            "FROM donations d " +
            "LEFT JOIN inventory_items i ON d.item_id = i.id " +
            "LEFT JOIN users u ON d.verified_by_user_id = u.id ";

    @Override
    public Optional<Donation> findById(Long id) {
        String sql = BASE_SELECT + "WHERE d.id = ?";
        List<Donation> list = jdbcTemplate.query(sql, rowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Donation> findByReference(String reference) {
        String sql = BASE_SELECT + "WHERE d.donation_reference = ?";
        List<Donation> list = jdbcTemplate.query(sql, rowMapper, reference);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<Donation> findAll() {
        String sql = BASE_SELECT + "ORDER BY d.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }

    @Override
    public List<Donation> findByEmail(String email) {
        String sql = BASE_SELECT + "WHERE LOWER(d.donor_email) = LOWER(?) ORDER BY d.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, email);
    }

    @Override
    public List<Donation> findByStatus(DonationStatus status) {
        String sql = BASE_SELECT + "WHERE d.status = ? ORDER BY d.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, status.name());
    }

    @Override
    public List<Donation> findByType(DonationType type) {
        String sql = BASE_SELECT + "WHERE d.donation_type = ? ORDER BY d.created_at DESC";
        return jdbcTemplate.query(sql, rowMapper, type.name());
    }

    @Override
    public Long save(Donation donation) {
        String sql = "INSERT INTO donations (donation_reference, donor_name, donor_email, donor_phone, donation_type, amount, item_id, item_quantity, donation_date, status, notes, verified_by_user_id, inventory_added) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, donation.getDonationReference());
            ps.setString(2, donation.getDonorName());
            ps.setString(3, donation.getDonorEmail());
            ps.setString(4, donation.getDonorPhone());
            ps.setString(5, donation.getDonationType().name());
            ps.setBigDecimal(6, donation.getAmount());
            if (donation.getItemId() != null) {
                ps.setLong(7, donation.getItemId());
            } else {
                ps.setNull(7, java.sql.Types.BIGINT);
            }
            if (donation.getItemQuantity() != null) {
                ps.setInt(8, donation.getItemQuantity());
            } else {
                ps.setNull(8, java.sql.Types.INTEGER);
            }
            ps.setDate(9, Date.valueOf(donation.getDonationDate()));
            ps.setString(10, donation.getStatus().name());
            ps.setString(11, donation.getNotes());
            if (donation.getVerifiedByUserId() != null) {
                ps.setLong(12, donation.getVerifiedByUserId());
            } else {
                ps.setNull(12, java.sql.Types.BIGINT);
            }
            ps.setBoolean(13, donation.isInventoryAdded());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            donation.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public void updateStatus(Long id, DonationStatus status, Long verifiedByUserId) {
        String sql = "UPDATE donations SET status = ?, verified_by_user_id = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), verifiedByUserId, id);
    }

    @Override
    public void markInventoryAdded(Long id) {
        String sql = "UPDATE donations SET inventory_added = TRUE WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM donations";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public BigDecimal sumTotalFunds() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM donations WHERE donation_type = 'MONETARY' AND status IN ('VERIFIED', 'RECEIVED', 'ALLOCATED')";
        BigDecimal sum = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Override
    public int sumMaterialItemsReceived() {
        String sql = "SELECT COALESCE(SUM(item_quantity), 0) FROM donations WHERE donation_type = 'MATERIAL' AND status IN ('VERIFIED', 'RECEIVED', 'ALLOCATED')";
        Integer sum = jdbcTemplate.queryForObject(sql, Integer.class);
        return sum != null ? sum : 0;
    }

    @Override
    public String generateNextReference() {
        int year = LocalDate.now().getYear();
        String pattern = "DON-" + year + "-%";
        String sql = "SELECT COUNT(*) FROM donations WHERE donation_reference LIKE ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, pattern);
        int next = (count != null ? count : 0) + 1;
        return String.format("DON-%d-%04d", year, next);
    }
}
