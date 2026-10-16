package com.resqhub.service;

import com.resqhub.dao.DonationDao;
import com.resqhub.dao.InventoryDao;
import com.resqhub.dto.DonationFormDto;
import com.resqhub.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class DonationServiceImpl implements DonationService {

    private final DonationDao donationDao;
    private final InventoryDao inventoryDao;
    private final ActivityLogService activityLogService;

    @Autowired
    public DonationServiceImpl(DonationDao donationDao,
                               InventoryDao inventoryDao,
                               ActivityLogService activityLogService) {
        this.donationDao = donationDao;
        this.inventoryDao = inventoryDao;
        this.activityLogService = activityLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donation> getAllDonations() {
        return donationDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donation> getDonationsByStatus(DonationStatus status) {
        return donationDao.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donation> getDonationsByType(DonationType type) {
        return donationDao.findByType(type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Donation> getDonationsByEmail(String email) {
        return donationDao.findByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Donation> getDonationById(Long id) {
        return donationDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Donation> getDonationByReference(String reference) {
        return donationDao.findByReference(reference);
    }

    @Override
    public Donation submitDonation(DonationFormDto dto, Long currentUserId) {
        Donation donation = new Donation();
        donation.setDonationReference(donationDao.generateNextReference());
        donation.setDonorName(dto.getDonorName());
        donation.setDonorEmail(dto.getDonorEmail());
        donation.setDonorPhone(dto.getDonorPhone());
        donation.setDonationType(dto.getDonationType());
        donation.setDonationDate(LocalDate.now());
        donation.setStatus(DonationStatus.SUBMITTED);
        donation.setNotes(dto.getNotes());
        donation.setInventoryAdded(false);

        if (dto.getDonationType() == DonationType.MONETARY) {
            if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Monetary donation requires a valid positive amount.");
            }
            donation.setAmount(dto.getAmount());
        } else if (dto.getDonationType() == DonationType.MATERIAL) {
            if (dto.getItemId() == null || dto.getItemQuantity() == null || dto.getItemQuantity() <= 0) {
                throw new IllegalArgumentException("Material donation requires selecting a relief item and entering a positive quantity.");
            }
            inventoryDao.findById(dto.getItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid inventory item selected: " + dto.getItemId()));

            donation.setItemId(dto.getItemId());
            donation.setItemQuantity(dto.getItemQuantity());
        }

        Long id = donationDao.save(donation);
        donation.setId(id);

        String detail = (donation.getDonationType() == DonationType.MONETARY)
                ? "$" + donation.getAmount()
                : donation.getItemQuantity() + " unit(s) of relief supplies";

        activityLogService.log(currentUserId, "SUBMIT_DONATION", "DONATIONS",
                donation.getDonationReference(), "Donation recorded from " + donation.getDonorName() + " (" + detail + ")");

        return donation;
    }

    @Override
    public void verifyDonation(Long donationId, Long currentUserId) {
        Donation donation = donationDao.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found with id: " + donationId));

        donationDao.updateStatus(donationId, DonationStatus.VERIFIED, currentUserId);
        activityLogService.log(currentUserId, "VERIFY_DONATION", "DONATIONS",
                donation.getDonationReference(), "Verified donation " + donation.getDonationReference() + " from " + donation.getDonorName());
    }

    @Override
    public void receiveMaterialDonation(Long donationId, Long currentUserId) {
        Donation donation = donationDao.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found with id: " + donationId));

        if (donation.getDonationType() != DonationType.MATERIAL) {
            throw new IllegalStateException("Only MATERIAL donations can be received into inventory.");
        }

        if (donation.isInventoryAdded()) {
            throw new IllegalStateException("Items for donation " + donation.getDonationReference() + " have already been received into inventory.");
        }

        InventoryItem item = inventoryDao.findById(donation.getItemId())
                .orElseThrow(() -> new IllegalStateException("Donated inventory item no longer exists."));

        // Atomically increment stock
        int newQuantity = item.getQuantityAvailable() + donation.getItemQuantity();
        inventoryDao.updateQuantity(item.getId(), newQuantity);

        // Record stock ledger transaction
        InventoryTransaction trans = new InventoryTransaction();
        trans.setItemId(item.getId());
        trans.setTransactionType(TransactionType.RECEIVED);
        trans.setQuantity(donation.getItemQuantity());
        trans.setReason("Donation delivery received (" + donation.getDonationReference() + ") from " + donation.getDonorName());
        trans.setPerformedByUserId(currentUserId);
        inventoryDao.saveTransaction(trans);

        donationDao.markInventoryAdded(donationId);
        donationDao.updateStatus(donationId, DonationStatus.RECEIVED, currentUserId);

        activityLogService.log(currentUserId, "RECEIVE_MATERIAL_DONATION", "DONATIONS",
                donation.getDonationReference(), "Received " + donation.getItemQuantity() + " " + item.getItemName() + " into inventory from " + donation.getDonorName());
    }

    @Override
    public void cancelDonation(Long donationId, Long currentUserId) {
        Donation donation = donationDao.findById(donationId)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found with id: " + donationId));

        if (donation.isInventoryAdded()) {
            // Roll back inventory addition if material supplies were already deposited
            inventoryDao.findById(donation.getItemId()).ifPresent(item -> {
                int rolledBack = Math.max(0, item.getQuantityAvailable() - donation.getItemQuantity());
                inventoryDao.updateQuantity(item.getId(), rolledBack);

                InventoryTransaction trans = new InventoryTransaction();
                trans.setItemId(item.getId());
                trans.setTransactionType(TransactionType.ADJUSTED);
                trans.setQuantity(donation.getItemQuantity());
                trans.setReason("Reversal due to cancelled donation " + donation.getDonationReference());
                trans.setPerformedByUserId(currentUserId);
                inventoryDao.saveTransaction(trans);
            });
        }

        donationDao.updateStatus(donationId, DonationStatus.CANCELLED, currentUserId);
        activityLogService.log(currentUserId, "CANCEL_DONATION", "DONATIONS",
                donation.getDonationReference(), "Cancelled donation " + donation.getDonationReference());
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalDonationCount() {
        return donationDao.count();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalMonetaryFunds() {
        return donationDao.sumTotalFunds();
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalMaterialUnits() {
        return donationDao.sumMaterialItemsReceived();
    }
}
