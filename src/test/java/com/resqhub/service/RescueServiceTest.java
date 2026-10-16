package com.resqhub.service;

import com.resqhub.dto.RescueRequestDto;
import com.resqhub.dto.RescueTeamDto;
import com.resqhub.dto.TeamRecommendation;
import com.resqhub.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RescueServiceTest {

    @Autowired
    private RescueService rescueService;

    @Test
    @DisplayName("Calculate priority: critical urgency and large group yields CRITICAL priority")
    void testCalculatePriorityCritical() {
        VictimPriority priority = rescueService.calculatePriority(Urgency.CRITICAL, 12, 1L);
        assertEquals(VictimPriority.CRITICAL, priority);
    }

    @Test
    @DisplayName("Calculate priority: low urgency and single person yields LOW or MEDIUM priority")
    void testCalculatePriorityLow() {
        VictimPriority priority = rescueService.calculatePriority(Urgency.LOW, 1, 4L);
        assertTrue(priority == VictimPriority.LOW || priority == VictimPriority.MEDIUM);
    }

    @Test
    @DisplayName("Submit rescue request and verify unique reference number")
    void testSubmitRescueRequest() {
        RescueRequestDto dto = new RescueRequestDto();
        dto.setDisasterId(1L);
        dto.setLocation("Pier Approach Sector 1");
        dto.setDescription("Family stranded on terrace due to rising seawater.");
        dto.setContactNumber("+91 98111 88990");
        dto.setPeopleCount(4);
        dto.setUrgency(Urgency.HIGH);

        Long newId = rescueService.submitRescueRequest(dto, 8L);
        assertNotNull(newId);

        Optional<RescueRequest> req = rescueService.getRequestById(newId);
        assertTrue(req.isPresent());
        assertTrue(req.get().getRequestNumber().startsWith("REQ-"));
        assertEquals(RescueRequestStatus.PENDING, req.get().getStatus());
        assertEquals(4, req.get().getPeopleCount());
    }

    @Test
    @DisplayName("Smart team allocation: recommends available and capable team")
    void testSmartTeamRecommendations() {
        // REQ-2026-0001 is pending in Coastal District Zone 1 (Flood/Cyclone)
        List<TeamRecommendation> recommendations = rescueService.getRecommendedTeams(1L);
        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());

        TeamRecommendation topRecommendation = recommendations.get(0);
        assertNotNull(topRecommendation.getTeam());
        assertFalse(topRecommendation.getMatchingFactors().isEmpty());
    }

    @Test
    @DisplayName("Deploy team, prevent duplicate assignment, and complete rescue mission")
    void testFullRescueWorkflow() {
        // 1. Create a fresh available team
        RescueTeamDto teamDto = new RescueTeamDto();
        teamDto.setName("Unit Delta Rapid Deploy");
        teamDto.setTeamLeader("Lt. Ramesh Rao");
        teamDto.setContactNumber("+91 98700 99999");
        teamDto.setSkills("Water Rescue, Inflatable Boat Handling");
        teamDto.setOperatingArea("River Valley Sector 4");
        teamDto.setCapacity(12);
        teamDto.setAvailabilityStatus(TeamAvailability.AVAILABLE);

        Long teamId = rescueService.registerTeam(teamDto, 1L);
        assertNotNull(teamId);

        // 2. Submit a request
        RescueRequestDto reqDto = new RescueRequestDto();
        reqDto.setDisasterId(2L); // River Valley Flash Flood
        reqDto.setLocation("River Valley Sector 4 Island Point");
        reqDto.setDescription("3 villagers stranded near culvert.");
        reqDto.setContactNumber("+91 98111 00011");
        reqDto.setPeopleCount(3);
        reqDto.setUrgency(Urgency.HIGH);

        Long reqId = rescueService.submitRescueRequest(reqDto, 8L);

        // 3. Assign team to request
        Long assignmentId = rescueService.assignTeam(reqId, teamId, "Deploying boat squad immediately.", 2L);
        assertNotNull(assignmentId);

        // Verify request and team statuses updated
        RescueRequest assignedReq = rescueService.getRequestById(reqId).orElseThrow();
        assertEquals(RescueRequestStatus.ASSIGNED, assignedReq.getStatus());

        RescueTeam assignedTeam = rescueService.getTeamById(teamId).orElseThrow();
        assertEquals(TeamAvailability.ASSIGNED, assignedTeam.getAvailabilityStatus());

        // 4. Test Conflict Prevention: second assignment to the same request must be rejected
        assertThrows(IllegalStateException.class, () ->
                rescueService.assignTeam(reqId, teamId, "Duplicate attempt", 2L));

        // 5. Complete operation and verify release
        rescueService.completeRescueOperation(assignmentId, "All 3 villagers safely brought to dry land.", 2L);

        RescueRequest completedReq = rescueService.getRequestById(reqId).orElseThrow();
        assertEquals(RescueRequestStatus.COMPLETED, completedReq.getStatus());

        RescueTeam releasedTeam = rescueService.getTeamById(teamId).orElseThrow();
        assertEquals(TeamAvailability.AVAILABLE, releasedTeam.getAvailabilityStatus());
    }
}
