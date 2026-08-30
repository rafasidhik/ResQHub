package com.resqhub.model;

import com.resqhub.util.ValidationUtil;

/**
 * A relief shelter / camp where disaster-affected victims and families
 * can stay. Capacity, occupancy and the derived available capacity are
 * the core quantities used during allocation and capacity monitoring.
 */
public class Shelter extends BaseEntity {

    private String name;
    private String code;
    private String district;
    private String city;
    private String address;
    private String locationDescription;
    private int maxCapacity;
    private int currentOccupancy;
    private String contactNumber;
    private String managerName;
    private Long disasterId;
    private boolean wheelchairAccessible;
    private boolean elderlyFriendly;
    private boolean medicalAccessible;
    private boolean specialAssistance;
    private ShelterOperationalStatus operationalStatus =
            ShelterOperationalStatus.AVAILABLE;
    private Long createdBy;

    public Shelter() {
        super();
    }

    @Override
    public String getDetails() {
        return name + " [" + (district == null ? "?" : district)
                + "] cap " + maxCapacity + " occ " + currentOccupancy
                + " / " + (operationalStatus == null ? "?" 
                        : operationalStatus.getLabel());
    }

    /** Available capacity = maximum capacity - current occupancy. */
    public int availableCapacity() {
        return maxCapacity - currentOccupancy;
    }

    public boolean isNearCapacity() {
        return maxCapacity > 0
                && (double) currentOccupancy / maxCapacity >= 0.9;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = ValidationUtil.clean(name);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = ValidationUtil.clean(code);
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = ValidationUtil.clean(district);
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = ValidationUtil.clean(city);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = ValidationUtil.clean(address);
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = ValidationUtil.clean(locationDescription);
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public int getCurrentOccupancy() {
        return currentOccupancy;
    }

    public void setCurrentOccupancy(int currentOccupancy) {
        this.currentOccupancy = currentOccupancy;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = ValidationUtil.clean(contactNumber);
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = ValidationUtil.clean(managerName);
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isElderlyFriendly() {
        return elderlyFriendly;
    }

    public void setElderlyFriendly(boolean elderlyFriendly) {
        this.elderlyFriendly = elderlyFriendly;
    }

    public boolean isMedicalAccessible() {
        return medicalAccessible;
    }

    public void setMedicalAccessible(boolean medicalAccessible) {
        this.medicalAccessible = medicalAccessible;
    }

    public boolean isSpecialAssistance() {
        return specialAssistance;
    }

    public void setSpecialAssistance(boolean specialAssistance) {
        this.specialAssistance = specialAssistance;
    }

    public ShelterOperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(ShelterOperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
