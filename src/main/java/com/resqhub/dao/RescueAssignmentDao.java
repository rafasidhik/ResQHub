package com.resqhub.dao;

import com.resqhub.model.RescueAssignment;
import com.resqhub.model.RescueAssignmentStatus;

import java.util.List;
import java.util.Optional;

public interface RescueAssignmentDao {

    Optional<RescueAssignment> findById(Long id);

    Optional<RescueAssignment> findActiveByRequestId(Long requestId);

    Optional<RescueAssignment> findActiveByTeamId(Long teamId);

    Long save(RescueAssignment assignment);

    void updateStatus(Long id, RescueAssignmentStatus status, String progressNotes);

    void completeAssignment(Long id, String progressNotes);

    List<RescueAssignment> findHistoryByTeamId(Long teamId);

    List<RescueAssignment> findHistoryByRequestId(Long requestId);
}
