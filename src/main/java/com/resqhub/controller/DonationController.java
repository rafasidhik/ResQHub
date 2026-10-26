package com.resqhub.controller;

import java.math.BigDecimal;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.Donation;
import com.resqhub.model.DonationDistribution;
import com.resqhub.model.DonationStatus;
import com.resqhub.model.DonationType;
import com.resqhub.model.Donor;
import com.resqhub.model.DonorType;
import com.resqhub.service.DonationService;
import com.resqhub.util.InputParser;

/** Donation management screen controller. */
public class DonationController {

    private final DonationService donationService =
            new DonationService();

    // ── donors ───────────────────────────────────────────────────────

    public ActionResult registerDonor(String fullName,
            String contactNumber, String email, String location,
            DonorType donorType) {
        try {
            Donor d = donationService.registerDonor(fullName,
                    contactNumber, email, location, donorType);
            return ActionResult.successWithData(
                    "Donor registered as #" + d.getId()
                            + " (" + d.getDonorType().getLabel() + ")",
                    d);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateDonor(long id, String fullName,
            String contactNumber, String email, String location,
            DonorType donorType) {
        try {
            Donor existing = donationService.getDonor(id);
            if (existing == null) {
                return ActionResult.failure(
                        "No donor with id " + id);
            }
            existing.setFullName(fullName);
            existing.setContactNumber(contactNumber);
            existing.setEmail(email);
            existing.setLocation(location);
            existing.setDonorType(donorType);
            Donor saved = donationService.updateDonor(existing);
            return ActionResult.success(
                    "Donor #" + saved.getId() + " updated");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteDonor(long id) {
        try {
            donationService.deleteDonor(id);
            return ActionResult.success("Donor #" + id + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public Donor getDonor(long id) throws ResQHubException {
        return donationService.getDonor(id);
    }

    public List<Donor> getAllDonors() throws DataAccessException {
        return donationService.getAllDonors();
    }

    public List<Donor> searchDonors(String keyword)
            throws DataAccessException {
        return donationService.searchDonors(keyword);
    }

    // ── donations ────────────────────────────────────────────────────

    public ActionResult recordDonation(long donorId,
            DonationType type, String amountText, String materialName,
            String quantityText, String description) {
        try {
            double amount = InputParser.parseAmount(amountText, "Amount");
            int quantity = InputParser.parseInt(quantityText, "Quantity");
            Donation d = donationService.recordDonation(donorId, type,
                    amount, materialName, quantity, description);
            return ActionResult.successWithData(
                    "Donation recorded as #" + d.getId()
                            + " (" + d.getDetails() + ")",
                    d);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateDonation(long id, DonationType type,
            String amountText, String materialName, String quantityText,
            String description) {
        try {
            Donation existing = donationService.getDonation(id);
            if (existing == null) {
                return ActionResult.failure(
                        "No donation with id " + id);
            }
            double amount = InputParser.parseAmount(amountText, "Amount");
            int quantity = InputParser.parseInt(quantityText, "Quantity");
            existing.setDonationType(type);
            existing.setAmount(amount <= 0
                    ? null : BigDecimal.valueOf(amount));
            existing.setMaterialName(materialName);
            existing.setQuantity(quantity <= 0
                    ? null : quantity);
            existing.setDescription(description);
            Donation saved = donationService.updateDonation(existing);
            return ActionResult.success(
                    "Donation #" + saved.getId() + " updated");
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateDonationStatus(long id,
            DonationStatus status) {
        try {
            Donation d = donationService.updateDonationStatus(id, status);
            return ActionResult.success("Donation #" + d.getId()
                    + " is now " + d.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteDonation(long id) {
        try {
            donationService.deleteDonation(id);
            return ActionResult.success("Donation #" + id + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public Donation getDonation(long id) throws ResQHubException {
        return donationService.getDonation(id);
    }

    public List<Donation> getAllDonations() throws DataAccessException {
        return donationService.getAllDonations();
    }

    public List<Donation> searchDonations(String keyword)
            throws DataAccessException {
        return donationService.searchDonations(keyword);
    }

    public List<Donation> getDonationsForDonor(long donorId)
            throws DataAccessException {
        return donationService.getDonationsForDonor(donorId);
    }

    // ── distribution ─────────────────────────────────────────────────

    public ActionResult recordDistribution(long donationId,
            String distributedTo, String quantityText,
            String description) {
        try {
            int quantity = InputParser.parseInt(quantityText,
                    "Distribution quantity");
            DonationDistribution dist = donationService
                    .recordDistribution(donationId, distributedTo,
                            quantity, description);
            return ActionResult.successWithData(
                    "Distributed " + quantity + " unit(s) to "
                            + dist.getDistributedTo(),
                    dist);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public List<DonationDistribution> getDistributions(long donationId)
            throws DataAccessException {
        return donationService.getDistributions(donationId);
    }

    public int sumDistributed(long donationId) throws DataAccessException {
        return donationService.sumDistributed(donationId);
    }

    // ── reports / stats ──────────────────────────────────────────────

    public DonationService.DonorSummary summarizeDonor(long donorId)
            throws DataAccessException {
        return donationService.summarizeDonor(donorId);
    }

    public int countDonations() throws DataAccessException {
        return donationService.countDonations();
    }

    public int countDonors() throws DataAccessException {
        return donationService.countDonors();
    }

    public BigDecimal totalCashDonated() throws DataAccessException {
        return donationService.totalCashDonated();
    }

    public int countDistributions() throws DataAccessException {
        return donationService.countDistributions();
    }

    public int materialUnitsDistributed() throws DataAccessException {
        return donationService.materialUnitsDistributed();
    }

    public int materialUnitsRemaining() throws DataAccessException {
        return donationService.materialUnitsRemaining();
    }

    // ── display helpers ──────────────────────────────────────────────

    public static Object[] toDonorRow(Donor d) {
        return new Object[]{
                d.getId(),
                d.getFullName(),
                d.getDonorType() == null
                        ? "-" : d.getDonorType().getLabel(),
                d.getContactNumber() == null ? "-" : d.getContactNumber(),
                d.getEmail() == null ? "-" : d.getEmail(),
                d.getLocation() == null ? "-" : d.getLocation()
        };
    }

    public static String[] donorTableHeaders() {
        return new String[]{"ID", "Name", "Type", "Contact",
                "Email", "Location"};
    }

    public static Object[] toDonationRow(Donation d) {
        String what;
        if (d.getDonationType() == DonationType.CASH) {
            what = "\u20B9" + (d.getAmount() == null
                    ? "0" : d.getAmount());
        } else {
            what = (d.getMaterialName() == null ? "-" : d.getMaterialName())
                    + " x" + (d.getQuantity() == null
                            ? "?" : d.getQuantity());
        }
        String when = d.getDonatedAt() == null
                ? "-" : d.getDonatedAt().toLocalDate().toString();
        return new Object[]{
                d.getId(),
                (d.getDonationType() == null
                        ? "-" : d.getDonationType().getLabel()),
                what,
                d.getStatus() == null ? "-" : d.getStatus().getLabel(),
                when
        };
    }

    public static String[] donationTableHeaders() {
        return new String[]{"ID", "Type", "Item / Amount",
                "Status", "Date"};
    }

    public static Object[] toDistributionRow(DonationDistribution dist) {
        String when = dist.getDistributedAt() == null
                ? "-" : dist.getDistributedAt().toLocalDate().toString();
        return new Object[]{
                dist.getId(),
                dist.getDistributedTo(),
                dist.getQuantity(),
                dist.getDescription() == null
                        ? "-" : dist.getDescription(),
                when
        };
    }

    public static String[] distributionTableHeaders() {
        return new String[]{"ID", "Distributed To", "Qty",
                "Description", "Date"};
    }
}
