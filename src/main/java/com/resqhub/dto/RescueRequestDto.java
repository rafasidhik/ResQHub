package com.resqhub.dto;

import com.resqhub.model.Urgency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RescueRequestDto {

    private Long id;

    @NotNull(message = "Associated disaster is required")
    private Long disasterId;

    private Long victimId; // Optional link to victim

    @NotBlank(message = "Location of stranded people is required")
    @Size(min = 3, max = 255, message = "Location must be between 3 and 255 characters")
    private String location;

    @NotBlank(message = "Situation description is required")
    private String description;

    @NotBlank(message = "Contact phone number is required")
    private String contactNumber;

    @NotNull(message = "Number of people requiring assistance is required")
    @Min(value = 1, message = "At least 1 person must require assistance")
    private Integer peopleCount = 1;

    @NotNull(message = "Urgency level is required")
    private Urgency urgency = Urgency.MEDIUM;

    public RescueRequestDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDisasterId() {
        return disasterId;
    }

    public void setDisasterId(Long disasterId) {
        this.disasterId = disasterId;
    }

    public Long getVictimId() {
        return victimId;
    }

    public void setVictimId(Long victimId) {
        this.victimId = victimId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public Integer getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(Integer peopleCount) {
        this.peopleCount = peopleCount;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }
}
