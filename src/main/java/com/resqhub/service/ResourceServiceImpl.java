package com.resqhub.service;

import com.resqhub.dao.DisasterDao;
import com.resqhub.dao.FoodDistributionDao;
import com.resqhub.dao.InventoryDao;
import com.resqhub.dao.ResourceAllocationDao;
import com.resqhub.dao.ShelterDao;
import com.resqhub.dto.FoodDistributionDto;
import com.resqhub.dto.InventoryItemDto;
import com.resqhub.dto.ResourceAllocationDto;
import com.resqhub.dto.StockAdjustmentDto;
import com.resqhub.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ResourceServiceImpl implements ResourceService {

    private final InventoryDao inventoryDao;
    private final ResourceAllocationDao resourceAllocationDao;
    private final FoodDistributionDao foodDistributionDao;
    private final DisasterDao disasterDao;
    private final ShelterDao shelterDao;
    private final ActivityLogService activityLogService;

    @Autowired
    public ResourceServiceImpl(InventoryDao inventoryDao,
                               ResourceAllocationDao resourceAllocationDao,
                               FoodDistributionDao foodDistributionDao,
                               DisasterDao disasterDao,
                               ShelterDao shelterDao,
                               ActivityLogService activityLogService) {
        this.inventoryDao = inventoryDao;
        this.resourceAllocationDao = resourceAllocationDao;
        this.foodDistributionDao = foodDistributionDao;
        this.disasterDao = disasterDao;
        this.shelterDao = shelterDao;
        this.activityLogService = activityLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getAllItems() {
        return inventoryDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getItemsByCategory(ItemCategory category) {
        return inventoryDao.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItem> getLowStockItems() {
        return inventoryDao.findLowStockItems();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<InventoryItem> getItemById(Long id) {
        return inventoryDao.findById(id);
    }

    @Override
    public InventoryItem createItem(InventoryItemDto dto, Long currentUserId) {
        InventoryItem item = new InventoryItem();
        item.setItemName(dto.getItemName());
        item.setCategory(dto.getCategory());
        item.setUnit(dto.getUnit());
        item.setDescription(dto.getDescription());
        item.setQuantityAvailable(dto.getQuantityAvailable());
        item.setMinimumStockLevel(dto.getMinimumStockLevel());
        item.setStorageLocation(dto.getStorageLocation());

        Long id = inventoryDao.save(item);
        item.setId(id);

        if (dto.getQuantityAvailable() > 0) {
            InventoryTransaction trans = new InventoryTransaction();
            trans.setItemId(id);
            trans.setTransactionType(TransactionType.RECEIVED);
            trans.setQuantity(dto.getQuantityAvailable());
            trans.setReason("Initial inventory registration");
            trans.setPerformedByUserId(currentUserId);
            inventoryDao.saveTransaction(trans);
        }

        activityLogService.log(currentUserId, "CREATE_INVENTORY_ITEM", "RESOURCES",
                String.valueOf(id), "Registered new inventory item: " + item.getItemName() + " (" + item.getCategory().getDisplayName() + ")");

        return item;
    }

    @Override
    public InventoryItem updateItem(Long id, InventoryItemDto dto, Long currentUserId) {
        InventoryItem item = inventoryDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found with id: " + id));

        item.setItemName(dto.getItemName());
        item.setCategory(dto.getCategory());
        item.setUnit(dto.getUnit());
        item.setDescription(dto.getDescription());
        item.setMinimumStockLevel(dto.getMinimumStockLevel());
        item.setStorageLocation(dto.getStorageLocation());

        inventoryDao.update(item);

        activityLogService.log(currentUserId, "UPDATE_INVENTORY_ITEM", "RESOURCES",
                String.valueOf(id), "Updated specifications for: " + item.getItemName());

        return item;
    }

    @Override
    public void deleteItem(Long id, Long currentUserId) {
        InventoryItem item = inventoryDao.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found with id: " + id));

        // Check if item has active allocations
        List<ResourceAllocation> allocations = resourceAllocationDao.findAll().stream()
                .filter(a -> a.getItemId().equals(id) && (a.getStatus() == ResourceAllocationStatus.PLANNED || a.getStatus() == ResourceAllocationStatus.APPROVED || a.getStatus() == ResourceAllocationStatus.DISPATCHED))
                .toList();
        if (!allocations.isEmpty()) {
            throw new IllegalStateException("Cannot delete item '" + item.getItemName() + "' because it has " + allocations.size() + " active allocation(s).");
        }

        inventoryDao.delete(id);
        activityLogService.log(currentUserId, "DELETE_INVENTORY_ITEM", "RESOURCES",
                String.valueOf(id), "Deleted inventory item: " + item.getItemName());
    }

    @Override
    public void adjustStock(StockAdjustmentDto dto, Long currentUserId) {
        InventoryItem item = inventoryDao.findById(dto.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + dto.getItemId()));

        int current = item.getQuantityAvailable();
        int newQuantity;

        switch (dto.getTransactionType()) {
            case RECEIVED -> newQuantity = current + dto.getQuantity();
            case ISSUED, ALLOCATED -> {
                if (current < dto.getQuantity()) {
                    throw new IllegalStateException("Insufficient stock for '" + item.getItemName() + "'. Available: " + current + ", Requested: " + dto.getQuantity());
                }
                newQuantity = current - dto.getQuantity();
            }
            case ADJUSTED -> {
                newQuantity = dto.getQuantity();
            }
            default -> throw new IllegalArgumentException("Unsupported transaction type: " + dto.getTransactionType());
        }

        inventoryDao.updateQuantity(item.getId(), newQuantity);

        InventoryTransaction trans = new InventoryTransaction();
        trans.setItemId(item.getId());
        trans.setTransactionType(dto.getTransactionType());
        trans.setQuantity(dto.getQuantity());
        trans.setReason(dto.getReason());
        trans.setPerformedByUserId(currentUserId);
        inventoryDao.saveTransaction(trans);

        activityLogService.log(currentUserId, "STOCK_ADJUSTMENT", "RESOURCES",
                String.valueOf(item.getId()), dto.getTransactionType() + " " + dto.getQuantity() + " " + item.getUnit().name() + " of " + item.getItemName() + ". New balance: " + newQuantity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getItemTransactions(Long itemId) {
        return inventoryDao.findTransactionsByItemId(itemId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryTransaction> getRecentTransactions(int limit) {
        return inventoryDao.findRecentTransactions(limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceAllocation> getAllAllocations() {
        return resourceAllocationDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceAllocation> getAllocationsByDisaster(Long disasterId) {
        return resourceAllocationDao.findByDisasterId(disasterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceAllocation> getAllocationsByShelter(Long shelterId) {
        return resourceAllocationDao.findByShelterId(shelterId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResourceAllocation> getAllocationById(Long id) {
        return resourceAllocationDao.findById(id);
    }

    @Override
    public ResourceAllocation createAllocation(ResourceAllocationDto dto, Long currentUserId) {
        InventoryItem item = inventoryDao.findById(dto.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Resource item not found with id: " + dto.getItemId()));
        disasterDao.findById(dto.getDisasterId())
                .orElseThrow(() -> new IllegalArgumentException("Disaster not found with id: " + dto.getDisasterId()));

        if (dto.getShelterId() != null) {
            shelterDao.findById(dto.getShelterId())
                    .orElseThrow(() -> new IllegalArgumentException("Shelter not found with id: " + dto.getShelterId()));
        }

        if (item.getQuantityAvailable() < dto.getQuantity()) {
            throw new IllegalStateException("Insufficient inventory available. Current stock: " + item.getQuantityAvailable() + ", Requested: " + dto.getQuantity());
        }

        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setItemId(dto.getItemId());
        allocation.setQuantity(dto.getQuantity());
        allocation.setDisasterId(dto.getDisasterId());
        allocation.setShelterId(dto.getShelterId());
        allocation.setDestinationLocation(dto.getDestinationLocation());
        allocation.setPurpose(dto.getPurpose());
        allocation.setResponsiblePerson(dto.getResponsiblePerson());
        allocation.setStatus(dto.getStatus() != null ? dto.getStatus() : ResourceAllocationStatus.PLANNED);
        allocation.setAllocatedByUserId(currentUserId);
        allocation.setRemarks(dto.getRemarks());

        Long id = resourceAllocationDao.save(allocation);
        allocation.setId(id);

        activityLogService.log(currentUserId, "CREATE_RESOURCE_ALLOCATION", "RESOURCES",
                String.valueOf(id), "Created allocation plan of " + allocation.getQuantity() + " " + item.getItemName() + " for " + allocation.getDestinationLocation());

        return allocation;
    }

    @Override
    public void approveAllocation(Long allocationId, Long currentUserId) {
        ResourceAllocation allocation = resourceAllocationDao.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with id: " + allocationId));

        if (allocation.getStatus() != ResourceAllocationStatus.PLANNED) {
            throw new IllegalStateException("Only PLANNED allocations can be approved. Current status: " + allocation.getStatus());
        }

        resourceAllocationDao.updateStatus(allocationId, ResourceAllocationStatus.APPROVED);
        activityLogService.log(currentUserId, "APPROVE_RESOURCE_ALLOCATION", "RESOURCES",
                String.valueOf(allocationId), "Approved resource allocation #" + allocationId);
    }

    @Override
    public void dispatchAllocation(Long allocationId, Long currentUserId) {
        ResourceAllocation allocation = resourceAllocationDao.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with id: " + allocationId));

        if (allocation.getStatus() != ResourceAllocationStatus.APPROVED && allocation.getStatus() != ResourceAllocationStatus.PLANNED) {
            throw new IllegalStateException("Allocation cannot be dispatched from status: " + allocation.getStatus());
        }

        InventoryItem item = inventoryDao.findById(allocation.getItemId())
                .orElseThrow(() -> new IllegalStateException("Allocated item no longer exists"));

        if (item.getQuantityAvailable() < allocation.getQuantity()) {
            throw new IllegalStateException("Cannot dispatch. Insufficient inventory for '" + item.getItemName() + "'. In stock: " + item.getQuantityAvailable() + ", Required: " + allocation.getQuantity());
        }

        // Deduct inventory atomically
        int updatedStock = item.getQuantityAvailable() - allocation.getQuantity();
        inventoryDao.updateQuantity(item.getId(), updatedStock);

        InventoryTransaction trans = new InventoryTransaction();
        trans.setItemId(item.getId());
        trans.setTransactionType(TransactionType.ISSUED);
        trans.setQuantity(allocation.getQuantity());
        trans.setReason("Dispatched for Allocation #" + allocationId + " to " + allocation.getDestinationLocation());
        trans.setPerformedByUserId(currentUserId);
        inventoryDao.saveTransaction(trans);

        resourceAllocationDao.updateStatus(allocationId, ResourceAllocationStatus.DISPATCHED);
        activityLogService.log(currentUserId, "DISPATCH_RESOURCE_ALLOCATION", "RESOURCES",
                String.valueOf(allocationId), "Dispatched " + allocation.getQuantity() + " " + item.getItemName() + " (Allocation #" + allocationId + ")");
    }

    @Override
    public void deliverAllocation(Long allocationId, Long currentUserId) {
        ResourceAllocation allocation = resourceAllocationDao.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with id: " + allocationId));

        if (allocation.getStatus() != ResourceAllocationStatus.DISPATCHED) {
            throw new IllegalStateException("Only DISPATCHED allocations can be marked as DELIVERED. Current status: " + allocation.getStatus());
        }

        resourceAllocationDao.updateStatus(allocationId, ResourceAllocationStatus.DELIVERED);
        activityLogService.log(currentUserId, "DELIVER_RESOURCE_ALLOCATION", "RESOURCES",
                String.valueOf(allocationId), "Marked allocation #" + allocationId + " as DELIVERED.");
    }

    @Override
    public void cancelAllocation(Long allocationId, Long currentUserId) {
        ResourceAllocation allocation = resourceAllocationDao.findById(allocationId)
                .orElseThrow(() -> new IllegalArgumentException("Allocation not found with id: " + allocationId));

        if (allocation.getStatus() == ResourceAllocationStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel an allocation that has already been DELIVERED.");
        }

        // If it was already dispatched, return items to inventory
        if (allocation.getStatus() == ResourceAllocationStatus.DISPATCHED) {
            InventoryItem item = inventoryDao.findById(allocation.getItemId())
                    .orElseThrow(() -> new IllegalStateException("Item no longer exists"));
            int restocked = item.getQuantityAvailable() + allocation.getQuantity();
            inventoryDao.updateQuantity(item.getId(), restocked);

            InventoryTransaction trans = new InventoryTransaction();
            trans.setItemId(item.getId());
            trans.setTransactionType(TransactionType.RECEIVED);
            trans.setQuantity(allocation.getQuantity());
            trans.setReason("Restocked due to cancellation of dispatched allocation #" + allocationId);
            trans.setPerformedByUserId(currentUserId);
            inventoryDao.saveTransaction(trans);
        }

        resourceAllocationDao.updateStatus(allocationId, ResourceAllocationStatus.CANCELLED);
        activityLogService.log(currentUserId, "CANCEL_RESOURCE_ALLOCATION", "RESOURCES",
                String.valueOf(allocationId), "Cancelled allocation #" + allocationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodDistribution> getAllFoodDistributions() {
        return foodDistributionDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodDistribution> getFoodDistributionsByShelter(Long shelterId) {
        return foodDistributionDao.findByShelterId(shelterId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FoodDistribution> getFoodDistributionById(Long id) {
        return foodDistributionDao.findById(id);
    }

    @Override
    public FoodDistribution createFoodDistribution(FoodDistributionDto dto, Long currentUserId) {
        InventoryItem item = inventoryDao.findById(dto.getItemId())
                .orElseThrow(() -> new IllegalArgumentException("Item not found with id: " + dto.getItemId()));

        if (dto.getShelterId() != null) {
            shelterDao.findById(dto.getShelterId())
                    .orElseThrow(() -> new IllegalArgumentException("Shelter not found with id: " + dto.getShelterId()));
        }

        FoodDistribution dist = new FoodDistribution();
        dist.setShelterId(dto.getShelterId());
        dist.setLocation(dto.getLocation());
        dist.setItemId(dto.getItemId());
        dist.setQuantity(dto.getQuantity());
        dist.setRecipientCount(dto.getRecipientCount());
        dist.setDistributionDate(dto.getDistributionDate());
        dist.setStatus(dto.getStatus() != null ? dto.getStatus() : DistributionStatus.PLANNED);

        // If created directly as COMPLETED, deduct stock
        if (dist.getStatus() == DistributionStatus.COMPLETED) {
            if (item.getQuantityAvailable() < dist.getQuantity()) {
                throw new IllegalStateException("Insufficient inventory to complete food distribution. In stock: " + item.getQuantityAvailable() + ", Required: " + dist.getQuantity());
            }
            int updated = item.getQuantityAvailable() - dist.getQuantity();
            inventoryDao.updateQuantity(item.getId(), updated);

            InventoryTransaction trans = new InventoryTransaction();
            trans.setItemId(item.getId());
            trans.setTransactionType(TransactionType.ISSUED);
            trans.setQuantity(dist.getQuantity());
            trans.setReason("Food distribution to " + dist.getRecipientCount() + " recipients at " + dist.getLocation());
            trans.setPerformedByUserId(currentUserId);
            inventoryDao.saveTransaction(trans);
        }

        Long id = foodDistributionDao.save(dist);
        dist.setId(id);

        activityLogService.log(currentUserId, "CREATE_FOOD_DISTRIBUTION", "RESOURCES",
                String.valueOf(id), "Recorded food distribution event: " + dist.getQuantity() + " " + item.getItemName() + " to " + dist.getRecipientCount() + " people at " + dist.getLocation());

        return dist;
    }

    @Override
    public void completeFoodDistribution(Long distributionId, Long currentUserId) {
        FoodDistribution dist = foodDistributionDao.findById(distributionId)
                .orElseThrow(() -> new IllegalArgumentException("Food distribution event not found with id: " + distributionId));

        if (dist.getStatus() == DistributionStatus.COMPLETED) {
            return;
        }

        InventoryItem item = inventoryDao.findById(dist.getItemId())
                .orElseThrow(() -> new IllegalStateException("Distribution item no longer exists"));

        if (item.getQuantityAvailable() < dist.getQuantity()) {
            throw new IllegalStateException("Insufficient stock for item '" + item.getItemName() + "'. Available: " + item.getQuantityAvailable() + ", Required: " + dist.getQuantity());
        }

        int updated = item.getQuantityAvailable() - dist.getQuantity();
        inventoryDao.updateQuantity(item.getId(), updated);

        InventoryTransaction trans = new InventoryTransaction();
        trans.setItemId(item.getId());
        trans.setTransactionType(TransactionType.ISSUED);
        trans.setQuantity(dist.getQuantity());
        trans.setReason("Food distribution event completed: " + dist.getRecipientCount() + " served at " + dist.getLocation());
        trans.setPerformedByUserId(currentUserId);
        inventoryDao.saveTransaction(trans);

        foodDistributionDao.completeDistribution(distributionId);
        activityLogService.log(currentUserId, "COMPLETE_FOOD_DISTRIBUTION", "RESOURCES",
                String.valueOf(distributionId), "Completed food distribution #" + distributionId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalItemCount() {
        return inventoryDao.count();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalQuantity() {
        return inventoryDao.getTotalQuantitySum();
    }

    @Override
    @Transactional(readOnly = true)
    public int getLowStockCount() {
        return inventoryDao.countLowStock();
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalAllocationsCount() {
        return resourceAllocationDao.count();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalRecipientsServed() {
        return foodDistributionDao.sumRecipientsServed();
    }
}
