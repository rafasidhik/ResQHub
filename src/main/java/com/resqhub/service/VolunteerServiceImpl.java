package com.resqhub.service;

import com.resqhub.dao.DisasterDao;
import com.resqhub.dao.VolunteerAssignmentDao;
import com.resqhub.dao.VolunteerDao;
import com.resqhub.dto.VolunteerAssignmentDto;
import com.resqhub.dto.VolunteerRegistrationDto;
import com.resqhub.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VolunteerServiceImpl implements VolunteerService {

    private final VolunteerDao volunteerDao;
    private final VolunteerAssignmentDao volunteerAssignmentDao;
    private final DisasterDao disasterDao;
    private final ActivityLogService activityLogService;

    @Autowired
    public VolunteerServiceImpl(VolunteerDao volunteerDao,
                                VolunteerAssignmentDao volunteerAssignmentDao,
                                DisasterDao disasterDao,
                                ActivityLogService activityLogService) {
        this.volunteerDao = volunteerDao;
        this.volunteerAssignmentDao = volunteerAssignmentDao;
        this.disasterDao = disasterDao;
        this.activityLogService = activityLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Volunteer> getAllVolunteers() {
        return volunteerDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Volunteer> getVolunteersByApproval(VolunteerApprovalStatus status) {
        return volunteerDao.findByApprovalStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Volunteer> getAvailableVolunteers() {
        return volunteerDao.findAvailableVolunteers();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Volunteer> getVolunteerById(Long id) {
        return volunteerDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Volunteer> getVolunteerByIdentifier(String identifier) {
        return volunteerDao.findByIdentifier(identifier);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Volunteer> getVolunteerByUserId(Long userId) {
        return volunteerDao.findByUserId(userId);
    }

    @Override
    public Volunteer registerVolunteer(VolunteerRegistrationDto dto, Long currentUserId) {
        Volunteer volunteer = new Volunteer();
        volunteer.setVolunteerIdentifier(volunteerDao.generateNextIdentifier());
        volunteer.setUserId(currentUserId);
        volunteer.setFullName(dto.getFullName());
        volunteer.setEmail(dto.getEmail());
        volunteer.setPhone(dto.getPhone());
        volunteer.setOperatingArea(dto.getOperatingArea());
        volunteer.setSkills(dto.getSkills());
        volunteer.setExperienceYears(dto.getExperienceYears());
        volunteer.setAvailabilityStatus(VolunteerAvailability.AVAILABLE);
        volunteer.setApprovalStatus(VolunteerApprovalStatus.PENDING);
        volunteer.setEmergencyContact(dto.getEmergencyContact());

        Long id = volunteerDao.save(volunteer);
        volunteer.setId(id);

        activityLogService.log(currentUserId, "REGISTER_VOLUNTEER", "VOLUNTEERS",
                volunteer.getVolunteerIdentifier(), "New volunteer registered: " + volunteer.getFullName() + " (" + volunteer.getSkills() + ")");

        return volunteer;
    }

    @Override
    public void approveVolunteer(Long volunteerId, Long currentUserId) {
        Volunteer volunteer = volunteerDao.findById(volunteerId)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer not found with id: " + volunteerId));

        volunteerDao.updateApprovalStatus(volunteerId, VolunteerApprovalStatus.APPROVED);
        activityLogService.log(currentUserId, "APPROVE_VOLUNTEER", "VOLUNTEERS",
                volunteer.getVolunteerIdentifier(), "Approved volunteer application for: " + volunteer.getFullName());
    }

    @Override
    public void rejectVolunteer(Long volunteerId, Long currentUserId) {
        Volunteer volunteer = volunteerDao.findById(volunteerId)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer not found with id: " + volunteerId));

        volunteerDao.updateApprovalStatus(volunteerId, VolunteerApprovalStatus.REJECTED);
        activityLogService.log(currentUserId, "REJECT_VOLUNTEER", "VOLUNTEERS",
                volunteer.getVolunteerIdentifier(), "Rejected volunteer application for: " + volunteer.getFullName());
    }

    @Override
    public void updateAvailability(Long volunteerId, VolunteerAvailability status, Long currentUserId) {
        Volunteer volunteer = volunteerDao.findById(volunteerId)
                .orElseThrow(() -> new IllegalArgumentException("Volunteer not found with id: " + volunteerId));

        volunteerDao.updateAvailabilityStatus(volunteerId, status);
        activityLogService.log(currentUserId, "UPDATE_VOLUNTEER_AVAILABILITY", "VOLUNTEERS",
                volunteer.getVolunteerIdentifier(), "Updated availability status to " + status + " for " + volunteer.getFullName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerAssignment> getAllAssignments() {
        return volunteerAssignmentDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerAssignment> getAssignmentsByVolunteer(Long volunteerId) {
        return volunteerAssignmentDao.findByVolunteerId(volunteerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerAssignment> getAssignmentsByDisaster(Long disasterId) {
        return volunteerAssignmentDao.findByDisasterId(disasterId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VolunteerAssignment> getAssignmentById(Long id) {
        return volunteerAssignmentDao.findById(id);
    }

    @Override
    public VolunteerAssignment assignTask(VolunteerAssignmentDto dto, Long currentUserId) {
        Volunteer volunteer = volunteerDao.findById(dto.getVolunteerId())
                .orElseThrow(() -> new IllegalArgumentException("Volunteer not found with id: " + dto.getVolunteerId()));

        if (volunteer.getApprovalStatus() != VolunteerApprovalStatus.APPROVED) {
            throw new IllegalStateException("Cannot assign tasks to volunteer '" + volunteer.getFullName() + "' because approval status is " + volunteer.getApprovalStatus());
        }

        if (volunteer.getAvailabilityStatus() != VolunteerAvailability.AVAILABLE) {
            throw new IllegalStateException("Volunteer '" + volunteer.getFullName() + "' is currently " + volunteer.getAvailabilityStatus());
        }

        disasterDao.findById(dto.getDisasterId())
                .orElseThrow(() -> new IllegalArgumentException("Disaster not found with id: " + dto.getDisasterId()));

        VolunteerAssignment assignment = new VolunteerAssignment();
        assignment.setVolunteerId(dto.getVolunteerId());
        assignment.setDisasterId(dto.getDisasterId());
        assignment.setTaskTitle(dto.getTaskTitle());
        assignment.setDescription(dto.getDescription());
        assignment.setLocation(dto.getLocation());
        assignment.setPriority(dto.getPriority() != null ? dto.getPriority() : TaskPriority.MEDIUM);
        assignment.setExpectedCompletion(dto.getExpectedCompletion());
        assignment.setStatus(VolunteerAssignmentStatus.OFFERED);
        assignment.setProgressNotes("Task offered to volunteer.");
        assignment.setAssignedByUserId(currentUserId);

        Long assignmentId = volunteerAssignmentDao.save(assignment);
        assignment.setId(assignmentId);

        // Mark volunteer as assigned
        volunteerDao.updateAvailabilityStatus(volunteer.getId(), VolunteerAvailability.ASSIGNED);

        activityLogService.log(currentUserId, "ASSIGN_VOLUNTEER_TASK", "VOLUNTEERS",
                String.valueOf(assignmentId), "Offered task '" + assignment.getTaskTitle() + "' to volunteer " + volunteer.getFullName());

        return assignment;
    }

    @Override
    public void acceptTask(Long assignmentId, Long currentUserId) {
        VolunteerAssignment assignment = volunteerAssignmentDao.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with id: " + assignmentId));

        volunteerAssignmentDao.updateStatus(assignmentId, VolunteerAssignmentStatus.ACCEPTED);
        activityLogService.log(currentUserId, "ACCEPT_VOLUNTEER_TASK", "VOLUNTEERS",
                String.valueOf(assignmentId), "Volunteer accepted assignment: " + assignment.getTaskTitle());
    }

    @Override
    public void declineTask(Long assignmentId, String reason, Long currentUserId) {
        VolunteerAssignment assignment = volunteerAssignmentDao.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with id: " + assignmentId));

        volunteerAssignmentDao.updateStatus(assignmentId, VolunteerAssignmentStatus.DECLINED);
        if (reason != null && !reason.isBlank()) {
            volunteerAssignmentDao.updateProgressNotes(assignmentId, "Declined reason: " + reason);
        }

        // Release volunteer back to AVAILABLE
        volunteerDao.updateAvailabilityStatus(assignment.getVolunteerId(), VolunteerAvailability.AVAILABLE);

        activityLogService.log(currentUserId, "DECLINE_VOLUNTEER_TASK", "VOLUNTEERS",
                String.valueOf(assignmentId), "Volunteer declined assignment: " + assignment.getTaskTitle());
    }

    @Override
    public void startTask(Long assignmentId, Long currentUserId) {
        VolunteerAssignment assignment = volunteerAssignmentDao.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with id: " + assignmentId));

        volunteerAssignmentDao.updateStatus(assignmentId, VolunteerAssignmentStatus.IN_PROGRESS);
        activityLogService.log(currentUserId, "START_VOLUNTEER_TASK", "VOLUNTEERS",
                String.valueOf(assignmentId), "Task marked IN_PROGRESS: " + assignment.getTaskTitle());
    }

    @Override
    public void updateProgressNotes(Long assignmentId, String notes, Long currentUserId) {
        volunteerAssignmentDao.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with id: " + assignmentId));

        volunteerAssignmentDao.updateProgressNotes(assignmentId, notes);
        activityLogService.log(currentUserId, "UPDATE_VOLUNTEER_PROGRESS", "VOLUNTEERS",
                String.valueOf(assignmentId), "Updated progress notes on assignment #" + assignmentId);
    }

    @Override
    public void completeTask(Long assignmentId, String notes, Long currentUserId) {
        VolunteerAssignment assignment = volunteerAssignmentDao.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with id: " + assignmentId));

        volunteerAssignmentDao.updateStatus(assignmentId, VolunteerAssignmentStatus.COMPLETED);
        if (notes != null && !notes.isBlank()) {
            volunteerAssignmentDao.updateProgressNotes(assignmentId, notes);
        }

        // Release volunteer back to AVAILABLE
        volunteerDao.updateAvailabilityStatus(assignment.getVolunteerId(), VolunteerAvailability.AVAILABLE);

        activityLogService.log(currentUserId, "COMPLETE_VOLUNTEER_TASK", "VOLUNTEERS",
                String.valueOf(assignmentId), "Completed volunteer task: " + assignment.getTaskTitle());
    }

    @Override
    public void cancelTask(Long assignmentId, Long currentUserId) {
        VolunteerAssignment assignment = volunteerAssignmentDao.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with id: " + assignmentId));

        volunteerAssignmentDao.updateStatus(assignmentId, VolunteerAssignmentStatus.CANCELLED);

        // Release volunteer back to AVAILABLE
        volunteerDao.updateAvailabilityStatus(assignment.getVolunteerId(), VolunteerAvailability.AVAILABLE);

        activityLogService.log(currentUserId, "CANCEL_VOLUNTEER_TASK", "VOLUNTEERS",
                String.valueOf(assignmentId), "Cancelled volunteer assignment #" + assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalVolunteerCount() {
        return volunteerDao.count();
    }

    @Override
    @Transactional(readOnly = true)
    public int getPendingApprovalCount() {
        return volunteerDao.countPendingApproval();
    }

    @Override
    @Transactional(readOnly = true)
    public int getAvailableCount() {
        return volunteerDao.countAvailable();
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalAssignmentsCount() {
        return volunteerAssignmentDao.count();
    }
}
