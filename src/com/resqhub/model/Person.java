package com.resqhub.model;

/**
 * Abstract person shared by User accounts and Victim records
 * (and later by other teams' Volunteer / BloodDonor models).
 * Demonstrates single inheritance plus constructor chaining via this().
 */
public abstract class Person extends BaseEntity {

    protected String fullName;   // protected: subclasses access directly
    protected String phone;      // nullable

    protected Person() {
        super();
    }

    /** Constructor chaining: delegates to the full constructor. */
    protected Person(String fullName) {
        this(fullName, null);
    }

    protected Person(String fullName, String phone) {
        super();
        this.fullName = fullName;
        this.phone = phone;
    }

    public String getDisplayInfo() {
        if (phone == null || phone.isEmpty()) {
            return fullName;
        }
        return fullName + " (" + phone + ")";
    }

    @Override
    public String getDetails() {
        return getDisplayInfo();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
