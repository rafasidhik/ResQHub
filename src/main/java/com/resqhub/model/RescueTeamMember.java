package com.resqhub.model;

import java.time.LocalDateTime;

public class RescueTeamMember {
    private Long id;
    private Long teamId;
    private String memberName;
    private String roleTitle;
    private String contactNumber;
    private String specialty;
    private LocalDateTime createdAt;

    public RescueTeamMember() {
    }

    public RescueTeamMember(Long teamId, String memberName, String roleTitle, String contactNumber, String specialty) {
        this.teamId = teamId;
        this.memberName = memberName;
        this.roleTitle = roleTitle;
        this.contactNumber = contactNumber;
        this.specialty = specialty;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
