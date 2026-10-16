package com.resqhub.dto;

import com.resqhub.model.RescueTeam;

import java.util.ArrayList;
import java.util.List;

public class TeamRecommendation {

    private RescueTeam team;
    private int score; // Weighted score out of 100
    private boolean eligible;
    private List<String> matchingFactors = new ArrayList<>();
    private List<String> constraintWarnings = new ArrayList<>();

    public TeamRecommendation() {
    }

    public TeamRecommendation(RescueTeam team, int score, boolean eligible) {
        this.team = team;
        this.score = score;
        this.eligible = eligible;
    }

    public RescueTeam getTeam() {
        return team;
    }

    public void setTeam(RescueTeam team) {
        this.team = team;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }

    public List<String> getMatchingFactors() {
        return matchingFactors;
    }

    public void setMatchingFactors(List<String> matchingFactors) {
        this.matchingFactors = matchingFactors;
    }

    public void addFactor(String factor) {
        this.matchingFactors.add(factor);
    }

    public List<String> getConstraintWarnings() {
        return constraintWarnings;
    }

    public void setConstraintWarnings(List<String> constraintWarnings) {
        this.constraintWarnings = constraintWarnings;
    }

    public void addWarning(String warning) {
        this.constraintWarnings.add(warning);
    }
}
