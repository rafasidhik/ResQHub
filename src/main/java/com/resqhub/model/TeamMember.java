package com.resqhub.model;

/** A member of a rescue team. */
public class TeamMember extends BaseEntity {

    private Long teamId;
    private String memberName;
    private String role;
    private String contactNumber;
    private String specialSkills;
    private AvailabilityStatus availability = AvailabilityStatus.AVAILABLE;

    public TeamMember() {
        super();
    }

    public TeamMember(Long teamId, String memberName, String role) {
        super();
        this.teamId = teamId;
        this.memberName = memberName;
        this.role = role;
    }

    @Override
    public String getDetails() {
        return memberName + " [" + role + "]";
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getSpecialSkills() {
        return specialSkills;
    }

    public void setSpecialSkills(String specialSkills) {
        this.specialSkills = specialSkills;
    }

    public AvailabilityStatus getAvailability() {
        return availability;
    }

    public void setAvailability(AvailabilityStatus availability) {
        this.availability = availability;
    }
}
