package com.resqhub.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.dao.DisasterDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidDisasterDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.model.RoleType;
import com.resqhub.util.ValidationUtil;

/**
 * Disaster lifecycle management. Write operations are restricted to
 * ADMIN and RESCUE_OFFICER; reads are open to every logged-in role.
 */
public class DisasterService {

    private final DisasterDAO disasterDAO = new DisasterDAO();
    private final SessionManager session = SessionManager.getInstance();

    public Disaster createDisaster(String title, DisasterType type,
                                   DisasterSeverity severity, String location,
                                   int affectedPopulation, LocalDateTime start,
                                   LocalDateTime end, String description)
            throws UnauthorizedOperationException, InvalidDisasterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);

        Disaster disaster = new Disaster(ValidationUtil.clean(title), type,
                severity, ValidationUtil.clean(location), start);
        disaster.setAffectedPopulation(affectedPopulation);
        disaster.setEndDateTime(end);
        disaster.setDescription(ValidationUtil.clean(description));

        validate(disaster);
        disaster.setReportedBy(session.currentUserId());
        return disasterDAO.save(disaster);
    }

    public Disaster updateDisaster(Disaster disaster)
            throws UnauthorizedOperationException, InvalidDisasterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (disaster == null || disaster.getId() == null) {
            throw new InvalidDisasterDataException("Cannot update an unsaved disaster");
        }
        validate(disaster);
        return disasterDAO.save(disaster);
    }

    /** Marks a disaster resolved, stamping its end date. */
    public Disaster closeDisaster(long disasterId)
            throws UnauthorizedOperationException, InvalidDisasterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        Disaster disaster = requireExisting(disasterId);
        if (disaster.getStatus() == DisasterStatus.RESOLVED) {
            throw new InvalidDisasterDataException(
                    "Disaster #" + disasterId + " is already resolved");
        }
        disaster.setStatus(DisasterStatus.RESOLVED);
        disaster.setEndDateTime(LocalDateTime.now());
        return disasterDAO.save(disaster);
    }

    private void validate(Disaster d) throws InvalidDisasterDataException {
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(d.getTitle()) || d.getTitle().length() < 5) {
            errors.add("title must be at least 5 characters");
        }
        if (d.getDisasterType() == null) {
            errors.add("disaster type must be selected");
        }
        if (d.getSeverity() == null) {
            errors.add("severity must be selected");
        }
        if (!ValidationUtil.requireNonBlank(d.getLocation())) {
            errors.add("location is required");
        }
        if (!ValidationUtil.isNonNegative(d.getAffectedPopulation())) {
            errors.add("affected population cannot be negative");
        }
        if (d.getStartDateTime() == null) {
            errors.add("start date/time is required");
        }
        if (!ValidationUtil.isChronological(d.getStartDateTime(), d.getEndDateTime())) {
            errors.add("end date must not be before the start date");
        }
        if (!errors.isEmpty()) {
            throw new InvalidDisasterDataException(String.join("; ", errors));
        }
    }

    /** Loads a disaster or throws - shared by victim and rescue services. */
    public Disaster requireExisting(long disasterId)
            throws InvalidDisasterDataException, DataAccessException {
        Disaster disaster = disasterDAO.findById(disasterId);
        if (disaster == null) {
            throw new InvalidDisasterDataException(
                    "No disaster with id " + disasterId);
        }
        return disaster;
    }

    public List<Disaster> getAllDisasters() throws DataAccessException {
        return disasterDAO.findAll();
    }

    public List<Disaster> getActiveDisasters() throws DataAccessException {
        return disasterDAO.findByStatus(DisasterStatus.ACTIVE);
    }

    public List<Disaster> search(String keyword) throws DataAccessException {
        if (ValidationUtil.clean(keyword) == null
                || keyword.trim().isEmpty()) {
            return getAllDisasters();
        }
        return disasterDAO.search(keyword.trim());
    }

    /** ADMIN-only hard delete; blocked while victims or rescue requests
     *  still reference the disaster (FK RESTRICT). */
    public void deleteDisaster(long disasterId)
            throws UnauthorizedOperationException, InvalidDisasterDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN);
        try {
            if (!disasterDAO.deleteById(disasterId)) {
                throw new InvalidDisasterDataException(
                        "No disaster with id " + disasterId);
            }
        } catch (DataAccessException e) {
            throw new InvalidDisasterDataException(
                    "Cannot delete disaster #" + disasterId
                            + " - victims or rescue requests still reference it");
        }
    }
}
