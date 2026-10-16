package com.resqhub.service;

import com.resqhub.dao.DisasterDao;
import com.resqhub.dao.ShelterAllocationDao;
import com.resqhub.dao.ShelterDao;
import com.resqhub.dao.ShelterResidentDao;
import com.resqhub.dao.VictimDao;
import com.resqhub.dto.ResidentCheckInDto;
import com.resqhub.dto.ShelterDto;
import com.resqhub.dto.ShelterRecommendation;
import com.resqhub.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShelterServiceTest {

    @Mock
    private ShelterDao shelterDao;

    @Mock
    private ShelterResidentDao shelterResidentDao;

    @Mock
    private ShelterAllocationDao shelterAllocationDao;

    @Mock
    private DisasterDao disasterDao;

    @Mock
    private VictimDao victimDao;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ShelterServiceImpl shelterService;

    private Shelter testShelter;
    private Disaster testDisaster;

    @BeforeEach
    void setUp() {
        testShelter = new Shelter();
        testShelter.setId(1L);
        testShelter.setShelterIdentifier("SHT-0001");
        testShelter.setName("Central Community Shelter");
        testShelter.setAddress("123 Relief Road, Downtown Sector");
        testShelter.setLocationDescription("Near Central Park");
        testShelter.setContactPerson("Alice Camp Manager");
        testShelter.setContactNumber("+1555123456");
        testShelter.setTotalCapacity(50);
        testShelter.setCurrentOccupancy(10);
        testShelter.setStatus(ShelterStatus.AVAILABLE);

        testDisaster = new Disaster();
        testDisaster.setId(10L);
        testDisaster.setName("Downtown River Flood");
        testDisaster.setLocation("Downtown Sector");
        testDisaster.setStatus(DisasterStatus.ACTIVE);
    }

    @Test
    void testCreateShelter_Success() {
        ShelterDto dto = new ShelterDto();
        dto.setName("New Safe Haven");
        dto.setAddress("456 Hill St");
        dto.setContactPerson("Bob Manager");
        dto.setContactNumber("+1555987654");
        dto.setTotalCapacity(100);
        dto.setStatus(ShelterStatus.AVAILABLE);

        when(shelterDao.generateNextIdentifier()).thenReturn("SHT-0002");
        when(shelterDao.save(any(Shelter.class))).thenReturn(2L);

        Shelter created = shelterService.createShelter(dto, 1L);

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("SHT-0002", created.getShelterIdentifier());
        verify(shelterDao, times(1)).save(any(Shelter.class));
        verify(activityLogService, times(1)).log(eq(1L), eq("CREATE_SHELTER"), eq("SHELTERS"), anyString(), anyString());
    }

    @Test
    void testCheckInResident_Success() {
        ResidentCheckInDto checkInDto = new ResidentCheckInDto();
        checkInDto.setShelterId(1L);
        checkInDto.setDisasterId(10L);
        checkInDto.setResidentName("John Evacuee");
        checkInDto.setFamilySize(4);

        when(shelterDao.findById(1L)).thenReturn(Optional.of(testShelter));
        when(shelterResidentDao.save(any(ShelterResident.class))).thenReturn(101L);
        when(shelterResidentDao.sumActiveFamilySizeByShelterId(1L)).thenReturn(14);

        ShelterResident resident = shelterService.checkInResident(checkInDto, 1L);

        assertNotNull(resident);
        assertEquals(101L, resident.getId());
        assertEquals("John Evacuee", resident.getResidentName());
        assertEquals(4, resident.getFamilySize());
        verify(shelterResidentDao, times(1)).save(any(ShelterResident.class));
        verify(shelterDao, times(1)).updateOccupancy(eq(1L), eq(14), eq(ShelterStatus.AVAILABLE));
    }

    @Test
    void testCheckInResident_InsufficientCapacity_ThrowsException() {
        ResidentCheckInDto checkInDto = new ResidentCheckInDto();
        checkInDto.setShelterId(1L);
        checkInDto.setDisasterId(10L);
        checkInDto.setResidentName("Large Group");
        checkInDto.setFamilySize(45); // Remaining is 40

        when(shelterDao.findById(1L)).thenReturn(Optional.of(testShelter));

        assertThrows(IllegalStateException.class, () -> shelterService.checkInResident(checkInDto, 1L));
        verify(shelterResidentDao, never()).save(any(ShelterResident.class));
    }

    @Test
    void testCheckInResident_TemporarilyClosed_ThrowsException() {
        testShelter.setStatus(ShelterStatus.TEMPORARILY_CLOSED);

        ResidentCheckInDto checkInDto = new ResidentCheckInDto();
        checkInDto.setShelterId(1L);
        checkInDto.setDisasterId(10L);
        checkInDto.setResidentName("Jane Evacuee");
        checkInDto.setFamilySize(2);

        when(shelterDao.findById(1L)).thenReturn(Optional.of(testShelter));

        assertThrows(IllegalStateException.class, () -> shelterService.checkInResident(checkInDto, 1L));
        verify(shelterResidentDao, never()).save(any(ShelterResident.class));
    }

    @Test
    void testCheckOutResident_Success() {
        ShelterResident resident = new ShelterResident();
        resident.setId(101L);
        resident.setShelterId(1L);
        resident.setResidentName("John Evacuee");
        resident.setStatus(ResidentStatus.ACTIVE);

        when(shelterResidentDao.findById(101L)).thenReturn(Optional.of(resident));
        when(shelterDao.findById(1L)).thenReturn(Optional.of(testShelter));
        when(shelterResidentDao.sumActiveFamilySizeByShelterId(1L)).thenReturn(10);

        shelterService.checkOutResident(101L, 1L);

        verify(shelterResidentDao, times(1)).checkOut(101L);
        verify(shelterDao, times(1)).updateOccupancy(eq(1L), eq(10), eq(ShelterStatus.AVAILABLE));
        verify(activityLogService, times(1)).log(eq(1L), eq("CHECK_OUT_RESIDENT"), eq("SHELTERS"), anyString(), anyString());
    }

    @Test
    void testRecommendShelters_RanksProperly() {
        Shelter nearbyShelter = new Shelter();
        nearbyShelter.setId(1L);
        nearbyShelter.setName("Downtown Relief Camp");
        nearbyShelter.setAddress("Downtown Sector Hub");
        nearbyShelter.setTotalCapacity(100);
        nearbyShelter.setCurrentOccupancy(10); // remaining 90
        nearbyShelter.setStatus(ShelterStatus.AVAILABLE);

        Shelter farShelter = new Shelter();
        farShelter.setId(2L);
        farShelter.setName("Uptown Safe Zone");
        farShelter.setAddress("Uptown District 5");
        farShelter.setTotalCapacity(50);
        farShelter.setCurrentOccupancy(40); // remaining 10
        farShelter.setStatus(ShelterStatus.NEARLY_FULL);

        when(disasterDao.findById(10L)).thenReturn(Optional.of(testDisaster));
        when(shelterDao.findAll()).thenReturn(Arrays.asList(farShelter, nearbyShelter));

        List<ShelterRecommendation> recommendations = shelterService.recommendShelters(10L, 5);

        assertNotNull(recommendations);
        assertEquals(2, recommendations.size());
        // Top recommendation should be nearbyShelter due to higher proximity and ample capacity buffer
        assertEquals("Downtown Relief Camp", recommendations.get(0).getShelter().getName());
        assertTrue(recommendations.get(0).getScore() > recommendations.get(1).getScore());
    }

    @Test
    void testDeleteShelter_WithActiveResidents_ThrowsException() {
        when(shelterDao.findById(1L)).thenReturn(Optional.of(testShelter));
        when(shelterResidentDao.countActiveResidentsByShelterId(1L)).thenReturn(3);

        assertThrows(IllegalStateException.class, () -> shelterService.deleteShelter(1L, 1L));
        verify(shelterDao, never()).delete(1L);
    }
}
