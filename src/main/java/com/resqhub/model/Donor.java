package com.resqhub.model;

/** A person, organization or company that contributes donations. */
public class Donor extends BaseEntity {

    private String fullName;
    private String contactNumber;
    private String email;
    private String location;
    private DonorType donorType = DonorType.INDIVIDUAL;

    public Donor() {
        super();
    }

    public Donor(String fullName, String location, DonorType donorType) {
        super();
        this.fullName = fullName;
        this.location = location;
        this.donorType = donorType;
    }

    @Override
    public String getDetails() {
        String type = donorType == null ? "?" : donorType.getLabel();
        return fullName + " [" + type + "] "
                + (location == null ? "" : location);
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public DonorType getDonorType() {
        return donorType;
    }

    public void setDonorType(DonorType donorType) {
        this.donorType = donorType;
    }
}
