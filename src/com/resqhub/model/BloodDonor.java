package com.resqhub.model;

import java.time.LocalDate;

import com.resqhub.util.ValidationUtil;

/**
 * Registered blood donor: personal + contact info, blood group, location,
 * availability, eligibility and last-donation date (spec: Blood Donor
 * Registration / Donor Profile).
 */
public class BloodDonor extends BaseEntity {

    private String fullName;
    private BloodGroup bloodGroup;
    private String location;
    private String phone;
    private String email;
    private DonorAvailability availability = DonorAvailability.AVAILABLE;
    private LocalDate lastDonationDate;
    private DonorEligibility eligibility = DonorEligibility.TEMPORARY_DEFERRED;
    private String notes;
    private Long registeredBy;

    public BloodDonor() {
        super();
    }

    public BloodDonor(String fullName, BloodGroup bloodGroup, String location) {
        super();
        this.fullName = ValidationUtil.clean(fullName);
        this.bloodGroup = bloodGroup;
        this.location = ValidationUtil.clean(location);
    }

    /** A donor is currently suitable to be offered for a request. */
    public boolean isSuitable() {
        return availability == DonorAvailability.AVAILABLE
                && eligibility == DonorEligibility.ELIGIBLE
                && bloodGroup != null;
    }

    @Override
    public String getDetails() {
        return fullName + " [" + (bloodGroup == null ? "?" : bloodGroup.getLabel())
                + "] @" + (location == null ? "?" : location)
                + " (" + (availability == null ? "?" : availability.getLabel())
                + ", " + (eligibility == null ? "?" : eligibility.getLabel())
                + ")";
    }

    // ---- accessors ----------------------------------------------------

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = ValidationUtil.clean(fullName); }
    public BloodGroup getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(BloodGroup bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = ValidationUtil.clean(location); }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = ValidationUtil.clean(phone); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = ValidationUtil.clean(email); }
    public DonorAvailability getAvailability() { return availability; }
    public void setAvailability(DonorAvailability availability) { this.availability = availability; }
    public LocalDate getLastDonationDate() { return lastDonationDate; }
    public void setLastDonationDate(LocalDate lastDonationDate) { this.lastDonationDate = lastDonationDate; }
    public DonorEligibility getEligibility() { return eligibility; }
    public void setEligibility(DonorEligibility eligibility) { this.eligibility = eligibility; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = ValidationUtil.clean(notes); }
    public Long getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(Long registeredBy) { this.registeredBy = registeredBy; }
}
