package com.resqhub.dto.dashboard;

import com.resqhub.model.FoodDistribution;
import com.resqhub.model.InventoryItem;
import com.resqhub.model.Shelter;

import java.util.List;

public class CampManagerDashboardData {

    private int totalSheltersCount;
    private int totalCapacity;
    private int currentOccupancy;
    private int occupancyPercentage;

    private int lowStockItemsCount;
    private int todayDistributionsCount;
    private int totalRecipientsServed;

    private List<Shelter> sheltersList;
    private List<InventoryItem> lowStockItems;
    private List<FoodDistribution> recentDistributions;

    public CampManagerDashboardData() {}

    public int getTotalSheltersCount() {
        return totalSheltersCount;
    }

    public void setTotalSheltersCount(int totalSheltersCount) {
        this.totalSheltersCount = totalSheltersCount;
    }

    public int getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(int totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public int getCurrentOccupancy() {
        return currentOccupancy;
    }

    public void setCurrentOccupancy(int currentOccupancy) {
        this.currentOccupancy = currentOccupancy;
    }

    public int getOccupancyPercentage() {
        return occupancyPercentage;
    }

    public void setOccupancyPercentage(int occupancyPercentage) {
        this.occupancyPercentage = occupancyPercentage;
    }

    public int getLowStockItemsCount() {
        return lowStockItemsCount;
    }

    public void setLowStockItemsCount(int lowStockItemsCount) {
        this.lowStockItemsCount = lowStockItemsCount;
    }

    public int getTodayDistributionsCount() {
        return todayDistributionsCount;
    }

    public void setTodayDistributionsCount(int todayDistributionsCount) {
        this.todayDistributionsCount = todayDistributionsCount;
    }

    public int getTotalRecipientsServed() {
        return totalRecipientsServed;
    }

    public void setTotalRecipientsServed(int totalRecipientsServed) {
        this.totalRecipientsServed = totalRecipientsServed;
    }

    public List<Shelter> getSheltersList() {
        return sheltersList;
    }

    public void setSheltersList(List<Shelter> sheltersList) {
        this.sheltersList = sheltersList;
    }

    public List<InventoryItem> getLowStockItems() {
        return lowStockItems;
    }

    public void setLowStockItems(List<InventoryItem> lowStockItems) {
        this.lowStockItems = lowStockItems;
    }

    public List<FoodDistribution> getRecentDistributions() {
        return recentDistributions;
    }

    public void setRecentDistributions(List<FoodDistribution> recentDistributions) {
        this.recentDistributions = recentDistributions;
    }
}
