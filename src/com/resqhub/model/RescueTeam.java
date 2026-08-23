package com.resqhub.model;

/** A registered rescue unit. RescueTeam -> BaseEntity. */
public class RescueTeam extends BaseEntity {

    private String teamName;
    private TeamType teamType;
    private String leaderName;
    private String contactNumber;
    private int memberCount = 1;
    private String skills;
    private String equipment;
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;
    private String baseLocation;

    public RescueTeam() {
        super();
    }

    public RescueTeam(String teamName, TeamType teamType,
                      String leaderName, String contactNumber) {
        super();
        this.teamName = teamName;
        this.teamType = teamType;
        this.leaderName = leaderName;
        this.contactNumber = contactNumber;
    }

    @Override
    public String getDetails() {
        String typeLabel = teamType == null ? "?" : teamType.getLabel();
        String avail = availabilityStatus == null ? "?" : availabilityStatus.getLabel();
        return teamName + " [" + typeLabel + "/" + avail + "] members: " + memberCount;
    }

    public boolean isAvailable() {
        return availabilityStatus == AvailabilityStatus.AVAILABLE;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public TeamType getTeamType() {
        return teamType;
    }

    public void setTeamType(TeamType teamType) {
        this.teamType = teamType;
    }

    public String getLeaderName() {
        return leaderName;
    }

    public void setLeaderName(String leaderName) {
        this.leaderName = leaderName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public AvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(AvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getBaseLocation() {
        return baseLocation;
    }

    public void setBaseLocation(String baseLocation) {
        this.baseLocation = baseLocation;
    }
}
