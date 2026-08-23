package com.resqhub.model;

/** roles.role_name values - drives role-based access control. */
public enum RoleType {
    ADMIN("Administrator"),
    RESCUE_OFFICER("Rescue Officer"),
    CAMP_MANAGER("Camp Manager"),
    MEDICAL_OFFICER("Medical Officer"),
    BLOOD_COORDINATOR("Blood Coordinator"),
    VOLUNTEER("Volunteer"),
    CITIZEN("Citizen");

    private final String label;

    RoleType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
