package com.resqhub.service;

import com.resqhub.dto.FoodDistributionDto;
import com.resqhub.dto.InventoryItemDto;
import com.resqhub.dto.ResourceAllocationDto;
import com.resqhub.dto.StockAdjustmentDto;
import com.resqhub.model.*;

import java.util.List;
import java.util.Optional;

public interface ResourceService {
    List<InventoryItem> getAllItems();
    List<InventoryItem> getItemsByCategory(ItemCategory category);
    List<InventoryItem> getLowStockItems();
    Optional<InventoryItem> getItemById(Long id);
    InventoryItem createItem(InventoryItemDto dto, Long currentUserId);
    InventoryItem updateItem(Long id, InventoryItemDto dto, Long currentUserId);
    void deleteItem(Long id, Long currentUserId);

    void adjustStock(StockAdjustmentDto dto, Long currentUserId);
    List<InventoryTransaction> getItemTransactions(Long itemId);
    List<InventoryTransaction> getRecentTransactions(int limit);

    List<ResourceAllocation> getAllAllocations();
    List<ResourceAllocation> getAllocationsByDisaster(Long disasterId);
    List<ResourceAllocation> getAllocationsByShelter(Long shelterId);
    Optional<ResourceAllocation> getAllocationById(Long id);
    ResourceAllocation createAllocation(ResourceAllocationDto dto, Long currentUserId);
    void approveAllocation(Long allocationId, Long currentUserId);
    void dispatchAllocation(Long allocationId, Long currentUserId);
    void deliverAllocation(Long allocationId, Long currentUserId);
    void cancelAllocation(Long allocationId, Long currentUserId);

    List<FoodDistribution> getAllFoodDistributions();
    List<FoodDistribution> getFoodDistributionsByShelter(Long shelterId);
    Optional<FoodDistribution> getFoodDistributionById(Long id);
    FoodDistribution createFoodDistribution(FoodDistributionDto dto, Long currentUserId);
    void completeFoodDistribution(Long distributionId, Long currentUserId);

    long getTotalItemCount();
    int getTotalQuantity();
    int getLowStockCount();
    long getTotalAllocationsCount();
    int getTotalRecipientsServed();
}
