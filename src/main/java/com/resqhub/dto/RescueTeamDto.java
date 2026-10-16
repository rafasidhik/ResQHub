package com.resqhub.dto;

import com.resqhub.model.TeamAvailability;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RescueTeamDto {

    private Long id;

    private String teamIdentifier;

    @NotBlank(message = "Team name is required")
    @Size(min = 3, max = 100, message = "Team name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Team leader name is required")
    @Size(min = 2, max = 100, message = "Leader name must be between 2 and 100 characters")
    private String teamLeader;

    @NotBlank(message = "Contact number is required")
    private String contactNumber;

    @NotBlank(message = "Specialized skills are required")
    @Size(min = 3, max = 255, message = "Skills must be described")
    private String skills;

    @NotBlank(message = "Operating area is required")
    @Size(min = 2, max = 150, message = "Operating area must be between 2 and 150 characters")
    private String operatingArea;

    @NotNull(message = "Rescue capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1 person")
    private Integer capacity = 10;

    @NotNull(message = "Availability status is required")
    private TeamAvailability availabilityStatus = TeamAvailability.AVAILABLE;

    private String status = "ACTIVE";

    private String newMembersText; // Comma or newline separated squad members to add

    public RescueTeamDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTeamIdentifier() {
        return teamIdentifier;
    }

    public void setTeamIdentifier(String teamIdentifier) {
        this.teamIdentifier = teamIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeamLeader() {
        return teamLeader;
    }

    public void setTeamLeader(String teamLeader) {
        this.teamLeader = teamLeader;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getOperatingArea() {
        return operatingArea;
    }

    public void setOperatingArea(String operatingArea) {
        this.operatingArea = operatingArea;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public TeamAvailability getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(TeamAvailability availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNewMembersText() {
        return newMembersText;
    }

    public void setNewMembersText(String newMembersText) {
        this.newMembersText = newMembersText;
    }
}
