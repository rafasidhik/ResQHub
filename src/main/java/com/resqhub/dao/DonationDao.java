package com.resqhub.dao;

import com.resqhub.model.Donation;
import com.resqhub.model.DonationStatus;
import com.resqhub.model.DonationType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DonationDao {
    Optional<Donation> findById(Long id);
    Optional<Donation> findByReference(String reference);
    List<Donation> findAll();
    List<Donation> findByEmail(String email);
    List<Donation> findByStatus(DonationStatus status);
    List<Donation> findByType(DonationType type);
    Long save(Donation donation);
    void updateStatus(Long id, DonationStatus status, Long verifiedByUserId);
    void markInventoryAdded(Long id);
    long count();
    BigDecimal sumTotalFunds();
    int sumMaterialItemsReceived();
    String generateNextReference();
}
