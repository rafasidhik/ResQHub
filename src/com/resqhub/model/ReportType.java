package com.resqhub.model;

/**
 * The report families the Reports &amp; Analytics module can generate.
 * Each maps to a SQL aggregation over the ResQHub tables, exercising
 * COUNT / SUM / AVG / MIN / MAX / GROUP BY / HAVING / JOIN, filtering
 * and sorting.
 */
public enum ReportType {
    OVERVIEW("Dashboard Overview"),
    DISASTERS("Active Disaster Report"),
    VICTIMS("Victim Statistics"),
    RESCUE_REQUESTS("Rescue Request Statistics"),
    RESCUE_PERFORMANCE("Rescue Performance Report"),
    VOLUNTEERS("Volunteer Reports"),
    DONATIONS("Donation Statistics"),
    SHELTER_OCCUPANCY("Shelter Occupancy Report"),
    ALLOCATION_OVERVIEW("Shelter Allocation Overview"),
    HOSPITAL_CAPACITY("Hospital Capacity Report"),
    BLOOD_AVAILABILITY("Blood Availability Report"),
    RESOURCE_INVENTORY("Resource & Inventory Report"),
    FOOD_DISTRIBUTION("Food Distribution Report");

    private final String label;

    ReportType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
