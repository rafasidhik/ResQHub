package com.resqhub.service;

import com.resqhub.dao.DonationDao;
import com.resqhub.dao.InventoryDao;
import com.resqhub.dto.DonationFormDto;
import com.resqhub.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationDao donationDao;

    @Mock
    private InventoryDao inventoryDao;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private DonationServiceImpl donationService;

    private InventoryItem testItem;
    private Donation testMaterialDonation;

    @BeforeEach
    void setUp() {
        testItem = new InventoryItem();
        testItem.setId(10L);
        testItem.setItemName("Emergency Dry Rations Pack");
        testItem.setQuantityAvailable(100);
        testItem.setMinimumStockLevel(20);

        testMaterialDonation = new Donation();
        testMaterialDonation.setId(1L);
        testMaterialDonation.setDonationReference("DON-2026-0001");
        testMaterialDonation.setDonorName("Generous Donor");
        testMaterialDonation.setDonorEmail("donor@example.com");
        testMaterialDonation.setDonationType(DonationType.MATERIAL);
        testMaterialDonation.setItemId(10L);
        testMaterialDonation.setItemQuantity(50);
        testMaterialDonation.setStatus(DonationStatus.VERIFIED);
        testMaterialDonation.setInventoryAdded(false);
    }

    @Test
    void testSubmitMonetaryDonation_Success() {
        DonationFormDto dto = new DonationFormDto();
        dto.setDonorName("Alice Donor");
        dto.setDonorEmail("alice@example.com");
        dto.setDonationType(DonationType.MONETARY);
        dto.setAmount(new BigDecimal("150.00"));

        when(donationDao.generateNextReference()).thenReturn("DON-2026-0002");
        when(donationDao.save(any(Donation.class))).thenReturn(2L);

        Donation created = donationService.submitDonation(dto, 1L);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("DON-2026-0002", created.getDonationReference());
        assertEquals(DonationStatus.SUBMITTED, created.getStatus());
        verify(donationDao, times(1)).save(any(Donation.class));
        verify(activityLogService, times(1)).log(eq(1L), eq("SUBMIT_DONATION"), eq("DONATIONS"), anyString(), anyString());
    }

    @Test
    void testSubmitMaterialDonation_Success() {
        DonationFormDto dto = new DonationFormDto();
        dto.setDonorName("Bob Contributor");
        dto.setDonorEmail("bob@example.com");
        dto.setDonationType(DonationType.MATERIAL);
        dto.setItemId(10L);
        dto.setItemQuantity(25);

        when(inventoryDao.findById(10L)).thenReturn(Optional.of(testItem));
        when(donationDao.generateNextReference()).thenReturn("DON-2026-0003");
        when(donationDao.save(any(Donation.class))).thenReturn(3L);

        Donation created = donationService.submitDonation(dto, 1L);

        assertNotNull(created);
        assertEquals(3L, created.getId());
        assertEquals(DonationType.MATERIAL, created.getDonationType());
        assertEquals(25, created.getItemQuantity());
    }

    @Test
    void testSubmitMonetaryDonation_InvalidAmount_ThrowsException() {
        DonationFormDto dto = new DonationFormDto();
        dto.setDonorName("Zero Donor");
        dto.setDonorEmail("zero@example.com");
        dto.setDonationType(DonationType.MONETARY);
        dto.setAmount(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class, () -> donationService.submitDonation(dto, 1L));
        verify(donationDao, never()).save(any(Donation.class));
    }

    @Test
    void testVerifyDonation_Success() {
        when(donationDao.findById(1L)).thenReturn(Optional.of(testMaterialDonation));

        donationService.verifyDonation(1L, 1L);

        verify(donationDao, times(1)).updateStatus(1L, DonationStatus.VERIFIED, 1L);
        verify(activityLogService, times(1)).log(eq(1L), eq("VERIFY_DONATION"), eq("DONATIONS"), eq("DON-2026-0001"), anyString());
    }

    @Test
    void testReceiveMaterialDonation_Success_IncrementsInventory() {
        when(donationDao.findById(1L)).thenReturn(Optional.of(testMaterialDonation));
        when(inventoryDao.findById(10L)).thenReturn(Optional.of(testItem));

        donationService.receiveMaterialDonation(1L, 1L);

        // 100 in stock + 50 donated = 150
        verify(inventoryDao, times(1)).updateQuantity(10L, 150);
        verify(inventoryDao, times(1)).saveTransaction(any(InventoryTransaction.class));
        verify(donationDao, times(1)).markInventoryAdded(1L);
        verify(donationDao, times(1)).updateStatus(1L, DonationStatus.RECEIVED, 1L);
        verify(activityLogService, times(1)).log(eq(1L), eq("RECEIVE_MATERIAL_DONATION"), eq("DONATIONS"), anyString(), anyString());
    }

    @Test
    void testReceiveMaterialDonation_AlreadyAdded_ThrowsException() {
        testMaterialDonation.setInventoryAdded(true);
        when(donationDao.findById(1L)).thenReturn(Optional.of(testMaterialDonation));

        assertThrows(IllegalStateException.class, () -> donationService.receiveMaterialDonation(1L, 1L));
        verify(inventoryDao, never()).updateQuantity(anyLong(), anyInt());
    }

    @Test
    void testCancelDonation_RollsBackStockIfAdded() {
        testMaterialDonation.setInventoryAdded(true);
        when(donationDao.findById(1L)).thenReturn(Optional.of(testMaterialDonation));
        when(inventoryDao.findById(10L)).thenReturn(Optional.of(testItem));

        donationService.cancelDonation(1L, 1L);

        // 100 - 50 = 50 rollback
        verify(inventoryDao, times(1)).updateQuantity(10L, 50);
        verify(inventoryDao, times(1)).saveTransaction(any(InventoryTransaction.class));
        verify(donationDao, times(1)).updateStatus(1L, DonationStatus.CANCELLED, 1L);
    }
}
