package com.resqhub.dao;

import com.resqhub.model.InventoryItem;
import com.resqhub.model.InventoryTransaction;
import com.resqhub.model.ItemCategory;
import com.resqhub.model.ItemUnit;
import com.resqhub.model.TransactionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class InventoryDaoImpl implements InventoryDao {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public InventoryDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<InventoryItem> itemRowMapper = (rs, rowNum) -> {
        InventoryItem item = new InventoryItem();
        item.setId(rs.getLong("id"));
        item.setItemName(rs.getString("item_name"));
        String catStr = rs.getString("category");
        if (catStr != null) {
            item.setCategory(ItemCategory.valueOf(catStr));
        }
        String unitStr = rs.getString("unit");
        if (unitStr != null) {
            item.setUnit(ItemUnit.valueOf(unitStr));
        }
        item.setDescription(rs.getString("description"));
        item.setQuantityAvailable(rs.getInt("quantity_available"));
        item.setMinimumStockLevel(rs.getInt("minimum_stock_level"));
        item.setStorageLocation(rs.getString("storage_location"));
        Timestamp lastUpdated = rs.getTimestamp("last_updated_at");
        if (lastUpdated != null) {
            item.setLastUpdatedAt(lastUpdated.toLocalDateTime());
        }
        return item;
    };

    private final RowMapper<InventoryTransaction> transRowMapper = (rs, rowNum) -> {
        InventoryTransaction t = new InventoryTransaction();
        t.setId(rs.getLong("id"));
        t.setItemId(rs.getLong("item_id"));
        String typeStr = rs.getString("transaction_type");
        if (typeStr != null) {
            t.setTransactionType(TransactionType.valueOf(typeStr));
        }
        t.setQuantity(rs.getInt("quantity"));
        t.setReason(rs.getString("reason"));
        t.setPerformedByUserId(rs.getLong("performed_by_user_id"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            t.setCreatedAt(createdAt.toLocalDateTime());
        }

        try {
            t.setItemName(rs.getString("item_name"));
        } catch (Exception ignored) {}
        try {
            t.setPerformedByUsername(rs.getString("performed_by_username"));
        } catch (Exception ignored) {}

        return t;
    };

    @Override
    public Optional<InventoryItem> findById(Long id) {
        String sql = "SELECT * FROM inventory_items WHERE id = ?";
        List<InventoryItem> list = jdbcTemplate.query(sql, itemRowMapper, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<InventoryItem> findAll() {
        String sql = "SELECT * FROM inventory_items ORDER BY category ASC, item_name ASC";
        return jdbcTemplate.query(sql, itemRowMapper);
    }

    @Override
    public List<InventoryItem> findByCategory(ItemCategory category) {
        String sql = "SELECT * FROM inventory_items WHERE category = ? ORDER BY item_name ASC";
        return jdbcTemplate.query(sql, itemRowMapper, category.name());
    }

    @Override
    public List<InventoryItem> findLowStockItems() {
        String sql = "SELECT * FROM inventory_items WHERE quantity_available <= minimum_stock_level ORDER BY quantity_available ASC";
        return jdbcTemplate.query(sql, itemRowMapper);
    }

    @Override
    public Long save(InventoryItem item) {
        String sql = "INSERT INTO inventory_items (item_name, category, unit, description, quantity_available, minimum_stock_level, storage_location) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getCategory().name());
            ps.setString(3, item.getUnit().name());
            ps.setString(4, item.getDescription());
            ps.setInt(5, item.getQuantityAvailable());
            ps.setInt(6, item.getMinimumStockLevel());
            ps.setString(7, item.getStorageLocation());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            item.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public void update(InventoryItem item) {
        String sql = "UPDATE inventory_items SET item_name = ?, category = ?, unit = ?, description = ?, minimum_stock_level = ?, storage_location = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                item.getItemName(),
                item.getCategory().name(),
                item.getUnit().name(),
                item.getDescription(),
                item.getMinimumStockLevel(),
                item.getStorageLocation(),
                item.getId()
        );
    }

    @Override
    public void updateQuantity(Long itemId, int newQuantity) {
        String sql = "UPDATE inventory_items SET quantity_available = ? WHERE id = ?";
        jdbcTemplate.update(sql, newQuantity, itemId);
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM inventory_items WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM inventory_items";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public int countLowStock() {
        String sql = "SELECT COUNT(*) FROM inventory_items WHERE quantity_available <= minimum_stock_level";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public int getTotalQuantitySum() {
        String sql = "SELECT COALESCE(SUM(quantity_available), 0) FROM inventory_items";
        Integer sum = jdbcTemplate.queryForObject(sql, Integer.class);
        return sum != null ? sum : 0;
    }

    @Override
    public Long saveTransaction(InventoryTransaction transaction) {
        String sql = "INSERT INTO inventory_transactions (item_id, transaction_type, quantity, reason, performed_by_user_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, transaction.getItemId());
            ps.setString(2, transaction.getTransactionType().name());
            ps.setInt(3, transaction.getQuantity());
            ps.setString(4, transaction.getReason());
            ps.setLong(5, transaction.getPerformedByUserId());
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key != null) {
            transaction.setId(key.longValue());
            return key.longValue();
        }
        return null;
    }

    @Override
    public List<InventoryTransaction> findTransactionsByItemId(Long itemId) {
        String sql = "SELECT t.*, i.item_name, u.username AS performed_by_username " +
                "FROM inventory_transactions t " +
                "JOIN inventory_items i ON t.item_id = i.id " +
                "JOIN users u ON t.performed_by_user_id = u.id " +
                "WHERE t.item_id = ? " +
                "ORDER BY t.created_at DESC";
        return jdbcTemplate.query(sql, transRowMapper, itemId);
    }

    @Override
    public List<InventoryTransaction> findRecentTransactions(int limit) {
        String sql = "SELECT t.*, i.item_name, u.username AS performed_by_username " +
                "FROM inventory_transactions t " +
                "JOIN inventory_items i ON t.item_id = i.id " +
                "JOIN users u ON t.performed_by_user_id = u.id " +
                "ORDER BY t.created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, transRowMapper, limit);
    }
}
