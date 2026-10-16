package com.resqhub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ResidentCheckInDto {

    @NotNull(message = "Shelter is required")
    private Long shelterId;

    @NotNull(message = "Associated disaster is required")
    private Long disasterId;

    private Long victimId;

    @NotBlank(message = "Primary resident name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String residentName;

    @NotNull(message = "Family size is required")
    @Min(value = 1, message = "Family size must be at least 1 person")
    private Integer familySize = 1;

    public ResidentCheckInDto() {
    }

    public Long getShelterId() {
        return shelterId;
    }

    public void setShelterId(Long shelterId) {
        this.shelterId = shelterId;
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

    public String getResidentName() {
        return residentName;
    }

    public void setResidentName(String residentName) {
        this.residentName = residentName;
    }

    public Integer getFamilySize() {
        return familySize;
    }

    public void setFamilySize(Integer familySize) {
        this.familySize = familySize;
    }
}
