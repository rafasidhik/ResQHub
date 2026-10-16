package com.resqhub.dao;

import com.resqhub.model.VolunteerAssignment;
import com.resqhub.model.VolunteerAssignmentStatus;

import java.util.List;
import java.util.Optional;

public interface VolunteerAssignmentDao {
    Optional<VolunteerAssignment> findById(Long id);
    List<VolunteerAssignment> findAll();
    List<VolunteerAssignment> findByVolunteerId(Long volunteerId);
    List<VolunteerAssignment> findByDisasterId(Long disasterId);
    List<VolunteerAssignment> findByStatus(VolunteerAssignmentStatus status);
    Long save(VolunteerAssignment assignment);
    void updateStatus(Long id, VolunteerAssignmentStatus status);
    void updateProgressNotes(Long id, String notes);
    long count();
}
