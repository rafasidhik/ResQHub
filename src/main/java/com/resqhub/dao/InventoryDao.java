package com.resqhub.dao;

import com.resqhub.model.InventoryItem;
import com.resqhub.model.InventoryTransaction;
import com.resqhub.model.ItemCategory;

import java.util.List;
import java.util.Optional;

public interface InventoryDao {
    Optional<InventoryItem> findById(Long id);
    List<InventoryItem> findAll();
    List<InventoryItem> findByCategory(ItemCategory category);
    List<InventoryItem> findLowStockItems();
    Long save(InventoryItem item);
    void update(InventoryItem item);
    void updateQuantity(Long itemId, int newQuantity);
    void delete(Long id);
    long count();
    int countLowStock();
    int getTotalQuantitySum();

    Long saveTransaction(InventoryTransaction transaction);
    List<InventoryTransaction> findTransactionsByItemId(Long itemId);
    List<InventoryTransaction> findRecentTransactions(int limit);
}
