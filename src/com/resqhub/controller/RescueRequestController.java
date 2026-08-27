package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.AssignmentStatus;
import com.resqhub.model.RequestStatus;
import com.resqhub.model.RescueRequest;
import com.resqhub.service.RescueRequestService;
import com.resqhub.util.InputParser;

/**
 * Rescue request screen controller - the operations desk.
 * Covers citizen submission plus officer assignment workflow.
 */
public class RescueRequestController {

    private final RescueRequestService requestService = new RescueRequestService();

    public ActionResult submitRequest(Long disasterId, Long victimId,
                                      String requesterName, String contactNumber,
                                      String location, String peopleText,
                                      String childrenText, String elderlyText,
                                      boolean lifeThreatening,
                                      boolean medicalEmergency,
                                      boolean trappedUnderDebris,
                                      String requiredAssistance) {
        try {
            int people = InputParser.parseInt(peopleText, "People count");
            int children = childrenText == null || childrenText.trim().isEmpty()
                    ? 0 : InputParser.parseInt(childrenText, "Children count");
            int elderly = elderlyText == null || elderlyText.trim().isEmpty()
                    ? 0 : InputParser.parseInt(elderlyText, "Elderly count");

            RescueRequest saved = requestService.submitRequest(disasterId,
                    victimId, requesterName, contactNumber, location,
                    people, children, elderly,
                    lifeThreatening, medicalEmergency, trappedUnderDebris,
                    requiredAssistance);
            return ActionResult.successWithData(
                    "Request #" + saved.getId() + " submitted with "
                            + saved.getPriority().getLabel() + " priority",
                    saved);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult assignTeam(long requestId, long teamId) {
        try {
            long assignmentId = requestService.assignTeam(requestId, teamId);
            return ActionResult.success("Team assigned (assignment #"
                    + assignmentId + "). Request is now ASSIGNED.");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult progressAssignment(long assignmentId,
                                           AssignmentStatus target, String notes) {
        try {
            requestService.progressAssignment(assignmentId, target, notes);
            return ActionResult.success("Assignment #" + assignmentId
                    + " is now " + target.getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult completeAssignment(long assignmentId) {
        try {
            requestService.completeAssignment(assignmentId);
            return ActionResult.success("Assignment #" + assignmentId
                    + " completed. Team released and request RESCUED.");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult cancelRequest(long requestId) {
        try {
            requestService.cancelRequest(requestId);
            return ActionResult.success("Request #" + requestId + " cancelled");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateRequest(long requestId, Long disasterId,
                                      Long victimId, String requesterName,
                                      String contactNumber, String location,
                                      String peopleText, String childrenText,
                                      String elderlyText,
                                      boolean lifeThreatening,
                                      boolean medicalEmergency,
                                      boolean trappedUnderDebris,
                                      String requiredAssistance) {
        try {
            int people = InputParser.parseInt(peopleText, "People count");
            int children = childrenText == null || childrenText.trim().isEmpty()
                    ? 0 : InputParser.parseInt(childrenText, "Children count");
            int elderly = elderlyText == null || elderlyText.trim().isEmpty()
                    ? 0 : InputParser.parseInt(elderlyText, "Elderly count");

            requestService.updateRequest(requestId, disasterId, victimId,
                    requesterName, contactNumber, location, people, children,
                    elderly, lifeThreatening, medicalEmergency,
                    trappedUnderDebris, requiredAssistance);
            return ActionResult.success("Request #" + requestId + " updated");
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult abortAssignment(long assignmentId, String notes) {
        try {
            requestService.abortAssignment(assignmentId, notes);
            return ActionResult.success("Assignment #" + assignmentId
                    + " aborted. Team released and request back to PENDING.");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /** All requests regardless of status (history browsing). */
    public List<RescueRequest> getAllRequests() throws DataAccessException {
        return requestService.getAllRequests();
    }

    /** Assignment history of one request; empty when never assigned. */
    public List<com.resqhub.model.RescueAssignment> getAssignmentHistory(
            long requestId) throws DataAccessException {
        try {
            return requestService.getAssignmentsForRequest(requestId);
        } catch (UnauthorizedOperationException e) {
            throw new DataAccessException(e.getMessage(), e);
        } catch (com.resqhub.exception.InvalidRescueRequestException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    public ActionResult deleteRequest(long requestId) {
        try {
            requestService.deleteRequest(requestId);
            return ActionResult.success("Request #" + requestId + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error deleting request: "
                    + e.getMessage());
        }
    }

    /** Latest assignment id for a selected request (-1 when none yet). */
    public long getLatestAssignmentId(long requestId) {
        try {
            Long assignmentId = requestService.getLatestAssignmentId(requestId);
            return assignmentId == null ? -1 : assignmentId;
        } catch (ResQHubException e) {
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /** Score breakdown text for the details dialog. */
    public ActionResult explainPriority(long requestId) {
        try {
            RescueRequest request = requestService.getRequest(requestId);
            String explanation = requestService.explainPriority(request,
                    request.getDisasterId());
            return ActionResult.successWithData(explanation, explanation);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    /** Read methods translate authorization failures into DataAccessException
     *  so views only ever handle one checked exception type. */
    public List<RescueRequest> getPendingQueue() throws DataAccessException {
        try {
            return requestService.getPendingQueue();
        } catch (UnauthorizedOperationException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    public List<RescueRequest> getByStatus(RequestStatus status)
            throws DataAccessException {
        try {
            return requestService.getByStatus(status);
        } catch (UnauthorizedOperationException e) {
            throw new DataAccessException(e.getMessage(), e);
        }
    }

    public List<RescueRequest> search(String keyword)
            throws DataAccessException {
        return requestService.search(keyword);
    }

    public int countPending() throws DataAccessException {
        return requestService.countPending();
    }

    public ActionResult startReview(long requestId) {
        try {
            requestService.startReview(requestId);
            return ActionResult.success("Request #" + requestId
                    + " moved to Under Review");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult unreview(long requestId) {
        try {
            requestService.unreview(requestId);
            return ActionResult.success("Request #" + requestId
                    + " sent back to Pending");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public static Object[] toRow(RescueRequest r) {
        return new Object[] {
                r.getId(),
                r.getRequesterName(),
                r.getContactNumber(),
                r.getLocation(),
                r.getPeopleCount(),
                r.getChildrenCount(),
                r.getElderlyCount(),
                flagsSummary(r),
                r.getPriority() == null ? "UNRATED" : r.getPriority().getLabel(),
                r.getStatus().getLabel()
        };
    }

    private static String flagsSummary(RescueRequest r) {
        StringBuilder sb = new StringBuilder();
        if (r.isLifeThreatening()) {
            sb.append("LIFE ");
        }
        if (r.isMedicalEmergency()) {
            sb.append("MED ");
        }
        if (r.isTrappedUnderDebris()) {
            sb.append("TRAP");
        }
        return sb.length() == 0 ? "-" : sb.toString().trim();
    }

    public static String[] tableHeaders() {
        return new String[] {"ID", "Requester", "Contact", "Location", "Ppl",
                "Kids", "Elderly", "Flags", "Priority", "Status"};
    }
}
