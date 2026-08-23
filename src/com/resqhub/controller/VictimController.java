package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.Gender;
import com.resqhub.model.ShelterStatus;
import com.resqhub.model.Victim;
import com.resqhub.service.VictimService;
import com.resqhub.util.InputParser;

/** Victim screen controller. */
public class VictimController {

    private final VictimService victimService = new VictimService();

    public ActionResult registerVictim(String fullName, String ageText,
                                       Gender gender, String phone,
                                       EmergencyStatus emergencyStatus,
                                       String medicalCondition,
                                       String familyInfo, String currentLocation,
                                       Long disasterId) {
        try {
            int age = InputParser.parseInt(ageText, "Age");
            Victim victim = victimService.registerVictim(fullName, age, gender,
                    phone, emergencyStatus, medicalCondition, familyInfo,
                    currentLocation, disasterId);
            return ActionResult.successWithData(
                    "Victim registered as #" + victim.getId()
                            + " (" + victim.getEmergencyStatus().getLabel() + ")",
                    victim);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateEmergencyStatus(long victimId,
                                              EmergencyStatus status) {
        try {
            Victim updated = victimService.updateEmergencyStatus(victimId, status);
            return ActionResult.success("Victim #" + updated.getId()
                    + " status set to " + updated.getEmergencyStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /** Called by Ameya's shelter-allocation screen after successful allocation. */
    public ActionResult markShelterStatus(long victimId, ShelterStatus status) {
        try {
            Victim updated = victimService.markShelterStatus(victimId, status);
            return ActionResult.success("Victim #" + updated.getId()
                    + " marked " + updated.getShelterStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteVictim(long victimId) {
        try {
            victimService.deleteVictim(victimId);
            return ActionResult.success("Victim #" + victimId + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error deleting victim: "
                    + e.getMessage());
        }
    }

    public List<Victim> getVictimsByDisaster(long disasterId)
            throws DataAccessException {
        return victimService.getVictimsByDisaster(disasterId);
    }

    public List<Victim> getAllVictims() throws DataAccessException {
        return victimService.getAllVictims();
    }

    public static Object[] toRow(Victim v) {
        return new Object[] {
                v.getId(),
                v.getFullName(),
                v.getAge(),
                v.getGender() == null ? "-" : v.getGender().getLabel(),
                v.getEmergencyStatus().getLabel(),
                v.getCurrentLocation(),
                v.getDisasterId()
        };
    }

    public static String[] tableHeaders() {
        return new String[] {"ID", "Name", "Age", "Gender", "Status",
                "Location", "Disaster #"};
    }
}
