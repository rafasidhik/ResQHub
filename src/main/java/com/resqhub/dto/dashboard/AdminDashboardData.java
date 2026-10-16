package com.resqhub.dto.dashboard;

import com.resqhub.model.RescueRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AdminDashboardData {

    private int totalDisasters;
    private int activeDisasters;
    private int criticalDisasters;

    private int totalVictims;
    private int criticalVictims;
    private int rescuedVictims;
    private int awaitingRescueVictims;

    private int totalRescueTeams;
    private int availableRescueTeams;
    private int deployedRescueTeams;

    private int totalShelters;
    private int totalShelterCapacity;
    private int currentShelterOccupancy;
    private int shelterOccupancyPercentage;

    private int totalInventoryItems;
    private int lowStockCount;

    private int totalVolunteers;
    private int activeVolunteers;
    private int pendingVolunteers;

    private BigDecimal totalMonetaryFunds;
    private int totalMaterialUnits;

    private List<Map<String, Object>> recentActivityLogs;
    private List<RescueRequest> recentRescueRequests;

    // Chart analytics datasets
    private Map<String, Integer> disasterSeverityCounts;
    private Map<String, Integer> disasterTypeCounts;
    private List<Map<String, Object>> shelterOccupancyChartData;
    private Map<String, Integer> inventoryCategoryCounts;

    public AdminDashboardData() {
        this.totalMonetaryFunds = BigDecimal.ZERO;
    }

    public int getTotalDisasters() {
        return totalDisasters;
    }

    public void setTotalDisasters(int totalDisasters) {
        this.totalDisasters = totalDisasters;
    }

    public int getActiveDisasters() {
        return activeDisasters;
    }

    public void setActiveDisasters(int activeDisasters) {
        this.activeDisasters = activeDisasters;
    }

    public int getCriticalDisasters() {
        return criticalDisasters;
    }

    public void setCriticalDisasters(int criticalDisasters) {
        this.criticalDisasters = criticalDisasters;
    }

    public int getTotalVictims() {
        return totalVictims;
    }

    public void setTotalVictims(int totalVictims) {
        this.totalVictims = totalVictims;
    }

    public int getCriticalVictims() {
        return criticalVictims;
    }

    public void setCriticalVictims(int criticalVictims) {
        this.criticalVictims = criticalVictims;
    }

    public int getRescuedVictims() {
        return rescuedVictims;
    }

    public void setRescuedVictims(int rescuedVictims) {
        this.rescuedVictims = rescuedVictims;
    }

    public int getAwaitingRescueVictims() {
        return awaitingRescueVictims;
    }

    public void setAwaitingRescueVictims(int awaitingRescueVictims) {
        this.awaitingRescueVictims = awaitingRescueVictims;
    }

    public int getTotalRescueTeams() {
        return totalRescueTeams;
    }

    public void setTotalRescueTeams(int totalRescueTeams) {
        this.totalRescueTeams = totalRescueTeams;
    }

    public int getAvailableRescueTeams() {
        return availableRescueTeams;
    }

    public void setAvailableRescueTeams(int availableRescueTeams) {
        this.availableRescueTeams = availableRescueTeams;
    }

    public int getDeployedRescueTeams() {
        return deployedRescueTeams;
    }

    public void setDeployedRescueTeams(int deployedRescueTeams) {
        this.deployedRescueTeams = deployedRescueTeams;
    }

    public int getTotalShelters() {
        return totalShelters;
    }

    public void setTotalShelters(int totalShelters) {
        this.totalShelters = totalShelters;
    }

    public int getTotalShelterCapacity() {
        return totalShelterCapacity;
    }

    public void setTotalShelterCapacity(int totalShelterCapacity) {
        this.totalShelterCapacity = totalShelterCapacity;
    }

    public int getCurrentShelterOccupancy() {
        return currentShelterOccupancy;
    }

    public void setCurrentShelterOccupancy(int currentShelterOccupancy) {
        this.currentShelterOccupancy = currentShelterOccupancy;
    }

    public int getShelterOccupancyPercentage() {
        return shelterOccupancyPercentage;
    }

    public void setShelterOccupancyPercentage(int shelterOccupancyPercentage) {
        this.shelterOccupancyPercentage = shelterOccupancyPercentage;
    }

    public int getTotalInventoryItems() {
        return totalInventoryItems;
    }

    public void setTotalInventoryItems(int totalInventoryItems) {
        this.totalInventoryItems = totalInventoryItems;
    }

    public int getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(int lowStockCount) {
        this.lowStockCount = lowStockCount;
    }

    public int getTotalVolunteers() {
        return totalVolunteers;
    }

    public void setTotalVolunteers(int totalVolunteers) {
        this.totalVolunteers = totalVolunteers;
    }

    public int getActiveVolunteers() {
        return activeVolunteers;
    }

    public void setActiveVolunteers(int activeVolunteers) {
        this.activeVolunteers = activeVolunteers;
    }

    public int getPendingVolunteers() {
        return pendingVolunteers;
    }

    public void setPendingVolunteers(int pendingVolunteers) {
        this.pendingVolunteers = pendingVolunteers;
    }

    public BigDecimal getTotalMonetaryFunds() {
        return totalMonetaryFunds;
    }

    public void setTotalMonetaryFunds(BigDecimal totalMonetaryFunds) {
        this.totalMonetaryFunds = totalMonetaryFunds;
    }

    public int getTotalMaterialUnits() {
        return totalMaterialUnits;
    }

    public void setTotalMaterialUnits(int totalMaterialUnits) {
        this.totalMaterialUnits = totalMaterialUnits;
    }

    public List<Map<String, Object>> getRecentActivityLogs() {
        return recentActivityLogs;
    }

    public void setRecentActivityLogs(List<Map<String, Object>> recentActivityLogs) {
        this.recentActivityLogs = recentActivityLogs;
    }

    public List<RescueRequest> getRecentRescueRequests() {
        return recentRescueRequests;
    }

    public void setRecentRescueRequests(List<RescueRequest> recentRescueRequests) {
        this.recentRescueRequests = recentRescueRequests;
    }

    public Map<String, Integer> getDisasterSeverityCounts() {
        return disasterSeverityCounts;
    }

    public void setDisasterSeverityCounts(Map<String, Integer> disasterSeverityCounts) {
        this.disasterSeverityCounts = disasterSeverityCounts;
    }

    public Map<String, Integer> getDisasterTypeCounts() {
        return disasterTypeCounts;
    }

    public void setDisasterTypeCounts(Map<String, Integer> disasterTypeCounts) {
        this.disasterTypeCounts = disasterTypeCounts;
    }

    public List<Map<String, Object>> getShelterOccupancyChartData() {
        return shelterOccupancyChartData;
    }

    public void setShelterOccupancyChartData(List<Map<String, Object>> shelterOccupancyChartData) {
        this.shelterOccupancyChartData = shelterOccupancyChartData;
    }

    public Map<String, Integer> getInventoryCategoryCounts() {
        return inventoryCategoryCounts;
    }

    public void setInventoryCategoryCounts(Map<String, Integer> inventoryCategoryCounts) {
        this.inventoryCategoryCounts = inventoryCategoryCounts;
    }
}
