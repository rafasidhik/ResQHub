package com.resqhub.dto.dashboard;

import com.resqhub.model.Disaster;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.Shelter;

import java.util.List;

public class CitizenDashboardData {

    private int myRequestsCount;
    private int activeDisastersCount;
    private int openSheltersCount;

    private List<RescueRequest> myRequests;
    private List<Shelter> openShelters;
    private List<Disaster> activeDisasters;

    public CitizenDashboardData() {}

    public int getMyRequestsCount() {
        return myRequestsCount;
    }

    public void setMyRequestsCount(int myRequestsCount) {
        this.myRequestsCount = myRequestsCount;
    }

    public int getActiveDisastersCount() {
        return activeDisastersCount;
    }

    public void setActiveDisastersCount(int activeDisastersCount) {
        this.activeDisastersCount = activeDisastersCount;
    }

    public int getOpenSheltersCount() {
        return openSheltersCount;
    }

    public void setOpenSheltersCount(int openSheltersCount) {
        this.openSheltersCount = openSheltersCount;
    }

    public List<RescueRequest> getMyRequests() {
        return myRequests;
    }

    public void setMyRequests(List<RescueRequest> myRequests) {
        this.myRequests = myRequests;
    }

    public List<Shelter> getOpenShelters() {
        return openShelters;
    }

    public void setOpenShelters(List<Shelter> openShelters) {
        this.openShelters = openShelters;
    }

    public List<Disaster> getActiveDisasters() {
        return activeDisasters;
    }

    public void setActiveDisasters(List<Disaster> activeDisasters) {
        this.activeDisasters = activeDisasters;
    }
}
