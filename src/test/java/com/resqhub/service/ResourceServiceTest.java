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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private InventoryDao inventoryDao;

    @Mock
    private ResourceAllocationDao resourceAllocationDao;

    @Mock
    private FoodDistributionDao foodDistributionDao;

    @Mock
    private DisasterDao disasterDao;

    @Mock
    private ShelterDao shelterDao;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private InventoryItem testItem;
    private Disaster testDisaster;

    @BeforeEach
    void setUp() {
        testItem = new InventoryItem();
        testItem.setId(1L);
        testItem.setItemName("Clean Water 1L");
        testItem.setCategory(ItemCategory.DRINKING_WATER);
        testItem.setUnit(ItemUnit.LITERS);
        testItem.setQuantityAvailable(100);
        testItem.setMinimumStockLevel(20);
        testItem.setStorageLocation("Bay 1");

        testDisaster = new Disaster();
        testDisaster.setId(10L);
        testDisaster.setName("River Flood");
    }

    @Test
    void testCreateItem_Success() {
        InventoryItemDto dto = new InventoryItemDto();
        dto.setItemName("Thermal Blankets");
        dto.setCategory(ItemCategory.BLANKETS);
        dto.setUnit(ItemUnit.PIECES);
        dto.setQuantityAvailable(50);
        dto.setMinimumStockLevel(10);
        dto.setStorageLocation("Warehouse A");

        when(inventoryDao.save(any(InventoryItem.class))).thenReturn(2L);

        InventoryItem created = resourceService.createItem(dto, 1L);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        verify(inventoryDao, times(1)).save(any(InventoryItem.class));
        verify(inventoryDao, times(1)).saveTransaction(any(InventoryTransaction.class));
        verify(activityLogService, times(1)).log(eq(1L), eq("CREATE_INVENTORY_ITEM"), eq("RESOURCES"), anyString(), anyString());
    }

    @Test
    void testAdjustStock_Received_IncreasesStock() {
        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setItemId(1L);
        dto.setTransactionType(TransactionType.RECEIVED);
        dto.setQuantity(50);
        dto.setReason("Donation delivery");

        when(inventoryDao.findById(1L)).thenReturn(Optional.of(testItem));

        resourceService.adjustStock(dto, 1L);

        verify(inventoryDao, times(1)).updateQuantity(1L, 150);
        verify(inventoryDao, times(1)).saveTransaction(any(InventoryTransaction.class));
        verify(activityLogService, times(1)).log(eq(1L), eq("STOCK_ADJUSTMENT"), eq("RESOURCES"), eq("1"), anyString());
    }

    @Test
    void testAdjustStock_Issued_DecreasesStock() {
        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setItemId(1L);
        dto.setTransactionType(TransactionType.ISSUED);
        dto.setQuantity(30);
        dto.setReason("Emergency dispatch");

        when(inventoryDao.findById(1L)).thenReturn(Optional.of(testItem));

        resourceService.adjustStock(dto, 1L);

        verify(inventoryDao, times(1)).updateQuantity(1L, 70);
        verify(inventoryDao, times(1)).saveTransaction(any(InventoryTransaction.class));
    }

    @Test
    void testAdjustStock_Issued_InsufficientStock_ThrowsException() {
        StockAdjustmentDto dto = new StockAdjustmentDto();
        dto.setItemId(1L);
        dto.setTransactionType(TransactionType.ISSUED);
        dto.setQuantity(150); // available is 100
        dto.setReason("Over-request");

        when(inventoryDao.findById(1L)).thenReturn(Optional.of(testItem));

        assertThrows(IllegalStateException.class, () -> resourceService.adjustStock(dto, 1L));
        verify(inventoryDao, never()).updateQuantity(anyLong(), anyInt());
    }

    @Test
    void testDispatchAllocation_Success_DeductsStockAtomically() {
        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setId(5L);
        allocation.setItemId(1L);
        allocation.setQuantity(40);
        allocation.setStatus(ResourceAllocationStatus.APPROVED);
        allocation.setDestinationLocation("Relief Post Delta");

        when(resourceAllocationDao.findById(5L)).thenReturn(Optional.of(allocation));
        when(inventoryDao.findById(1L)).thenReturn(Optional.of(testItem));

        resourceService.dispatchAllocation(5L, 1L);

        verify(inventoryDao, times(1)).updateQuantity(1L, 60);
        verify(inventoryDao, times(1)).saveTransaction(any(InventoryTransaction.class));
        verify(resourceAllocationDao, times(1)).updateStatus(5L, ResourceAllocationStatus.DISPATCHED);
    }

    @Test
    void testDispatchAllocation_InsufficientStock_ThrowsException() {
        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setId(5L);
        allocation.setItemId(1L);
        allocation.setQuantity(120); // available is 100
        allocation.setStatus(ResourceAllocationStatus.APPROVED);

        when(resourceAllocationDao.findById(5L)).thenReturn(Optional.of(allocation));
        when(inventoryDao.findById(1L)).thenReturn(Optional.of(testItem));

        assertThrows(IllegalStateException.class, () -> resourceService.dispatchAllocation(5L, 1L));
        verify(inventoryDao, never()).updateQuantity(anyLong(), anyInt());
    }

    @Test
    void testCancelAllocation_WhenDispatched_RestocksItem() {
        ResourceAllocation allocation = new ResourceAllocation();
        allocation.setId(5L);
        allocation.setItemId(1L);
        allocation.setQuantity(40);
        allocation.setStatus(ResourceAllocationStatus.DISPATCHED);

        when(resourceAllocationDao.findById(5L)).thenReturn(Optional.of(allocation));
        when(inventoryDao.findById(1L)).thenReturn(Optional.of(testItem));

        resourceService.cancelAllocation(5L, 1L);

        // Restocks 100 + 40 = 140
        verify(inventoryDao, times(1)).updateQuantity(1L, 140);
        verify(inventoryDao, times(1)).saveTransaction(any(InventoryTransaction.class));
        verify(resourceAllocationDao, times(1)).updateStatus(5L, ResourceAllocationStatus.CANCELLED);
    }

    @Test
    void testCreateFoodDistribution_Completed_DeductsStock() {
        FoodDistributionDto dto = new FoodDistributionDto();
        dto.setItemId(1L);
        dto.setQuantity(20);
        dto.setRecipientCount(50);
        dto.setLocation("Central Feeding Center");
        dto.setDistributionDate(LocalDate.now());
        dto.setStatus(DistributionStatus.COMPLETED);

        when(inventoryDao.findById(1L)).thenReturn(Optional.of(testItem));
        when(foodDistributionDao.save(any(FoodDistribution.class))).thenReturn(10L);

        FoodDistribution created = resourceService.createFoodDistribution(dto, 1L);

        assertNotNull(created);
        assertEquals(10L, created.getId());
        verify(inventoryDao, times(1)).updateQuantity(1L, 80);
        verify(inventoryDao, times(1)).saveTransaction(any(InventoryTransaction.class));
        verify(foodDistributionDao, times(1)).save(any(FoodDistribution.class));
    }

    @Test
    void testDeleteItem_WithActiveAllocations_ThrowsException() {
        ResourceAllocation activeAllocation = new ResourceAllocation();
        activeAllocation.setItemId(1L);
        activeAllocation.setStatus(ResourceAllocationStatus.PLANNED);

        when(inventoryDao.findById(1L)).thenReturn(Optional.of(testItem));
        when(resourceAllocationDao.findAll()).thenReturn(List.of(activeAllocation));

        assertThrows(IllegalStateException.class, () -> resourceService.deleteItem(1L, 1L));
        verify(inventoryDao, never()).delete(1L);
    }
}
