package com.resqhub.service;

import com.resqhub.dto.DisasterFormDto;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.model.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DisasterServiceTest {

    @Autowired
    private DisasterService disasterService;

    @Test
    @DisplayName("Create disaster and verify persistence")
    void testCreateDisaster() {
        DisasterFormDto dto = new DisasterFormDto();
        dto.setName("Monsoon Flash Flood Sector 9");
        dto.setDisasterType(DisasterType.FLOOD);
        dto.setLocation("Valley Basin Sector 9");
        dto.setSeverity(Severity.HIGH);
        dto.setStatus(DisasterStatus.ACTIVE);
        dto.setStartDate(LocalDate.now());
        dto.setDescription("Testing automated flash flood incident registration.");

        Long newId = disasterService.createDisaster(dto, 1L);
        assertNotNull(newId);

        Optional<Disaster> fetched = disasterService.getDisasterById(newId);
        assertTrue(fetched.isPresent());
        assertEquals("Monsoon Flash Flood Sector 9", fetched.get().getName());
        assertEquals(DisasterType.FLOOD, fetched.get().getDisasterType());
        assertEquals(Severity.HIGH, fetched.get().getSeverity());
        assertEquals(DisasterStatus.ACTIVE, fetched.get().getStatus());
    }

    @Test
    @DisplayName("Update disaster status to CONTAINED")
    void testUpdateDisasterStatus() {
        DisasterFormDto dto = new DisasterFormDto();
        dto.setName("Urban Storm Runoff");
        dto.setDisasterType(DisasterType.CYCLONE);
        dto.setLocation("Perimeter Zone 2");
        dto.setSeverity(Severity.MODERATE);
        dto.setStatus(DisasterStatus.ACTIVE);
        dto.setStartDate(LocalDate.now());

        Long id = disasterService.createDisaster(dto, 1L);

        // Update
        dto.setId(id);
        dto.setStatus(DisasterStatus.CONTAINED);
        disasterService.updateDisaster(dto, 1L);

        Optional<Disaster> updated = disasterService.getDisasterById(id);
        assertTrue(updated.isPresent());
        assertEquals(DisasterStatus.CONTAINED, updated.get().getStatus());
    }

    @Test
    @DisplayName("Verify deletion safety: cannot delete disaster with active victims or requests")
    void testPreventDeletionWithOperationalRecords() {
        // Disaster ID 1 (Cyclone Vardah) is seeded with victims and requests
        assertThrows(IllegalStateException.class, () -> disasterService.deleteDisaster(1L, 1L));
    }

    @Test
    @DisplayName("Verify deletion of an empty isolated disaster")
    void testDeleteEmptyDisaster() {
        DisasterFormDto dto = new DisasterFormDto();
        dto.setName("Temporary Drill Incident");
        dto.setDisasterType(DisasterType.FIRE);
        dto.setLocation("Test Training Facility");
        dto.setSeverity(Severity.LOW);
        dto.setStatus(DisasterStatus.CLOSED);
        dto.setStartDate(LocalDate.now());

        Long newId = disasterService.createDisaster(dto, 1L);
        assertNotNull(newId);

        // Delete should succeed since it has 0 victims, 0 requests, 0 allocations
        assertDoesNotThrow(() -> disasterService.deleteDisaster(newId, 1L));

        Optional<Disaster> postDelete = disasterService.getDisasterById(newId);
        assertFalse(postDelete.isPresent());
    }

    @Test
    @DisplayName("Verify filtering disasters by active status")
    void testFilterActiveDisasters() {
        List<Disaster> active = disasterService.getActiveDisasters();
        assertNotNull(active);
        assertFalse(active.isEmpty());
        for (Disaster d : active) {
            assertEquals(DisasterStatus.ACTIVE, d.getStatus());
        }
    }
}
