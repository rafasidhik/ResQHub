package com.resqhub.service;

import com.resqhub.dto.RescueRequestDto;
import com.resqhub.dto.RescueTeamDto;
import com.resqhub.dto.TeamRecommendation;
import com.resqhub.model.*;

import java.util.List;
import java.util.Optional;

public interface RescueService {

    Long submitRescueRequest(RescueRequestDto dto, Long reportingUserId);

    void verifyRequest(Long requestId, Long officerId);

    void updateRequestStatus(Long requestId, RescueRequestStatus status, Long officerId);

    Optional<RescueRequest> getRequestById(Long id);

    List<RescueRequest> getRequests(int page, int size, String search, Long disasterId, String status, String priority);

    int countRequests(String search, Long disasterId, String status, String priority);

    List<RescueRequest> getRequestsByCitizen(Long userId, int page, int size);

    int countRequestsByCitizen(Long userId);

    VictimPriority calculatePriority(Urgency urgency, int peopleCount, Long disasterId);

    List<TeamRecommendation> getRecommendedTeams(Long requestId);

    Long assignTeam(Long requestId, Long teamId, String notes, Long officerId);

    void updateAssignmentProgress(Long assignmentId, RescueAssignmentStatus status, String notes, Long officerId);

    void completeRescueOperation(Long assignmentId, String notes, Long officerId);

    Long registerTeam(RescueTeamDto dto, Long officerId);

    void updateTeam(RescueTeamDto dto, Long officerId);

    void releaseTeam(Long teamId, Long officerId);

    Optional<RescueTeam> getTeamById(Long id);

    List<RescueTeam> getTeams(int page, int size, String search, String availability, String status);

    int countTeams(String search, String availability, String status);

    List<RescueTeamMember> getTeamMembers(Long teamId);

    List<RescueAssignment> getTeamHistory(Long teamId);

    List<RescueAssignment> getRequestHistory(Long requestId);
}
