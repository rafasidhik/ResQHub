package com.resqhub.model;

/** A skill record attached to a rescue team. */
public class TeamSkill extends BaseEntity {

    private Long teamId;
    private String skillName;
    private String description;

    public TeamSkill() {
        super();
    }

    public TeamSkill(Long teamId, String skillName) {
        super();
        this.teamId = teamId;
        this.skillName = skillName;
    }

    @Override
    public String getDetails() {
        return skillName + (description == null ? "" : " - " + description);
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
