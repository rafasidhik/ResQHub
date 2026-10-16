package com.resqhub.service.impl;

import com.resqhub.dao.DisasterDao;
import com.resqhub.dao.RescueAssignmentDao;
import com.resqhub.dao.RescueRequestDao;
import com.resqhub.dao.RescueTeamDao;
import com.resqhub.dao.VictimDao;
import com.resqhub.dto.RescueRequestDto;
import com.resqhub.dto.RescueTeamDto;
import com.resqhub.dto.TeamRecommendation;
import com.resqhub.model.*;
import com.resqhub.service.ActivityLogService;
import com.resqhub.service.RescueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.*;

@Service
public class RescueServiceImpl implements RescueService {

    private final RescueRequestDao requestDao;
    private final RescueTeamDao teamDao;
    private final RescueAssignmentDao assignmentDao;
    private final DisasterDao disasterDao;
    private final VictimDao victimDao;
    private final ActivityLogService activityLogService;

    @Autowired
    public RescueServiceImpl(RescueRequestDao requestDao,
                             RescueTeamDao teamDao,
                             RescueAssignmentDao assignmentDao,
                             DisasterDao disasterDao,
                             VictimDao victimDao,
                             ActivityLogService activityLogService) {
        this.requestDao = requestDao;
        this.teamDao = teamDao;
        this.assignmentDao = assignmentDao;
        this.disasterDao = disasterDao;
        this.victimDao = victimDao;
        this.activityLogService = activityLogService;
    }

    private synchronized String generateRequestNumber() {
        int seq = requestDao.getNextSequence();
        return String.format("REQ-%d-%04d", Year.now().getValue(), seq);
    }

    private synchronized String generateTeamIdentifier() {
        int seq = teamDao.getNextSequence();
        return String.format("TEAM-%03d", seq);
    }

    @Override
    public VictimPriority calculatePriority(Urgency urgency, int peopleCount, Long disasterId) {
        int score = 0;

        // 1. Urgency weight
        if (urgency != null) {
            score += urgency.getBaseWeight();
        }

        // 2. People stranded weight
        if (peopleCount >= 10) {
            score += 30;
        } else if (peopleCount >= 5) {
            score += 15;
        } else if (peopleCount > 1) {
            score += 5;
        }

        // 3. Disaster severity weight
        if (disasterId != null) {
            Optional<Disaster> dOpt = disasterDao.findById(disasterId);
            if (dOpt.isPresent()) {
                Disaster d = dOpt.get();
                if (d.getSeverity() == Severity.CRITICAL) score += 25;
                else if (d.getSeverity() == Severity.HIGH) score += 15;
                else if (d.getSeverity() == Severity.MODERATE) score += 10;
                else score += 5;
            }
        }

        if (score >= 70) return VictimPriority.CRITICAL;
        if (score >= 45) return VictimPriority.HIGH;
        if (score >= 25) return VictimPriority.MEDIUM;
        return VictimPriority.LOW;
    }

    @Override
    @Transactional
    public Long submitRescueRequest(RescueRequestDto dto, Long reportingUserId) {
        disasterDao.findById(dto.getDisasterId())
                .orElseThrow(() -> new IllegalArgumentException("Disaster record not found with ID: " + dto.getDisasterId()));

        RescueRequest request = new RescueRequest();
        request.setRequestNumber(generateRequestNumber());
        request.setDisasterId(dto.getDisasterId());
        request.setVictimId(dto.getVictimId());
        request.setReportedByUserId(reportingUserId);
        request.setLocation(dto.getLocation().trim());
        request.setDescription(dto.getDescription().trim());
        request.setContactNumber(dto.getContactNumber().trim());
        request.setPeopleCount(dto.getPeopleCount() != null ? dto.getPeopleCount() : 1);
        request.setUrgency(dto.getUrgency() != null ? dto.getUrgency() : Urgency.MEDIUM);

        // Transparent rule-based prioritization
        VictimPriority priority = calculatePriority(request.getUrgency(), request.getPeopleCount(), request.getDisasterId());
        request.setCalculatedPriority(priority);
        request.setStatus(RescueRequestStatus.PENDING);

        Long newId = requestDao.save(request);

        activityLogService.log(reportingUserId, "SUBMIT_RESCUE_REQUEST", "RESCUE", request.getRequestNumber(),
                "Rescue request submitted for " + request.getPeopleCount() + " individuals. Priority: " + priority.name());

        return newId;
    }

    @Override
    @Transactional
    public void verifyRequest(Long requestId, Long officerId) {
        RescueRequest req = requestDao.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));

        if (req.getStatus() == RescueRequestStatus.PENDING) {
            requestDao.updateStatus(requestId, RescueRequestStatus.VERIFIED);
            activityLogService.log(officerId, "VERIFY_REQUEST", "RESCUE", req.getRequestNumber(),
                    "Verified emergency status of request " + req.getRequestNumber());
        }
    }

    @Override
    @Transactional
    public void updateRequestStatus(Long requestId, RescueRequestStatus status, Long officerId) {
        RescueRequest req = requestDao.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));

        requestDao.updateStatus(requestId, status);
        activityLogService.log(officerId, "UPDATE_REQUEST_STATUS", "RESCUE", req.getRequestNumber(),
                "Changed status of request " + req.getRequestNumber() + " to " + status.name());
    }

    @Override
    public Optional<RescueRequest> getRequestById(Long id) {
        return requestDao.findById(id);
    }

    @Override
    public List<RescueRequest> getRequests(int page, int size, String search, Long disasterId, String status, String priority) {
        return requestDao.findAll(page, size, search, disasterId, status, priority);
    }

    @Override
    public int countRequests(String search, Long disasterId, String status, String priority) {
        return requestDao.count(search, disasterId, status, priority);
    }

    @Override
    public List<RescueRequest> getRequestsByCitizen(Long userId, int page, int size) {
        return requestDao.findByReportedUserId(userId, page, size);
    }

    @Override
    public int countRequestsByCitizen(Long userId) {
        return requestDao.countByReportedUserId(userId);
    }

    @Override
    public List<TeamRecommendation> getRecommendedTeams(Long requestId) {
        RescueRequest req = requestDao.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));

        Disaster disaster = disasterDao.findById(req.getDisasterId()).orElse(null);
        List<RescueTeam> allTeams = teamDao.findAll(0, 100, null, null, "ACTIVE");
        List<TeamRecommendation> recommendations = new ArrayList<>();

        for (RescueTeam team : allTeams) {
            TeamRecommendation rec = new TeamRecommendation();
            rec.setTeam(team);
            int score = 0;
            boolean eligible = true;

            // 1. Availability check (Primary hard constraint)
            if (team.getAvailabilityStatus() != TeamAvailability.AVAILABLE) {
                eligible = false;
                rec.addWarning("Team is currently " + team.getAvailabilityStatus().getDisplayName() + " (not available for new deployment)");
            } else {
                score += 25;
                rec.addFactor("Team status is AVAILABLE for immediate deployment (+25)");
            }

            // 2. Capacity check
            if (team.getCapacity() < req.getPeopleCount()) {
                eligible = false;
                rec.addWarning(String.format("Squad capacity (%d) is less than required people count (%d)",
                        team.getCapacity(), req.getPeopleCount()));
            } else {
                score += 15;
                rec.addFactor(String.format("Squad capacity (%d) safely accommodates group of %d (+15)",
                        team.getCapacity(), req.getPeopleCount()));
            }

            // 3. Operating area proximity check (Sub-string / keyword match)
            String teamArea = team.getOperatingArea().toLowerCase();
            String reqLocation = req.getLocation().toLowerCase();
            String disasterLocation = disaster != null ? disaster.getLocation().toLowerCase() : "";

            if (reqLocation.contains(teamArea) || teamArea.contains(reqLocation) ||
                disasterLocation.contains(teamArea) || teamArea.contains(disasterLocation)) {
                score += 30;
                rec.addFactor("Designated operating sector matches incident perimeter (+30)");
            } else {
                score += 10;
                rec.addFactor("Adjacent or secondary zone coverage (+10)");
            }

            // 4. Specialized Hazard & Skill Match
            String skills = team.getSkills().toLowerCase();
            if (disaster != null) {
                if ((disaster.getDisasterType() == DisasterType.FLOOD || disaster.getDisasterType() == DisasterType.CYCLONE)
                        && (skills.contains("water") || skills.contains("boat") || skills.contains("dive") || skills.contains("swift"))) {
                    score += 25;
                    rec.addFactor("Certified swift-water navigation and flood rescue equipment (+25)");
                } else if ((disaster.getDisasterType() == DisasterType.LANDSLIDE || disaster.getDisasterType() == DisasterType.EARTHQUAKE)
                        && (skills.contains("rope") || skills.contains("rigging") || skills.contains("debris") || skills.contains("mountain"))) {
                    score += 25;
                    rec.addFactor("Certified high-angle rope rigging and debris clearance specialist (+25)");
                } else if (skills.contains("first aid") || skills.contains("medical") || skills.contains("cpr")) {
                    score += 15;
                    rec.addFactor("Field trauma medical and first aid capability (+15)");
                }
            }

            rec.setScore(Math.min(100, score));
            rec.setEligible(eligible);
            recommendations.add(rec);
        }

        // Sort: eligible teams first, then descending by score
        recommendations.sort((a, b) -> {
            if (a.isEligible() != b.isEligible()) {
                return a.isEligible() ? -1 : 1;
            }
            return Integer.compare(b.getScore(), a.getScore());
        });

        return recommendations;
    }

    @Override
    @Transactional
    public Long assignTeam(Long requestId, Long teamId, String notes, Long officerId) {
        RescueRequest req = requestDao.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found with ID: " + requestId));

        RescueTeam team = teamDao.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));

        // 1. Conflict Prevention: Prevent duplicate active assignments
        if (requestDao.hasActiveAssignment(requestId)) {
            throw new IllegalStateException("Rescue request " + req.getRequestNumber() + " already has an active deployment.");
        }

        // 2. Conflict Prevention: Prevent assigning unavailable team
        if (team.getAvailabilityStatus() != TeamAvailability.AVAILABLE) {
            throw new IllegalStateException("Rescue team " + team.getName() + " is currently " + team.getAvailabilityStatus().getDisplayName());
        }

        // 3. Create Rescue Assignment Record
        RescueAssignment assignment = new RescueAssignment();
        assignment.setRequestId(requestId);
        assignment.setTeamId(teamId);
        assignment.setAssignedByUserId(officerId);
        assignment.setStatus(RescueAssignmentStatus.ASSIGNED);
        assignment.setProgressNotes(notes != null ? notes.trim() : "Team dispatched to coordinates.");

        Long assignmentId = assignmentDao.save(assignment);

        // 4. Update Rescue Request Status
        requestDao.updateStatus(requestId, RescueRequestStatus.ASSIGNED);

        // 5. Update Rescue Team Availability
        teamDao.updateAvailability(teamId, TeamAvailability.ASSIGNED);

        // 6. Update linked Victim status if exists
        if (req.getVictimId() != null) {
            victimDao.updateStatus(req.getVictimId(), RescueStatus.ASSIGNED);
        }

        activityLogService.log(officerId, "ASSIGN_RESCUE_TEAM", "RESCUE", req.getRequestNumber(),
                "Assigned rescue team " + team.getName() + " to request " + req.getRequestNumber());

        return assignmentId;
    }

    @Override
    @Transactional
    public void updateAssignmentProgress(Long assignmentId, RescueAssignmentStatus status, String notes, Long officerId) {
        RescueAssignment assignment = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with ID: " + assignmentId));

        assignmentDao.updateStatus(assignmentId, status, notes);

        if (status == RescueAssignmentStatus.IN_PROGRESS) {
            requestDao.updateStatus(assignment.getRequestId(), RescueRequestStatus.IN_PROGRESS);
            teamDao.updateAvailability(assignment.getTeamId(), TeamAvailability.ON_OPERATION);
        } else if (status == RescueAssignmentStatus.CANCELLED) {
            requestDao.updateStatus(assignment.getRequestId(), RescueRequestStatus.VERIFIED);
            teamDao.updateAvailability(assignment.getTeamId(), TeamAvailability.AVAILABLE);
        }

        activityLogService.log(officerId, "UPDATE_ASSIGNMENT", "RESCUE", assignment.getRequestNumber(),
                "Updated mission progress to " + status.name() + ": " + notes);
    }

    @Override
    @Transactional
    public void completeRescueOperation(Long assignmentId, String notes, Long officerId) {
        RescueAssignment assignment = assignmentDao.findById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found with ID: " + assignmentId));

        // 1. Complete Assignment
        assignmentDao.completeAssignment(assignmentId, notes != null ? notes : "Mission completed successfully.");

        // 2. Update Request Status to COMPLETED
        requestDao.updateStatus(assignment.getRequestId(), RescueRequestStatus.COMPLETED);

        // 3. Release Team back to AVAILABLE
        teamDao.updateAvailability(assignment.getTeamId(), TeamAvailability.AVAILABLE);

        // 4. If linked victim exists, update victim status to RESCUED
        requestDao.findById(assignment.getRequestId()).ifPresent(req -> {
            if (req.getVictimId() != null) {
                victimDao.updateStatus(req.getVictimId(), RescueStatus.RESCUED);
            }
        });

        activityLogService.log(officerId, "COMPLETE_RESCUE_OPERATION", "RESCUE", assignment.getRequestNumber(),
                "Completed rescue mission for request " + assignment.getRequestNumber() + ". Team " + assignment.getTeamName() + " released to AVAILABLE.");
    }

    @Override
    @Transactional
    public Long registerTeam(RescueTeamDto dto, Long officerId) {
        RescueTeam team = new RescueTeam();
        team.setTeamIdentifier(generateTeamIdentifier());
        team.setName(dto.getName().trim());
        team.setTeamLeader(dto.getTeamLeader().trim());
        team.setContactNumber(dto.getContactNumber().trim());
        team.setSkills(dto.getSkills().trim());
        team.setOperatingArea(dto.getOperatingArea().trim());
        team.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : 10);
        team.setAvailabilityStatus(dto.getAvailabilityStatus() != null ? dto.getAvailabilityStatus() : TeamAvailability.AVAILABLE);
        team.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");

        Long newId = teamDao.save(team);

        // Save individual squad members if provided
        if (dto.getNewMembersText() != null && !dto.getNewMembersText().trim().isEmpty()) {
            String[] lines = dto.getNewMembersText().split("[\\r\\n,]+");
            for (String line : lines) {
                String memberName = line.trim();
                if (!memberName.isEmpty()) {
                    teamDao.saveMember(new RescueTeamMember(newId, memberName, "Operative", dto.getContactNumber(), team.getSkills()));
                }
            }
        }

        activityLogService.log(officerId, "REGISTER_RESCUE_TEAM", "RESCUE", team.getTeamIdentifier(),
                "Registered rescue unit " + team.getName() + " (" + team.getTeamIdentifier() + ")");

        return newId;
    }

    @Override
    @Transactional
    public void updateTeam(RescueTeamDto dto, Long officerId) {
        RescueTeam existing = teamDao.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + dto.getId()));

        existing.setName(dto.getName().trim());
        existing.setTeamLeader(dto.getTeamLeader().trim());
        existing.setContactNumber(dto.getContactNumber().trim());
        existing.setSkills(dto.getSkills().trim());
        existing.setOperatingArea(dto.getOperatingArea().trim());
        existing.setCapacity(dto.getCapacity() != null ? dto.getCapacity() : 10);
        existing.setAvailabilityStatus(dto.getAvailabilityStatus());
        existing.setStatus(dto.getStatus());

        teamDao.update(existing);

        activityLogService.log(officerId, "UPDATE_RESCUE_TEAM", "RESCUE", existing.getTeamIdentifier(),
                "Updated details for rescue squad " + existing.getName());
    }

    @Override
    @Transactional
    public void releaseTeam(Long teamId, Long officerId) {
        RescueTeam team = teamDao.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with ID: " + teamId));

        teamDao.updateAvailability(teamId, TeamAvailability.AVAILABLE);

        activityLogService.log(officerId, "RELEASE_TEAM", "RESCUE", team.getTeamIdentifier(),
                "Released rescue team " + team.getName() + " back to AVAILABLE.");
    }

    @Override
    public Optional<RescueTeam> getTeamById(Long id) {
        return teamDao.findById(id);
    }

    @Override
    public List<RescueTeam> getTeams(int page, int size, String search, String availability, String status) {
        return teamDao.findAll(page, size, search, availability, status);
    }

    @Override
    public int countTeams(String search, String availability, String status) {
        return teamDao.count(search, availability, status);
    }

    @Override
    public List<RescueTeamMember> getTeamMembers(Long teamId) {
        return teamDao.findMembersByTeamId(teamId);
    }

    @Override
    public List<RescueAssignment> getTeamHistory(Long teamId) {
        return assignmentDao.findHistoryByTeamId(teamId);
    }

    @Override
    public List<RescueAssignment> getRequestHistory(Long requestId) {
        return assignmentDao.findHistoryByRequestId(requestId);
    }
}
