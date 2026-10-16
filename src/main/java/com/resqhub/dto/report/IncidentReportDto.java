package com.resqhub.dto.report;

import com.resqhub.model.*;

import java.util.Collections;
import java.util.List;

public class IncidentReportDto {

    private Disaster disaster;
    private int totalVictimsCount;
    private int rescuedVictimsCount;
    private int criticalVictimsCount;
    private int totalRescueRequestsCount;
    private int completedRescueRequestsCount;
    private int activeShelterResidentsCount;
    private int deployedRescueSquadsCount;

    private List<Victim> victims = Collections.emptyList();
    private List<RescueRequest> rescueRequests = Collections.emptyList();
    private List<ShelterResident> shelterResidents = Collections.emptyList();
    private List<VolunteerAssignment> volunteerAssignments = Collections.emptyList();

    public IncidentReportDto() {
    }

    public Disaster getDisaster() {
        return disaster;
    }

    public void setDisaster(Disaster disaster) {
        this.disaster = disaster;
    }

    public int getTotalVictimsCount() {
        return totalVictimsCount;
    }

    public void setTotalVictimsCount(int totalVictimsCount) {
        this.totalVictimsCount = totalVictimsCount;
    }

    public int getRescuedVictimsCount() {
        return rescuedVictimsCount;
    }

    public void setRescuedVictimsCount(int rescuedVictimsCount) {
        this.rescuedVictimsCount = rescuedVictimsCount;
    }

    public int getCriticalVictimsCount() {
        return criticalVictimsCount;
    }

    public void setCriticalVictimsCount(int criticalVictimsCount) {
        this.criticalVictimsCount = criticalVictimsCount;
    }

    public int getTotalRescueRequestsCount() {
        return totalRescueRequestsCount;
    }

    public void setTotalRescueRequestsCount(int totalRescueRequestsCount) {
        this.totalRescueRequestsCount = totalRescueRequestsCount;
    }

    public int getCompletedRescueRequestsCount() {
        return completedRescueRequestsCount;
    }

    public void setCompletedRescueRequestsCount(int completedRescueRequestsCount) {
        this.completedRescueRequestsCount = completedRescueRequestsCount;
    }

    public int getActiveShelterResidentsCount() {
        return activeShelterResidentsCount;
    }

    public void setActiveShelterResidentsCount(int activeShelterResidentsCount) {
        this.activeShelterResidentsCount = activeShelterResidentsCount;
    }

    public int getDeployedRescueSquadsCount() {
        return deployedRescueSquadsCount;
    }

    public void setDeployedRescueSquadsCount(int deployedRescueSquadsCount) {
        this.deployedRescueSquadsCount = deployedRescueSquadsCount;
    }

    public List<Victim> getVictims() {
        return victims;
    }

    public void setVictims(List<Victim> victims) {
        this.victims = victims;
    }

    public List<RescueRequest> getRescueRequests() {
        return rescueRequests;
    }

    public void setRescueRequests(List<RescueRequest> rescueRequests) {
        this.rescueRequests = rescueRequests;
    }

    public List<ShelterResident> getShelterResidents() {
        return shelterResidents;
    }

    public void setShelterResidents(List<ShelterResident> shelterResidents) {
        this.shelterResidents = shelterResidents;
    }

    public List<VolunteerAssignment> getVolunteerAssignments() {
        return volunteerAssignments;
    }

    public void setVolunteerAssignments(List<VolunteerAssignment> volunteerAssignments) {
        this.volunteerAssignments = volunteerAssignments;
    }
}
