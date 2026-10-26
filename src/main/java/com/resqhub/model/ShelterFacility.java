package com.resqhub.model;

import com.resqhub.util.ValidationUtil;

/**
 * A facility available at a shelter (drinking water, food, medical
 * support, toilets, electricity, first-aid, sleeping arrangements...).
 * Child of {@link Shelter}, stored in the shelter_facilities table.
 */
public class ShelterFacility extends BaseEntity {

    private Long shelterId;
    private String facilityName;
    private boolean available = true;

    public ShelterFacility() {
        super();
    }

    @Override
    public String getDetails() {
        return facilityName + (available ? " (available)" : " (unavailable)");
    }

    public Long getShelterId() {
        return shelterId;
    }

    public void setShelterId(Long shelterId) {
        this.shelterId = shelterId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = ValidationUtil.clean(facilityName);
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
