package com.resqhub.model;

/** rescue_teams.team_type column. */
public enum TeamType {
    FIRE_RESCUE("Fire & Rescue"),
    MEDICAL("Medical"),
    NDRF("NDRF"),
    POLICE("Police"),
    COMMUNITY("Community"),
    OTHER("Other");

    private final String label;

    TeamType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
