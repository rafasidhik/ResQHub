package com.resqhub.service;

import com.resqhub.dto.DonationFormDto;
import com.resqhub.model.Donation;
import com.resqhub.model.DonationStatus;
import com.resqhub.model.DonationType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DonationService {
    List<Donation> getAllDonations();
    List<Donation> getDonationsByStatus(DonationStatus status);
    List<Donation> getDonationsByType(DonationType type);
    List<Donation> getDonationsByEmail(String email);
    Optional<Donation> getDonationById(Long id);
    Optional<Donation> getDonationByReference(String reference);

    Donation submitDonation(DonationFormDto dto, Long currentUserId);
    void verifyDonation(Long donationId, Long currentUserId);
    void receiveMaterialDonation(Long donationId, Long currentUserId);
    void cancelDonation(Long donationId, Long currentUserId);

    long getTotalDonationCount();
    BigDecimal getTotalMonetaryFunds();
    int getTotalMaterialUnits();
}
