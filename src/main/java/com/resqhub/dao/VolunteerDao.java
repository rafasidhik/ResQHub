package com.resqhub.dao;

import com.resqhub.model.Volunteer;
import com.resqhub.model.VolunteerApprovalStatus;
import com.resqhub.model.VolunteerAvailability;

import java.util.List;
import java.util.Optional;

public interface VolunteerDao {
    Optional<Volunteer> findById(Long id);
    Optional<Volunteer> findByIdentifier(String identifier);
    Optional<Volunteer> findByUserId(Long userId);
    List<Volunteer> findAll();
    List<Volunteer> findByApprovalStatus(VolunteerApprovalStatus status);
    List<Volunteer> findAvailableVolunteers();
    Long save(Volunteer volunteer);
    void update(Volunteer volunteer);
    void updateApprovalStatus(Long id, VolunteerApprovalStatus status);
    void updateAvailabilityStatus(Long id, VolunteerAvailability status);
    void delete(Long id);
    long count();
    int countPendingApproval();
    int countAvailable();
    String generateNextIdentifier();
}
