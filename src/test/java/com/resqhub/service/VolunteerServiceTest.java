package com.resqhub.service;

import com.resqhub.dao.DisasterDao;
import com.resqhub.dao.VolunteerAssignmentDao;
import com.resqhub.dao.VolunteerDao;
import com.resqhub.dto.VolunteerAssignmentDto;
import com.resqhub.dto.VolunteerRegistrationDto;
import com.resqhub.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VolunteerServiceTest {

    @Mock
    private VolunteerDao volunteerDao;

    @Mock
    private VolunteerAssignmentDao volunteerAssignmentDao;

    @Mock
    private DisasterDao disasterDao;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private VolunteerServiceImpl volunteerService;

    private Volunteer approvedVolunteer;
    private Disaster testDisaster;

    @BeforeEach
    void setUp() {
        approvedVolunteer = new Volunteer();
        approvedVolunteer.setId(1L);
        approvedVolunteer.setVolunteerIdentifier("VOL-0001");
        approvedVolunteer.setFullName("Jane Volunteer");
        approvedVolunteer.setEmail("jane@example.com");
        approvedVolunteer.setPhone("+15550199");
        approvedVolunteer.setOperatingArea("Metro District");
        approvedVolunteer.setSkills("First Aid, Food Distribution");
        approvedVolunteer.setExperienceYears(3);
        approvedVolunteer.setAvailabilityStatus(VolunteerAvailability.AVAILABLE);
        approvedVolunteer.setApprovalStatus(VolunteerApprovalStatus.APPROVED);

        testDisaster = new Disaster();
        testDisaster.setId(10L);
        testDisaster.setName("Metro Flood");
    }

    @Test
    void testRegisterVolunteer_Success() {
        VolunteerRegistrationDto dto = new VolunteerRegistrationDto();
        dto.setFullName("John Responder");
        dto.setEmail("john@example.com");
        dto.setPhone("+15550122");
        dto.setOperatingArea("North Bay");
        dto.setSkills("Emergency Driving");
        dto.setExperienceYears(2);
        dto.setEmergencyContact("Mary Responder +15550123");

        when(volunteerDao.generateNextIdentifier()).thenReturn("VOL-0002");
        when(volunteerDao.save(any(Volunteer.class))).thenReturn(2L);

        Volunteer registered = volunteerService.registerVolunteer(dto, 1L);

        assertNotNull(registered);
        assertEquals(2L, registered.getId());
        assertEquals("VOL-0002", registered.getVolunteerIdentifier());
        assertEquals(VolunteerApprovalStatus.PENDING, registered.getApprovalStatus());
        assertEquals(VolunteerAvailability.AVAILABLE, registered.getAvailabilityStatus());
        verify(volunteerDao, times(1)).save(any(Volunteer.class));
        verify(activityLogService, times(1)).log(eq(1L), eq("REGISTER_VOLUNTEER"), eq("VOLUNTEERS"), anyString(), anyString());
    }

    @Test
    void testApproveVolunteer_Success() {
        when(volunteerDao.findById(1L)).thenReturn(Optional.of(approvedVolunteer));

        volunteerService.approveVolunteer(1L, 1L);

        verify(volunteerDao, times(1)).updateApprovalStatus(1L, VolunteerApprovalStatus.APPROVED);
        verify(activityLogService, times(1)).log(eq(1L), eq("APPROVE_VOLUNTEER"), eq("VOLUNTEERS"), anyString(), anyString());
    }

    @Test
    void testRejectVolunteer_Success() {
        when(volunteerDao.findById(1L)).thenReturn(Optional.of(approvedVolunteer));

        volunteerService.rejectVolunteer(1L, 1L);

        verify(volunteerDao, times(1)).updateApprovalStatus(1L, VolunteerApprovalStatus.REJECTED);
        verify(activityLogService, times(1)).log(eq(1L), eq("REJECT_VOLUNTEER"), eq("VOLUNTEERS"), anyString(), anyString());
    }

    @Test
    void testAssignTask_Success() {
        VolunteerAssignmentDto dto = new VolunteerAssignmentDto();
        dto.setVolunteerId(1L);
        dto.setDisasterId(10L);
        dto.setTaskTitle("Distribute Relief Food");
        dto.setDescription("Serve hot meals at main shelter");
        dto.setLocation("Central Shelter Logistics Tent");
        dto.setPriority(TaskPriority.HIGH);

        when(volunteerDao.findById(1L)).thenReturn(Optional.of(approvedVolunteer));
        when(disasterDao.findById(10L)).thenReturn(Optional.of(testDisaster));
        when(volunteerAssignmentDao.save(any(VolunteerAssignment.class))).thenReturn(101L);

        VolunteerAssignment assignment = volunteerService.assignTask(dto, 1L);

        assertNotNull(assignment);
        assertEquals(101L, assignment.getId());
        assertEquals(VolunteerAssignmentStatus.OFFERED, assignment.getStatus());
        verify(volunteerDao, times(1)).updateAvailabilityStatus(1L, VolunteerAvailability.ASSIGNED);
        verify(activityLogService, times(1)).log(eq(1L), eq("ASSIGN_VOLUNTEER_TASK"), eq("VOLUNTEERS"), eq("101"), anyString());
    }

    @Test
    void testAssignTask_UnapprovedVolunteer_ThrowsException() {
        approvedVolunteer.setApprovalStatus(VolunteerApprovalStatus.PENDING);

        VolunteerAssignmentDto dto = new VolunteerAssignmentDto();
        dto.setVolunteerId(1L);
        dto.setDisasterId(10L);

        when(volunteerDao.findById(1L)).thenReturn(Optional.of(approvedVolunteer));

        assertThrows(IllegalStateException.class, () -> volunteerService.assignTask(dto, 1L));
        verify(volunteerAssignmentDao, never()).save(any(VolunteerAssignment.class));
    }

    @Test
    void testAssignTask_UnavailableVolunteer_ThrowsException() {
        approvedVolunteer.setAvailabilityStatus(VolunteerAvailability.ASSIGNED);

        VolunteerAssignmentDto dto = new VolunteerAssignmentDto();
        dto.setVolunteerId(1L);
        dto.setDisasterId(10L);

        when(volunteerDao.findById(1L)).thenReturn(Optional.of(approvedVolunteer));

        assertThrows(IllegalStateException.class, () -> volunteerService.assignTask(dto, 1L));
        verify(volunteerAssignmentDao, never()).save(any(VolunteerAssignment.class));
    }

    @Test
    void testDeclineTask_ReleasesVolunteer() {
        VolunteerAssignment assignment = new VolunteerAssignment();
        assignment.setId(101L);
        assignment.setVolunteerId(1L);
        assignment.setTaskTitle("Distribute Food");

        when(volunteerAssignmentDao.findById(101L)).thenReturn(Optional.of(assignment));

        volunteerService.declineTask(101L, "Schedule conflict", 1L);

        verify(volunteerAssignmentDao, times(1)).updateStatus(101L, VolunteerAssignmentStatus.DECLINED);
        verify(volunteerDao, times(1)).updateAvailabilityStatus(1L, VolunteerAvailability.AVAILABLE);
    }

    @Test
    void testCompleteTask_ReleasesVolunteer() {
        VolunteerAssignment assignment = new VolunteerAssignment();
        assignment.setId(101L);
        assignment.setVolunteerId(1L);
        assignment.setTaskTitle("First Aid Shift");

        when(volunteerAssignmentDao.findById(101L)).thenReturn(Optional.of(assignment));

        volunteerService.completeTask(101L, "All shifts covered safely", 1L);

        verify(volunteerAssignmentDao, times(1)).updateStatus(101L, VolunteerAssignmentStatus.COMPLETED);
        verify(volunteerDao, times(1)).updateAvailabilityStatus(1L, VolunteerAvailability.AVAILABLE);
    }
}
