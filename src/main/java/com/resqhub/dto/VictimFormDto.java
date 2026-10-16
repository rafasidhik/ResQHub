package com.resqhub.dto;

import com.resqhub.model.RescueStatus;
import com.resqhub.model.VictimPriority;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VictimFormDto {

    private Long id;

    @NotNull(message = "Associated disaster is required")
    private Long disasterId;

    @NotBlank(message = "Victim full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    private Integer age;

    private String contactNumber;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 200, message = "Location must be between 2 and 200 characters")
    private String location;

    @NotNull(message = "Number of affected family members is required")
    @Min(value = 1, message = "Affected family members must be at least 1")
    private Integer affectedFamilyMembers = 1;

    private String accessibilityNeeds;

    private String currentSituation;

    @NotNull(message = "Rescue status is required")
    private RescueStatus rescueStatus = RescueStatus.AWAITING_RESCUE;

    @NotNull(message = "Priority level is required")
    private VictimPriority priority = VictimPriority.MEDIUM;

    public VictimFormDto() {
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getAffectedFamilyMembers() {
        return affectedFamilyMembers;
    }

    public void setAffectedFamilyMembers(Integer affectedFamilyMembers) {
        this.affectedFamilyMembers = affectedFamilyMembers;
    }

    public String getAccessibilityNeeds() {
        return accessibilityNeeds;
    }

    public void setAccessibilityNeeds(String accessibilityNeeds) {
        this.accessibilityNeeds = accessibilityNeeds;
    }

    public String getCurrentSituation() {
        return currentSituation;
    }

    public void setCurrentSituation(String currentSituation) {
        this.currentSituation = currentSituation;
    }

    public RescueStatus getRescueStatus() {
        return rescueStatus;
    }

    public void setRescueStatus(RescueStatus rescueStatus) {
        this.rescueStatus = rescueStatus;
    }

    public VictimPriority getPriority() {
        return priority;
    }

    public void setPriority(VictimPriority priority) {
        this.priority = priority;
    }
}
