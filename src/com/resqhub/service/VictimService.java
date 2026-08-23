package com.resqhub.service;

import java.util.ArrayList;
import java.util.List;

import com.resqhub.dao.DisasterDAO;
import com.resqhub.dao.VictimDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidVictimDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.Gender;
import com.resqhub.model.RoleType;
import com.resqhub.model.ShelterStatus;
import com.resqhub.model.Victim;
import com.resqhub.util.ValidationUtil;

/**
 * Victim registration and status tracking.
 * Write access: ADMIN, CAMP_MANAGER, RESCUE_OFFICER.
 */
public class VictimService {

    private final VictimDAO victimDAO = new VictimDAO();
    private final DisasterDAO disasterDAO = new DisasterDAO();
    private final SessionManager session = SessionManager.getInstance();

    public Victim registerVictim(String fullName, int age, Gender gender,
                                 String phone, EmergencyStatus emergencyStatus,
                                 String medicalCondition, String familyInfo,
                                 String currentLocation, Long disasterId)
            throws UnauthorizedOperationException, InvalidVictimDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.CAMP_MANAGER,
                RoleType.RESCUE_OFFICER);

        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.isValidName(fullName)) {
            errors.add("victim name is invalid");
        }
        if (!ValidationUtil.isValidAge(age)) {
            errors.add("age must be between 0 and 130");
        }
        if (gender == null) {
            errors.add("gender must be selected");
        }
        if (!ValidationUtil.requireNonBlank(currentLocation)) {
            errors.add("current location is required");
        }
        String cleanPhone = ValidationUtil.clean(phone);
        if (cleanPhone != null && !cleanPhone.isEmpty()
                && !ValidationUtil.isValidPhone(cleanPhone)) {
            errors.add("phone must be 10 digits");
        }
        if (disasterId == null) {
            errors.add("the disaster must be selected");
        }
        if (!errors.isEmpty()) {
            throw new InvalidVictimDataException(String.join("; ", errors));
        }

        if (disasterDAO.findById(disasterId) == null) {
            throw new InvalidVictimDataException(
                    "No disaster with id " + disasterId);
        }

        Victim victim = new Victim(ValidationUtil.clean(fullName), age, gender,
                cleanPhone == null || cleanPhone.isEmpty() ? null : cleanPhone);
        victim.setEmergencyStatus(emergencyStatus == null
                ? EmergencyStatus.SAFE : emergencyStatus);
        victim.setMedicalCondition(ValidationUtil.clean(medicalCondition));
        victim.setFamilyInfo(ValidationUtil.clean(familyInfo));
        victim.setCurrentLocation(ValidationUtil.clean(currentLocation));
        victim.setDisasterId(disasterId);
        victim.setRegisteredBy(session.currentUserId());

        return victimDAO.save(victim);
    }

    /** Updates the emergency triage state of a victim. */
    public Victim updateEmergencyStatus(long victimId, EmergencyStatus status)
            throws UnauthorizedOperationException, InvalidVictimDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.CAMP_MANAGER,
                RoleType.RESCUE_OFFICER);
        Victim victim = requireExisting(victimId);
        victim.setEmergencyStatus(status);
        return victimDAO.save(victim);
    }

    /**
     * Shelter flag update. Ameya's shelter-allocation service will call
     * this after a successful allocation in her module.
     */
    public Victim markShelterStatus(long victimId, ShelterStatus status)
            throws UnauthorizedOperationException, InvalidVictimDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.CAMP_MANAGER,
                RoleType.RESCUE_OFFICER);
        Victim victim = requireExisting(victimId);
        victim.setShelterStatus(status);
        return victimDAO.save(victim);
    }

    private Victim requireExisting(long victimId)
            throws InvalidVictimDataException, DataAccessException {
        Victim victim = victimDAO.findById(victimId);
        if (victim == null) {
            throw new InvalidVictimDataException("No victim with id " + victimId);
        }
        return victim;
    }

    public List<Victim> getVictimsByDisaster(long disasterId)
            throws DataAccessException {
        return victimDAO.findByDisaster(disasterId);
    }

    public List<Victim> getAllVictims() throws DataAccessException {
        return victimDAO.findAll();
    }
}
