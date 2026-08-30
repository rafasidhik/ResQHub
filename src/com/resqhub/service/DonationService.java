package com.resqhub.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.dao.DonationDAO;
import com.resqhub.dao.DonationDistributionDAO;
import com.resqhub.dao.DonorDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidDonationDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.Donation;
import com.resqhub.model.DonationDistribution;
import com.resqhub.model.DonationStatus;
import com.resqhub.model.DonationType;
import com.resqhub.model.Donor;
import com.resqhub.model.DonorType;
import com.resqhub.model.RoleType;
import com.resqhub.util.ValidationUtil;

/**
 * Donation management: donor registration, cash and material donation
 * recording, donation tracking, distribution tracking with quantity
 * validation, status lifecycles and statistics.
 * Write access: ADMIN and RESCUE_OFFICER.
 */
public class DonationService {

    private final DonorDAO donorDAO = new DonorDAO();
    private final DonationDAO donationDAO = new DonationDAO();
    private final DonationDistributionDAO distributionDAO =
            new DonationDistributionDAO();
    private final SessionManager session = SessionManager.getInstance();

    private static final int MAX_DISTRIBUTION_FOR_CASH = Integer.MAX_VALUE;

    // ── donor registration ───────────────────────────────────────────

    public Donor registerDonor(String fullName, String contactNumber,
            String email, String location, DonorType donorType)
            throws UnauthorizedOperationException,
            InvalidDonationDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);

        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(fullName)) {
            errors.add("donor name is required");
        }
        if (contactNumber != null && !contactNumber.trim().isEmpty()
                && !ValidationUtil.isValidPhone(contactNumber)) {
            errors.add("contact number must be 10 digits");
        }
        if (email != null && !email.trim().isEmpty()
                && !ValidationUtil.isValidEmail(email)) {
            errors.add("email is invalid");
        }
        if (!errors.isEmpty()) {
            throw new InvalidDonationDataException(
                    String.join("; ", errors));
        }

        if (contactNumber != null && !contactNumber.trim().isEmpty()) {
            for (Donor existing : donorDAO.findAll()) {
                if (contactNumber.trim().equals(
                        existing.getContactNumber())) {
                    throw new InvalidDonationDataException(
                            "donor already registered with contact "
                                    + contactNumber.trim());
                }
            }
        }

        Donor d = new Donor(ValidationUtil.clean(fullName),
                ValidationUtil.clean(location),
                donorType == null
                        ? DonorType.INDIVIDUAL : donorType);
        d.setContactNumber(contactNumber == null
                ? null : contactNumber.trim());
        d.setEmail(email == null ? null : email.trim());
        return donorDAO.save(d);
    }

    public Donor getDonor(long id) throws DataAccessException {
        return donorDAO.findById(id);
    }

    public Donor updateDonor(Donor donor)
            throws UnauthorizedOperationException,
            InvalidDonationDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (donor == null || donor.getId() == null) {
            throw new InvalidDonationDataException(
                    "Cannot update an unsaved donor");
        }
        if (!ValidationUtil.requireNonBlank(donor.getFullName())) {
            throw new InvalidDonationDataException(
                    "donor name is required");
        }
        if (donor.getContactNumber() != null
                && !donor.getContactNumber().isEmpty()
                && !ValidationUtil.isValidPhone(
                        donor.getContactNumber())) {
            throw new InvalidDonationDataException(
                    "contact number must be 10 digits");
        }
        return donorDAO.save(donor);
    }

    public void deleteDonor(long id)
            throws UnauthorizedOperationException,
            InvalidDonationDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN);
        Donor donor = donorDAO.findById(id);
        if (donor == null) {
            throw new InvalidDonationDataException(
                    "No donor with id " + id);
        }
        if (!donationDAO.findByDonor(id).isEmpty()) {
            throw new InvalidDonationDataException(
                    "Donor " + donor.getFullName()
                            + " has donation records and cannot be deleted");
        }
        if (!donorDAO.deleteById(id)) {
            throw new InvalidDonationDataException(
                    "Could not delete donor " + id);
        }
    }

    public List<Donor> getAllDonors() throws DataAccessException {
        return donorDAO.findAll();
    }

    public List<Donor> searchDonors(String keyword)
            throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return donorDAO.findAll();
        }
        return donorDAO.search(keyword.trim());
    }

    public List<Donor> filterDonorsByType(DonorType type)
            throws DataAccessException {
        return donorDAO.findByType(type);
    }

    // ── cash / material donation recording ───────────────────────────

    /**
     * Records a donation. For CASH the amount must be positive; for
     * MATERIAL the material name and a positive quantity are required.
     */
    public Donation recordDonation(long donorId, DonationType type,
            double amount, String materialName, int quantity,
            String description)
            throws UnauthorizedOperationException,
            InvalidDonationDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        Donor donor = donorDAO.findById(donorId);
        if (donor == null) {
            throw new InvalidDonationDataException(
                    "No donor with id " + donorId);
        }
        if (type == null) {
            throw new InvalidDonationDataException(
                    "donation type must be selected");
        }

        Donation d = new Donation();
        d.setDonorId(donorId);
        d.setDonationType(type);
        d.setDescription(ValidationUtil.clean(description));
        d.setStatus(DonationStatus.RECEIVED);
        d.setDonatedAt(java.time.LocalDateTime.now());

        if (type == DonationType.CASH) {
            if (amount <= 0) {
                throw new InvalidDonationDataException(
                        "cash amount must be greater than zero");
            }
            d.setAmount(BigDecimal.valueOf(amount)
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            if (!ValidationUtil.requireNonBlank(materialName)) {
                throw new InvalidDonationDataException(
                        "material name is required");
            }
            if (quantity <= 0) {
                throw new InvalidDonationDataException(
                        "material quantity must be greater than zero");
            }
            d.setMaterialName(ValidationUtil.clean(materialName));
            d.setQuantity(quantity);
        }
        return donationDAO.save(d);
    }

    // ── donation tracking / updates ──────────────────────────────────

    public Donation getDonation(long id) throws DataAccessException {
        return donationDAO.findById(id);
    }

    public Donation updateDonation(Donation d)
            throws UnauthorizedOperationException,
            InvalidDonationDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (d == null || d.getId() == null) {
            throw new InvalidDonationDataException(
                    "Cannot update an unsaved donation");
        }
        if (d.getDonationType() == null) {
            throw new InvalidDonationDataException(
                    "donation type must be selected");
        }
        if (d.getDonationType() == DonationType.CASH
                && (d.getAmount() == null
                    || d.getAmount().signum() <= 0)) {
            throw new InvalidDonationDataException(
                    "cash amount must be greater than zero");
        }
        if (d.getDonationType() == DonationType.MATERIAL) {
            if (!ValidationUtil.requireNonBlank(d.getMaterialName())) {
                throw new InvalidDonationDataException(
                        "material name is required");
            }
            if (d.getQuantity() == null || d.getQuantity() <= 0) {
                throw new InvalidDonationDataException(
                        "material quantity must be greater than zero");
            }
            // distribution cannot exceed the (updated) available quantity
            int distributed = distributionDAO
                    .sumDistributedByDonation(d.getId());
            if (d.getQuantity() < distributed) {
                throw new InvalidDonationDataException(
                        "quantity cannot be below already-distributed "
                                + distributed + " units");
            }
        }
        return donationDAO.save(d);
    }

    /** Records a donation status change (controllable lifecycle update). */
    public Donation updateDonationStatus(long donationId,
            DonationStatus newStatus)
            throws UnauthorizedOperationException,
            InvalidDonationDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        Donation d = donationDAO.findById(donationId);
        if (d == null) {
            throw new InvalidDonationDataException(
                    "No donation with id " + donationId);
        }
        d.setStatus(newStatus);
        return donationDAO.save(d);
    }

    public void deleteDonation(long id)
            throws UnauthorizedOperationException,
            InvalidDonationDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN);
        if (donationDAO.findById(id) == null) {
            throw new InvalidDonationDataException(
                    "No donation with id " + id);
        }
        if (!distributionDAO.findByDonation(id).isEmpty()) {
            throw new InvalidDonationDataException(
                    "Donation has distribution records and cannot be deleted");
        }
        if (!donationDAO.deleteById(id)) {
            throw new InvalidDonationDataException(
                    "Could not delete donation " + id);
        }
    }

    // ── distribution tracking ────────────────────────────────────────

    /**
     * Records a distribution of donated material. Validates that the
     * distribution quantity does not exceed what is still available, and
     * updates the donation status accordingly.
     */
    public DonationDistribution recordDistribution(long donationId,
            String distributedTo, int quantity, String description)
            throws UnauthorizedOperationException,
            InvalidDonationDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        Donation d = donationDAO.findById(donationId);
        if (d == null) {
            throw new InvalidDonationDataException(
                    "No donation with id " + donationId);
        }
        if (d.getDonationType() != DonationType.MATERIAL) {
            throw new InvalidDonationDataException(
                    "only material donations can be distributed");
        }
        if (!ValidationUtil.requireNonBlank(distributedTo)) {
            throw new InvalidDonationDataException(
                    "distribution recipient is required");
        }
        if (quantity <= 0) {
            throw new InvalidDonationDataException(
                    "distribution quantity must be greater than zero");
        }
        int totalQty = d.getQuantity() == null ? 0 : d.getQuantity();
        int alreadyDistributed =
                distributionDAO.sumDistributedByDonation(donationId);
        if (alreadyDistributed + quantity > totalQty) {
            throw new InvalidDonationDataException(
                    "Cannot distribute " + quantity + " units - only "
                            + (totalQty - alreadyDistributed)
                            + " remaining");
        }

        DonationDistribution dist = new DonationDistribution();
        dist.setDonationId(donationId);
        dist.setDistributedTo(ValidationUtil.clean(distributedTo));
        dist.setQuantity(quantity);
        dist.setDescription(ValidationUtil.clean(description));
        dist.setDistributedAt(java.time.LocalDateTime.now());
        DonationDistribution saved = distributionDAO.save(dist);

        refreshDonationStatus(d);
        return saved;
    }

    /** Recomputes donation status from how much has been distributed. */
    private void refreshDonationStatus(Donation d)
            throws DataAccessException {
        int total = d.getQuantity() == null ? 0 : d.getQuantity();
        if (total == 0) {
            return;
        }
        int distributed =
                distributionDAO.sumDistributedByDonation(d.getId());
        if (distributed == 0) {
            d.setStatus(DonationStatus.RECEIVED);
        } else if (distributed >= total) {
            d.setStatus(DonationStatus.DISTRIBUTED);
        } else {
            d.setStatus(DonationStatus.PARTIALLY_DISTRIBUTED);
        }
        donationDAO.save(d);
    }

    public List<DonationDistribution> getDistributions(long donationId)
            throws DataAccessException {
        return distributionDAO.findByDonation(donationId);
    }

    public int sumDistributed(long donationId) throws DataAccessException {
        return distributionDAO.sumDistributedByDonation(donationId);
    }

    // ── queries / reports ────────────────────────────────────────────

    public List<Donation> getAllDonations() throws DataAccessException {
        return donationDAO.findAll();
    }

    public List<Donation> searchDonations(String keyword)
            throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return donationDAO.findAll();
        }
        return donationDAO.search(keyword.trim());
    }

    public List<Donation> filterByType(DonationType type)
            throws DataAccessException {
        return donationDAO.findByType(type);
    }

    public List<Donation> filterByStatus(DonationStatus status)
            throws DataAccessException {
        return donationDAO.findByStatus(status);
    }

    public List<Donation> getDonationsForDonor(long donorId)
            throws DataAccessException {
        return donationDAO.findByDonor(donorId);
    }

    public List<Donation> getUndistributedDonations()
            throws DataAccessException {
        return donationDAO.findUndistributed();
    }

    // ── statistics ───────────────────────────────────────────────────

    public int countDonations() throws DataAccessException {
        return donationDAO.findAll().size();
    }

    public int countDonors() throws DataAccessException {
        return donorDAO.findAll().size();
    }

    public BigDecimal totalCashDonated() throws DataAccessException {
        BigDecimal total = BigDecimal.ZERO;
        for (Donation d : donationDAO.findByType(DonationType.CASH)) {
            if (d.getAmount() != null) {
                total = total.add(d.getAmount());
            }
        }
        return total;
    }

    public int countCashDonations() throws DataAccessException {
        return donationDAO.findByType(DonationType.CASH).size();
    }

    public int countMaterialDonations() throws DataAccessException {
        return donationDAO.findByType(DonationType.MATERIAL).size();
    }

    public int countDistributions() throws DataAccessException {
        return distributionDAO.findAll().size();
    }

    public int materialUnitsDistributed() throws DataAccessException {
        int total = 0;
        for (DonationDistribution dist : distributionDAO.findAll()) {
            total += dist.getQuantity();
        }
        return total;
    }

    /** Remaining (undistributed) units across all material donations. */
    public int materialUnitsRemaining() throws DataAccessException {
        int remaining = 0;
        for (Donation d : donationDAO.findByType(DonationType.MATERIAL)) {
            int total = d.getQuantity() == null ? 0 : d.getQuantity();
            int allocated =
                    distributionDAO.sumDistributedByDonation(d.getId());
            remaining += Math.max(0, total - allocated);
        }
        return remaining;
    }

    /** Per-donor summary of total cash and donation count (for profiles). */
    public DonorSummary summarizeDonor(long donorId)
            throws DataAccessException {
        List<Donation> donations = donationDAO.findByDonor(donorId);
        BigDecimal cash = BigDecimal.ZERO;
        int materialUnits = 0;
        for (Donation d : donations) {
            if (d.getDonationType() == DonationType.CASH
                    && d.getAmount() != null) {
                cash = cash.add(d.getAmount());
            } else if (d.getQuantity() != null) {
                materialUnits += d.getQuantity();
            }
        }
        return new DonorSummary(donations.size(), cash, materialUnits);
    }

    /** Aggregates a donor's donations with their donor object. */
    public DonationReport reportByDonor(long donorId)
            throws DataAccessException {
        Donor donor = donorDAO.findById(donorId);
        List<Donation> donations = donationDAO.findByDonor(donorId);
        return new DonationReport(donor, donations);
    }

    /** Compact value holder used for donor profiles. */
    public record DonorSummary(int donationCount, BigDecimal totalCash,
                               int materialUnits) {
    }

    /** Bundles a donor with their full donation history. */
    public record DonationReport(Donor donor, List<Donation> donations) {
    }
}
