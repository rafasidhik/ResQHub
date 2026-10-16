package com.resqhub.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VolunteerRegistrationDto {
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(min = 7, max = 30, message = "Please enter a valid phone number")
    private String phone;

    @NotBlank(message = "Operating area / home district is required")
    @Size(max = 150, message = "Operating area must not exceed 150 characters")
    private String operatingArea;

    @NotBlank(message = "Skills and capabilities are required")
    @Size(min = 3, max = 255, message = "Please list relevant skills (e.g. First Aid, Food Prep, Driving, Medical Support)")
    private String skills;

    @Min(value = 0, message = "Experience years cannot be negative")
    private int experienceYears = 0;

    @NotBlank(message = "Emergency contact name and phone are required")
    @Size(max = 100, message = "Emergency contact must not exceed 100 characters")
    private String emergencyContact;

    public VolunteerRegistrationDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getOperatingArea() {
        return operatingArea;
    }

    public void setOperatingArea(String operatingArea) {
        this.operatingArea = operatingArea;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = experienceYears;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}
