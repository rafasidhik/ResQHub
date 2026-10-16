package com.resqhub.service;

import com.resqhub.dto.VolunteerAssignmentDto;
import com.resqhub.dto.VolunteerRegistrationDto;
import com.resqhub.model.*;

import java.util.List;
import java.util.Optional;

public interface VolunteerService {
    List<Volunteer> getAllVolunteers();
    List<Volunteer> getVolunteersByApproval(VolunteerApprovalStatus status);
    List<Volunteer> getAvailableVolunteers();
    Optional<Volunteer> getVolunteerById(Long id);
    Optional<Volunteer> getVolunteerByIdentifier(String identifier);
    Optional<Volunteer> getVolunteerByUserId(Long userId);
    Volunteer registerVolunteer(VolunteerRegistrationDto dto, Long currentUserId);
    void approveVolunteer(Long volunteerId, Long currentUserId);
    void rejectVolunteer(Long volunteerId, Long currentUserId);
    void updateAvailability(Long volunteerId, VolunteerAvailability status, Long currentUserId);

    List<VolunteerAssignment> getAllAssignments();
    List<VolunteerAssignment> getAssignmentsByVolunteer(Long volunteerId);
    List<VolunteerAssignment> getAssignmentsByDisaster(Long disasterId);
    Optional<VolunteerAssignment> getAssignmentById(Long id);
    VolunteerAssignment assignTask(VolunteerAssignmentDto dto, Long currentUserId);
    void acceptTask(Long assignmentId, Long currentUserId);
    void declineTask(Long assignmentId, String reason, Long currentUserId);
    void startTask(Long assignmentId, Long currentUserId);
    void updateProgressNotes(Long assignmentId, String notes, Long currentUserId);
    void completeTask(Long assignmentId, String notes, Long currentUserId);
    void cancelTask(Long assignmentId, Long currentUserId);

    long getTotalVolunteerCount();
    int getPendingApprovalCount();
    int getAvailableCount();
    long getTotalAssignmentsCount();
}
