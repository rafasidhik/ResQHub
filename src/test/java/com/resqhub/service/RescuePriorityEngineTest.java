package com.resqhub.service;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterType;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RescueRequest;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RescuePriorityEngine Core Business Logic Tests")
class RescuePriorityEngineTest {

    private RescuePriorityEngine engine;
    private Disaster catastrophicDisaster;
    private Disaster lowDisaster;

    @BeforeEach
    void setUp() {
        engine = new RescuePriorityEngine();

        catastrophicDisaster = new Disaster("Major Flood", DisasterType.FLOOD,
                DisasterSeverity.CATASTROPHIC, "Wayanad", LocalDateTime.now());

        lowDisaster = new Disaster("Localized Inundation", DisasterType.FLOOD,
                DisasterSeverity.LOW, "Kochi", LocalDateTime.now());
    }

    @Test
    @DisplayName("Life-threatening condition with trapped victims results in CRITICAL priority (score >= 55)")
    void testCriticalPriority() {
        RescueRequest req = new RescueRequest(1L, "Meera", "9000000001", "Chooralmala");
        req.setPeopleCount(5);
        req.setChildrenCount(2);
        req.setElderlyCount(1);
        req.setLifeThreatening(true);
        req.setTrappedUnderDebris(true);

        PriorityLevel priority = engine.evaluate(req, catastrophicDisaster);

        assertEquals(PriorityLevel.CRITICAL, priority);
        String explanation = engine.explain(req, catastrophicDisaster);
        assertNotNull(explanation);
        assertTrue(explanation.contains("Critical"));
        assertTrue(explanation.contains("Life-threatening"));
    }

    @Test
    @DisplayName("Medical emergency with small crowd results in HIGH priority (score >= 38)")
    void testHighPriority() {
        RescueRequest req = new RescueRequest(1L, "Ramesh", "9000000002", "Meppadi");
        req.setPeopleCount(3);
        req.setChildrenCount(1);
        req.setElderlyCount(0);
        req.setLifeThreatening(false);
        req.setMedicalEmergency(true); // 28
        req.setTrappedUnderDebris(false);

        // medical(28) + children(10) + crowdSmall(5) + disaster(2*1=2) = 45 -> >= 38 HIGH
        PriorityLevel priority = engine.evaluate(req, lowDisaster);

        assertEquals(PriorityLevel.HIGH, priority);
    }

    @Test
    @DisplayName("Trapped group without medical emergency results in MEDIUM priority (score >= 16)")
    void testMediumPriority() {
        RescueRequest req = new RescueRequest(1L, "Group", "9000000003", "Camp B");
        req.setPeopleCount(2);
        req.setChildrenCount(0);
        req.setElderlyCount(0);
        req.setLifeThreatening(false);
        req.setMedicalEmergency(false);
        req.setTrappedUnderDebris(true); // 18 + crowdSmall(5) + lowDisaster(2) = 25 -> MEDIUM

        PriorityLevel priority = engine.evaluate(req, lowDisaster);

        assertEquals(PriorityLevel.MEDIUM, priority);
    }

    @Test
    @DisplayName("Single adult request for routine assistance results in LOW priority (score < 16)")
    void testLowPriority() {
        RescueRequest req = new RescueRequest(1L, "Citizen", "9000000004", "Town");
        req.setPeopleCount(1);
        req.setChildrenCount(0);
        req.setElderlyCount(0);
        req.setLifeThreatening(false);
        req.setMedicalEmergency(false);
        req.setTrappedUnderDebris(false);

        // Only low disaster weight = 2 -> LOW
        PriorityLevel priority = engine.evaluate(req, lowDisaster);

        assertEquals(PriorityLevel.LOW, priority);
    }
}
