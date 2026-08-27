package com.resqhub.model;

/** A skill record attached to a volunteer. */
public class VolunteerSkill extends BaseEntity {

    private Long volunteerId;
    private String skillName;

    public VolunteerSkill() {
        super();
    }

    public VolunteerSkill(Long volunteerId, String skillName) {
        super();
        this.volunteerId = volunteerId;
        this.skillName = skillName;
    }

    @Override
    public String getDetails() {
        return skillName;
    }

    public Long getVolunteerId() {
        return volunteerId;
    }

    public void setVolunteerId(Long volunteerId) {
        this.volunteerId = volunteerId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }
}
