package com.resqhub.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.dao.RescueAssignmentDAO;
import com.resqhub.dao.RescueRequestDAO;
import com.resqhub.dao.RescueTeamDAO;
import com.resqhub.dao.VictimDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidDisasterDataException;
import com.resqhub.exception.InvalidRescueRequestException;
import com.resqhub.exception.OperationNotAllowedException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RequestStatus;
import com.resqhub.model.RescueAssignment;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RoleType;
import com.resqhub.util.ValidationUtil;

/**
 * Rescue request workflow:
 *   citizen/officer submits -> priority engine rates it -> officer assigns
 *   a team -> team progresses EN_ROUTE/ON_SITE -> completion closes the
 *   request atomically and releases the team.
 *
 * Submitting is open to EVERY logged-in role (citizens report emergencies);
 * assignment and status progression require ADMIN or RESCUE_OFFICER.
 */
public class RescueRequestService {

    private final RescueRequestDAO requestDAO = new RescueRequestDAO();
    private final RescueTeamDAO teamDAO = new RescueTeamDAO();
    private final VictimDAO victimDAO = new VictimDAO();
    private final RescueAssignmentDAO assignmentDAO = new RescueAssignmentDAO();
    private final DisasterService disasterService = new DisasterService();
    private final RescuePriorityEngine engine = new RescuePriorityEngine();
    private final SessionManager session = SessionManager.getInstance();

    public RescueRequest submitRequest(Long disasterId, Long victimId,
                                       String requesterName, String contactNumber,
                                       String location, int peopleCount,
                                       int childrenCount, int elderlyCount,
                                       boolean lifeThreatening,
                                       boolean medicalEmergency,
                                       boolean trappedUnderDebris,
                                       String requiredAssistance)
            throws UnauthorizedOperationException, InvalidRescueRequestException,
            DataAccessException {

        session.requireRole();   // any logged-in role may call for help

        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.isValidName(requesterName)) {
            errors.add("requester name is invalid");
        }
        if (!ValidationUtil.isValidPhone(contactNumber)) {
            errors.add("contact number must be 10 digits");
        }
        if (!ValidationUtil.requireNonBlank(location)) {
            errors.add("location of the emergency is required");
        }
        if (!ValidationUtil.isPositive(peopleCount)) {
            errors.add("people count must be at least 1");
        }
        if (!ValidationUtil.isNonNegative(childrenCount)
                || !ValidationUtil.isNonNegative(elderlyCount)) {
            errors.add("children and elderly counts cannot be negative");
        }
        if (ValidationUtil.isNonNegative(childrenCount)
                && ValidationUtil.isNonNegative(elderlyCount)
                && childrenCount + elderlyCount > peopleCount) {
            errors.add("children + elderly cannot exceed total people");
        }
        if (disasterId == null) {
            errors.add("the related disaster must be selected");
        }
        if (!errors.isEmpty()) {
            throw new InvalidRescueRequestException(String.join("; ", errors));
        }

        Disaster disaster = requireDisaster(disasterId);

        if (victimId != null && victimDAO.findById(victimId) == null) {
            throw new InvalidRescueRequestException(
                    "Victim #" + victimId + " does not exist");
        }

        RescueRequest request = new RescueRequest(disasterId,
                ValidationUtil.clean(requesterName), contactNumber.trim(),
                ValidationUtil.clean(location));
        request.setVictimId(victimId);
        request.setPeopleCount(peopleCount);
        request.setChildrenCount(childrenCount);
        request.setElderlyCount(elderlyCount);
        request.setLifeThreatening(lifeThreatening);
        request.setMedicalEmergency(medicalEmergency);
        request.setTrappedUnderDebris(trappedUnderDebris);
        request.setRequiredAssistance(ValidationUtil.clean(requiredAssistance));
        request.setRequestedAt(LocalDateTime.now());

        // BUSINESS LOGIC: the priority algorithm runs at submission time
        request.setPriority(engine.evaluate(request, disaster));

        return requestDAO.save(request);
    }

    /** Recomputes and persists the priority of an existing request. */
    public PriorityLevel recomputePriority(long requestId)
            throws InvalidRescueRequestException, DataAccessException {

        RescueRequest request = requireExisting(requestId);
        Disaster disaster = requireDisaster(request.getDisasterId());
        PriorityLevel priority = engine.evaluate(request, disaster);
        requestDAO.updatePriorityAndStatus(requestId, priority, request.getStatus());
        return priority;
    }

    /** Edits a request while it is still PENDING; priority is recomputed. */
    public RescueRequest updateRequest(long requestId, Long disasterId,
                                       Long victimId, String requesterName,
                                       String contactNumber, String location,
                                       int peopleCount, int childrenCount,
                                       int elderlyCount,
                                       boolean lifeThreatening,
                                       boolean medicalEmergency,
                                       boolean trappedUnderDebris,
                                       String requiredAssistance)
            throws UnauthorizedOperationException, InvalidRescueRequestException,
            OperationNotAllowedException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        RescueRequest existing = requireExisting(requestId);
        if (existing.getStatus() != RequestStatus.PENDING) {
            throw new OperationNotAllowedException("Request #" + requestId
                    + " is " + existing.getStatus().getLabel()
                    + " - only PENDING requests can be edited");
        }

        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.isValidName(requesterName)) {
            errors.add("requester name is invalid");
        }
        if (!ValidationUtil.isValidPhone(contactNumber)) {
            errors.add("contact number must be 10 digits");
        }
        if (!ValidationUtil.requireNonBlank(location)) {
            errors.add("location of the emergency is required");
        }
        if (!ValidationUtil.isPositive(peopleCount)) {
            errors.add("people count must be at least 1");
        }
        if (!ValidationUtil.isNonNegative(childrenCount)
                || !ValidationUtil.isNonNegative(elderlyCount)) {
            errors.add("children and elderly counts cannot be negative");
        }
        if (ValidationUtil.isNonNegative(childrenCount)
                && ValidationUtil.isNonNegative(elderlyCount)
                && childrenCount + elderlyCount > peopleCount) {
            errors.add("children + elderly cannot exceed total people");
        }
        if (disasterId == null) {
            errors.add("the related disaster must be selected");
        }
        if (!errors.isEmpty()) {
            throw new InvalidRescueRequestException(String.join("; ", errors));
        }
        Disaster disaster = requireDisaster(disasterId);

        existing.setDisasterId(disasterId);
        existing.setVictimId(victimId);
        existing.setRequesterName(ValidationUtil.clean(requesterName));
        existing.setContactNumber(contactNumber.trim());
        existing.setLocation(ValidationUtil.clean(location));
        existing.setPeopleCount(peopleCount);
        existing.setChildrenCount(childrenCount);
        existing.setElderlyCount(elderlyCount);
        existing.setLifeThreatening(lifeThreatening);
        existing.setMedicalEmergency(medicalEmergency);
        existing.setTrappedUnderDebris(trappedUnderDebris);
        existing.setRequiredAssistance(ValidationUtil.clean(requiredAssistance));
        existing.setPriority(engine.evaluate(existing, disaster));

        return requestDAO.save(existing);
    }

    /** Every request regardless of status - powers the history view. */
    public List<RescueRequest> getAllRequests() throws DataAccessException {
        return requestDAO.findAll();
    }

    /**
     * Aborts the live assignment of a request: assignment -> ABORTED,
     * team released, request returns to PENDING for reassignment.
     */
    public void abortAssignment(long assignmentId, String notes)
            throws UnauthorizedOperationException, InvalidRescueRequestException,
            OperationNotAllowedException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        RescueAssignment assignment = requireAssignment(assignmentId);
        AssignmentStatus current = assignment.getAssignmentStatus();
        if (current == AssignmentStatus.COMPLETED
                || current == AssignmentStatus.ABORTED) {
            throw new OperationNotAllowedException("Assignment #"
                    + assignmentId + " is already " + current.getLabel());
        }
        assignmentDAO.abortAssignment(assignmentId, notes);
    }

    /** Assignment history rows for one request (newest first). */
    public List<RescueAssignment> getAssignmentsForRequest(long requestId)
            throws UnauthorizedOperationException, InvalidRescueRequestException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        requireExisting(requestId);
        return assignmentDAO.findByRequest(requestId);
    }

    /**
     * Assigns an available rescue team to a pending request using the
     * transactional DAO method (request + team + assignment update atomically).
     */
    public long assignTeam(long requestId, long teamId)
            throws UnauthorizedOperationException, InvalidRescueRequestException,
            OperationNotAllowedException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);

        RescueRequest request = requireExisting(requestId);
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new OperationNotAllowedException("Request #" + requestId
                    + " is " + request.getStatus().getLabel()
                    + " - only PENDING requests can be assigned");
        }
        if (request.getPriority() == null) {
            recomputePriority(requestId);
        }

        RescueTeam team = teamDAO.findById(teamId);
        if (team == null) {
            throw new InvalidRescueRequestException(
                    "No rescue team with id " + teamId);
        }
        if (!team.isAvailable()) {
            throw new OperationNotAllowedException("Team "
                    + team.getTeamName() + " is currently "
                    + team.getAvailabilityStatus().getLabel());
        }

        return assignmentDAO.assignTeam(requestId, teamId, session.currentUserId());
    }

    /** Progresses ASSIGNED -> EN_ROUTE -> ON_SITE; EN_ROUTE may be skipped
     *  when a team arrives directly, but no move can go backwards. */
    public void progressAssignment(long assignmentId, AssignmentStatus target,
                                   String notes)
            throws UnauthorizedOperationException, InvalidRescueRequestException,
            OperationNotAllowedException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        validateProgression(requireAssignment(assignmentId).getAssignmentStatus(),
                target);
        assignmentDAO.updateStatus(assignmentId, target, notes);
    }

    private void validateProgression(AssignmentStatus current,
                                     AssignmentStatus target)
            throws OperationNotAllowedException {
        boolean legal =
                (current == AssignmentStatus.ASSIGNED && target == AssignmentStatus.EN_ROUTE)
             || ((current == AssignmentStatus.ASSIGNED
                     || current == AssignmentStatus.EN_ROUTE)
                     && target == AssignmentStatus.ON_SITE);

        if (!legal) {
            throw new OperationNotAllowedException("Cannot move assignment from "
                    + current.getLabel() + " to " + target.getLabel());
        }
    }

    /** Closes out a finished mission (transactional in the DAO). */
    public void completeAssignment(long assignmentId)
            throws UnauthorizedOperationException, InvalidRescueRequestException,
            OperationNotAllowedException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        RescueAssignment assignment = requireAssignment(assignmentId);

        AssignmentStatus current = assignment.getAssignmentStatus();
        if (current == AssignmentStatus.COMPLETED
                || current == AssignmentStatus.ABORTED) {
            throw new OperationNotAllowedException(
                    "Assignment already " + current.getLabel());
        }
        assignmentDAO.completeAssignment(assignmentId);
    }

    /** Cancels a request that has not been rescued yet. */
    public void cancelRequest(long requestId)
            throws UnauthorizedOperationException, InvalidRescueRequestException,
            OperationNotAllowedException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        RescueRequest request = requireExisting(requestId);

        if (request.getStatus() == RequestStatus.RESCUED
                || request.getStatus() == RequestStatus.CANCELLED) {
            throw new OperationNotAllowedException("Request #" + requestId
                    + " is already " + request.getStatus().getLabel());
        }
        if (request.getStatus() == RequestStatus.IN_PROGRESS) {
            throw new OperationNotAllowedException(
                    "A team is on site - abort the assignment instead");
        }
        requestDAO.updatePriorityAndStatus(requestId, request.getPriority(),
                RequestStatus.CANCELLED);
    }

    /** Latest assignment for a request, or null - used by the operations UI. */
    public Long getLatestAssignmentId(long requestId)
            throws InvalidRescueRequestException, DataAccessException {
        requireExisting(requestId);
        List<RescueAssignment> assignments = assignmentDAO.findByRequest(requestId);
        return assignments.isEmpty() ? null : assignments.get(0).getId();
    }

    public RescueRequest getRequest(long requestId)
            throws InvalidRescueRequestException, DataAccessException {
        return requireExisting(requestId);
    }

    public List<RescueRequest> getPendingQueue()
            throws UnauthorizedOperationException, DataAccessException {
        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        return requestDAO.findPendingByPriority();
    }

    public List<RescueRequest> getByStatus(RequestStatus status)
            throws UnauthorizedOperationException, DataAccessException {
        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER,
                RoleType.CAMP_MANAGER, RoleType.MEDICAL_OFFICER);
        return requestDAO.findByStatus(status);
    }

    public int countPending() throws DataAccessException {
        return requestDAO.countByStatus(RequestStatus.PENDING);
    }

    public String explainPriority(RescueRequest request, Long disasterId)
            throws InvalidRescueRequestException, DataAccessException {
        Disaster disaster = disasterId == null ? null
                : requireDisaster(disasterId);
        return engine.explain(request, disaster);
    }

    /** Translates disaster-domain errors into request-domain errors. */
    private Disaster requireDisaster(long disasterId)
            throws InvalidRescueRequestException, DataAccessException {
        try {
            return disasterService.requireExisting(disasterId);
        } catch (InvalidDisasterDataException e) {
            throw new InvalidRescueRequestException(e.getMessage());
        }
    }

    private RescueRequest requireExisting(long requestId)
            throws InvalidRescueRequestException, DataAccessException {
        RescueRequest request = requestDAO.findById(requestId);
        if (request == null) {
            throw new InvalidRescueRequestException(
                    "No rescue request with id " + requestId);
        }
        return request;
    }

    private RescueAssignment requireAssignment(long assignmentId)
            throws InvalidRescueRequestException, DataAccessException {
        RescueAssignment assignment = assignmentDAO.findById(assignmentId);
        if (assignment == null) {
            throw new InvalidRescueRequestException(
                    "No assignment with id " + assignmentId);
        }
        return assignment;
    }

    /** ADMIN-only hard delete; blocked while an assignment row exists.
     *  Cancelled requests that were never assigned can always be removed. */
    public void deleteRequest(long requestId)
            throws UnauthorizedOperationException, InvalidRescueRequestException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN);
        try {
            if (!requestDAO.deleteById(requestId)) {
                throw new InvalidRescueRequestException(
                        "No rescue request with id " + requestId);
            }
        } catch (DataAccessException e) {
            throw new InvalidRescueRequestException(
                    "Cannot delete request #" + requestId
                            + " - it was already assigned to a team");
        }
    }
}
