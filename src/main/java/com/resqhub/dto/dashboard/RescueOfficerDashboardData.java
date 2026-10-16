package com.resqhub.dto.dashboard;

import com.resqhub.model.RescueAssignment;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueTeam;

import java.util.List;

public class RescueOfficerDashboardData {

    private int pendingRequestsCount;
    private int inProgressMissionsCount;
    private int availableTeamsCount;
    private int deployedTeamsCount;
    private int criticalVictimsCount;

    private List<RescueRequest> urgentRequests;
    private List<RescueTeam> activeTeams;
    private List<RescueAssignment> recentAssignments;

    public RescueOfficerDashboardData() {}

    public int getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public void setPendingRequestsCount(int pendingRequestsCount) {
        this.pendingRequestsCount = pendingRequestsCount;
    }

    public int getInProgressMissionsCount() {
        return inProgressMissionsCount;
    }

    public void setInProgressMissionsCount(int inProgressMissionsCount) {
        this.inProgressMissionsCount = inProgressMissionsCount;
    }

    public int getAvailableTeamsCount() {
        return availableTeamsCount;
    }

    public void setAvailableTeamsCount(int availableTeamsCount) {
        this.availableTeamsCount = availableTeamsCount;
    }

    public int getDeployedTeamsCount() {
        return deployedTeamsCount;
    }

    public void setDeployedTeamsCount(int deployedTeamsCount) {
        this.deployedTeamsCount = deployedTeamsCount;
    }

    public int getCriticalVictimsCount() {
        return criticalVictimsCount;
    }

    public void setCriticalVictimsCount(int criticalVictimsCount) {
        this.criticalVictimsCount = criticalVictimsCount;
    }

    public List<RescueRequest> getUrgentRequests() {
        return urgentRequests;
    }

    public void setUrgentRequests(List<RescueRequest> urgentRequests) {
        this.urgentRequests = urgentRequests;
    }

    public List<RescueTeam> getActiveTeams() {
        return activeTeams;
    }

    public void setActiveTeams(List<RescueTeam> activeTeams) {
        this.activeTeams = activeTeams;
    }

    public List<RescueAssignment> getRecentAssignments() {
        return recentAssignments;
    }

    public void setRecentAssignments(List<RescueAssignment> recentAssignments) {
        this.recentAssignments = recentAssignments;
    }
}
