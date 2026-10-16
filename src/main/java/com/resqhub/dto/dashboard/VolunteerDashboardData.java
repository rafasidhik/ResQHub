package com.resqhub.dto.dashboard;

import com.resqhub.model.Volunteer;
import com.resqhub.model.VolunteerApprovalStatus;
import com.resqhub.model.VolunteerAssignment;
import com.resqhub.model.VolunteerAvailability;

import java.util.List;

public class VolunteerDashboardData {

    private Volunteer volunteer;
    private VolunteerApprovalStatus approvalStatus;
    private VolunteerAvailability availability;

    private int assignedTasksCount;
    private int inProgressTasksCount;
    private int completedTasksCount;

    private List<VolunteerAssignment> myAssignments;
    private List<VolunteerAssignment> openMissions;

    public VolunteerDashboardData() {}

    public Volunteer getVolunteer() {
        return volunteer;
    }

    public void setVolunteer(Volunteer volunteer) {
        this.volunteer = volunteer;
    }

    public VolunteerApprovalStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(VolunteerApprovalStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public VolunteerAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(VolunteerAvailability availability) {
        this.availability = availability;
    }

    public int getAssignedTasksCount() {
        return assignedTasksCount;
    }

    public void setAssignedTasksCount(int assignedTasksCount) {
        this.assignedTasksCount = assignedTasksCount;
    }

    public int getInProgressTasksCount() {
        return inProgressTasksCount;
    }

    public void setInProgressTasksCount(int inProgressTasksCount) {
        this.inProgressTasksCount = inProgressTasksCount;
    }

    public int getCompletedTasksCount() {
        return completedTasksCount;
    }

    public void setCompletedTasksCount(int completedTasksCount) {
        this.completedTasksCount = completedTasksCount;
    }

    public List<VolunteerAssignment> getMyAssignments() {
        return myAssignments;
    }

    public void setMyAssignments(List<VolunteerAssignment> myAssignments) {
        this.myAssignments = myAssignments;
    }

    public List<VolunteerAssignment> getOpenMissions() {
        return openMissions;
    }

    public void setOpenMissions(List<VolunteerAssignment> openMissions) {
        this.openMissions = openMissions;
    }
}
