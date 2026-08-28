package com.resqhub.model;

/**
 * Emergency facilities a hospital can offer (spec section 8). Used both to
 * record what each hospital has and to express what an emergency case needs
 * so the system can match a suitable hospital.
 */
public enum HospitalFacility {
    EMERGENCY_DEPARTMENT("Emergency Department"),
    TRAUMA_CARE("Trauma Care"),
    ICU("ICU"),
    AMBULANCE_SUPPORT("Ambulance Support"),
    SURGERY_FACILITIES("Surgery Facilities"),
    BLOOD_BANK("Blood Bank"),
    VENTILATOR_SUPPORT("Ventilator Support");

    private final String label;

    HospitalFacility(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
