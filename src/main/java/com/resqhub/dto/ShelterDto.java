package com.resqhub.dto;

import com.resqhub.model.ShelterStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ShelterDto {

    private Long id;

    private String shelterIdentifier;

    @NotBlank(message = "Shelter name is required")
    @Size(min = 3, max = 150, message = "Name must be between 3 and 150 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(min = 3, max = 255, message = "Address must be between 3 and 255 characters")
    private String address;

    private String locationDescription;

    @NotBlank(message = "Camp manager / contact person is required")
    @Size(min = 2, max = 100, message = "Contact person name must be between 2 and 100 characters")
    private String contactPerson;

    @NotBlank(message = "Contact phone number is required")
    private String contactNumber;

    @NotNull(message = "Total capacity is required")
    @Min(value = 1, message = "Shelter capacity must be at least 1 person")
    private Integer totalCapacity = 50;

    @NotNull(message = "Shelter status is required")
    private ShelterStatus status = ShelterStatus.AVAILABLE;

    public ShelterDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShelterIdentifier() {
        return shelterIdentifier;
    }

    public void setShelterIdentifier(String shelterIdentifier) {
        this.shelterIdentifier = shelterIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Integer getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Integer totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public ShelterStatus getStatus() {
        return status;
    }

    public void setStatus(ShelterStatus status) {
        this.status = status;
    }
}
