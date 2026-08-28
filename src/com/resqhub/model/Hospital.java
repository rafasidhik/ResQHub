package com.resqhub.model;

import java.util.LinkedHashSet;
import java.util.Set;

import com.resqhub.util.ValidationUtil;

/**
 * A hospital registered for emergency response (spec section 1). Tracks
 * contact / location details, total bed capacity, current occupancy and the
 * emergency facilities on offer. Available beds are derived, never stored:
 *
 *   availableBeds = totalBeds - occupiedBeds
 */
public class Hospital extends BaseEntity {

    private String name;
    private String hospitalId;
    private String district;
    private String city;
    private String area;
    private String address;
    private String phone;
    private String emergencyContact;
    private String email;
    private int totalBeds;
    private int occupiedBeds;
    private Set<HospitalFacility> facilities = new LinkedHashSet<>();
    private HospitalStatus status = HospitalStatus.AVAILABLE;
    private Long disasterId;
    private Long createdBy;

    public Hospital() {
        super();
    }

    @Override
    public String getDetails() {
        return name + " [" + (district == null ? "?" : district)
                + "] beds " + occupiedBeds + "/" + totalBeds
                + " av " + availableBeds() + " ("
                + (status == null ? "?" : status.getLabel()) + ")";
    }

    /** Derived: beds currently free to accept patients. */
    public int availableBeds() {
        return Math.max(0, totalBeds - occupiedBeds);
    }

    /** Derived utilisation percentage (0-100). */
    public int utilisationPercent() {
        if (totalBeds <= 0) {
            return 0;
        }
        return Math.round(occupiedBeds * 100f / totalBeds);
    }

    /** True when 90% or more of beds are occupied (near capacity). */
    public boolean isNearCapacity() {
        return totalBeds > 0
                && ((double) occupiedBeds / totalBeds) >= 0.9;
    }

    /** Emergency facilities joined by comma for display. */
    public String facilitiesSummary() {
        if (facilities == null || facilities.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (HospitalFacility f : facilities) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(f.getLabel());
        }
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = ValidationUtil.clean(name);
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = ValidationUtil.clean(hospitalId);
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

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = ValidationUtil.clean(area);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = ValidationUtil.clean(address);
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = ValidationUtil.clean(phone);
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = ValidationUtil.clean(emergencyContact);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = ValidationUtil.clean(email);
    }

    public int getTotalBeds() {
        return totalBeds;
    }

    public void setTotalBeds(int totalBeds) {
        this.totalBeds = totalBeds;
    }

    public int getOccupiedBeds() {
        return occupiedBeds;
    }

    public void setOccupiedBeds(int occupiedBeds) {
        this.occupiedBeds = occupiedBeds;
    }

    public Set<HospitalFacility> getFacilities() {
        return facilities;
    }

    public void setFacilities(Set<HospitalFacility> facilities) {
        this.facilities = facilities == null
                ? new LinkedHashSet<>() : new LinkedHashSet<>(facilities);
    }

    public HospitalStatus getStatus() {
        return status;
    }

    public void setStatus(HospitalStatus status) {
        this.status = status;
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }
}
