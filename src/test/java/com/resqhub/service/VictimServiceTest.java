package com.resqhub.service;

import com.resqhub.dto.VictimFormDto;
import com.resqhub.model.RescueStatus;
import com.resqhub.model.Victim;
import com.resqhub.model.VictimPriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VictimServiceTest {

    @Autowired
    private VictimService victimService;

    @Test
    @DisplayName("Register victim and verify unique reference number generation")
    void testRegisterVictimWithAutoReference() {
        VictimFormDto dto = new VictimFormDto();
        dto.setDisasterId(1L); // Seed disaster: Cyclone Vardah
        dto.setFullName("Anand Verma");
        dto.setAge(42);
        dto.setContactNumber("+91 98333 44556");
        dto.setLocation("Coastal Pier Approach Rd");
        dto.setAffectedFamilyMembers(3);
        dto.setAccessibilityNeeds("Elderly mother with oxygen concentrator");
        dto.setCurrentSituation("Water 3 feet in porch, power cut");
        dto.setRescueStatus(RescueStatus.AWAITING_RESCUE);
        dto.setPriority(VictimPriority.CRITICAL);

        Long newId = victimService.registerVictim(dto, 1L);
        assertNotNull(newId);

        Optional<Victim> saved = victimService.getVictimById(newId);
        assertTrue(saved.isPresent());
        assertNotNull(saved.get().getReferenceNumber());
        assertTrue(saved.get().getReferenceNumber().startsWith("VIC-"));
        assertEquals("Anand Verma", saved.get().getFullName());
        assertEquals(VictimPriority.CRITICAL, saved.get().getPriority());
        assertEquals(RescueStatus.AWAITING_RESCUE, saved.get().getRescueStatus());
        assertEquals(3, saved.get().getAffectedFamilyMembers());
    }

    @Test
    @DisplayName("Reject victim registration with invalid disaster ID")
    void testRejectInvalidDisasterAssociation() {
        VictimFormDto dto = new VictimFormDto();
        dto.setDisasterId(999999L); // non-existent disaster
        dto.setFullName("Ghost Victim");
        dto.setLocation("Unknown place");
        dto.setAffectedFamilyMembers(1);

        assertThrows(IllegalArgumentException.class, () -> victimService.registerVictim(dto, 1L));
    }

    @Test
    @DisplayName("Update victim status from Awaiting Rescue to Rescued")
    void testUpdateRescueStatus() {
        VictimFormDto dto = new VictimFormDto();
        dto.setDisasterId(1L);
        dto.setFullName("Meenakshi Sundaram");
        dto.setLocation("Fishermen Harbor Shed #3");
        dto.setAffectedFamilyMembers(2);
        dto.setRescueStatus(RescueStatus.AWAITING_RESCUE);
        dto.setPriority(VictimPriority.HIGH);

        Long id = victimService.registerVictim(dto, 1L);

        // Update status to RESCUED
        victimService.updateRescueStatus(id, RescueStatus.RESCUED, 1L);

        Optional<Victim> updated = victimService.getVictimById(id);
        assertTrue(updated.isPresent());
        assertEquals(RescueStatus.RESCUED, updated.get().getRescueStatus());
    }

    @Test
    @DisplayName("Search victims by reference number")
    void testSearchByReference() {
        Optional<Victim> found = victimService.getVictimByReference("VIC-2026-0001");
        assertTrue(found.isPresent());
        assertEquals("Sunil Das", found.get().getFullName());
    }

    @Test
    @DisplayName("Find victims awaiting rescue for a disaster")
    void testFindAwaitingRescue() {
        List<Victim> awaiting = victimService.getAwaitingRescue(1L);
        assertNotNull(awaiting);
        for (Victim v : awaiting) {
            assertEquals(RescueStatus.AWAITING_RESCUE, v.getRescueStatus());
            assertEquals(1L, v.getDisasterId());
        }
    }
}
