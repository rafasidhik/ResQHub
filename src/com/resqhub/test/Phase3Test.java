package com.resqhub.test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.model.BaseEntity;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;
import com.resqhub.model.Person;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RoleType;
import com.resqhub.model.TeamType;
import com.resqhub.model.User;
import com.resqhub.model.Victim;

/**
 * Phase 3 smoke test - model layer.
 * Run after compile.bat:
 *   java -cp "out;lib\*;resources" com.resqhub.test.Phase3Test
 */
public class Phase3Test {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        testInheritanceChain();
        testConstructorChaining();
        testDynamicDispatch();
        testEnumMapping();
        testDomainHelpers();

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testInheritanceChain() {
        Victim victim = new Victim("Anand Menon", 34, null);
        User user = new User();
        Disaster disaster = new Disaster();

        check("Victim is a Person", victim instanceof Person);
        check("Victim is a BaseEntity (multilevel)", victim instanceof BaseEntity);
        check("User is a Person", user instanceof Person);
        check("Disaster extends BaseEntity directly (hierarchical)",
                disaster instanceof BaseEntity
                && !(((Object) disaster) instanceof Person));
    }

    private static void testConstructorChaining() {
        User chained = new User("Rafa Nair");
        check("Single-arg ctor chains to two-arg (phone null)",
                "Rafa Nair".equals(chained.getFullName()) && chained.getPhone() == null);

        User full = new User("Rafa Nair", "9876500002", "rafa@resqhub.org");
        check("Two-arg ctor chains to three-arg values",
                "9876500002".equals(full.getPhone()) && "rafa@resqhub.org".equals(full.getEmail()));
    }

    private static void testDynamicDispatch() {
        List<BaseEntity> mixed = new ArrayList<>();

        Victim v = new Victim("Lakshmi Pillai", 67, null);
        v.setCurrentLocation("Meppadi camp ground");
        v.setEmergencyStatus(com.resqhub.model.EmergencyStatus.INJURED);
        mixed.add(v);

        Disaster d = new Disaster("Wayanad Floods", DisasterType.FLOOD,
                DisasterSeverity.SEVERE, "Wayanad", LocalDateTime.now());
        d.setId(1L);
        mixed.add(d);

        RescueTeam t = new RescueTeam("Coast Guard Alpha", TeamType.NDRF,
                "Cmdr. Suresh", "9848000001");
        t.setId(7L);
        mixed.add(t);

        System.out.println("   dynamic dispatch output:");
        for (BaseEntity entity : mixed) {
            System.out.println("     " + entity);   // runtime type decides getDetails()
        }

        check("Mixed list holds 3 different subtypes",
                mixed.size() == 3
                && mixed.get(0) instanceof Victim
                && mixed.get(1) instanceof Disaster
                && mixed.get(2) instanceof RescueTeam);
        check("Each subtype renders its own details",
                mixed.get(0).getDetails().contains("Lakshmi")
                && mixed.get(1).getDetails().contains("Floods")
                && mixed.get(2).getDetails().contains("Coast Guard"));
    }

    private static void testEnumMapping() {
        check("PriorityLevel weights order correctly",
                PriorityLevel.CRITICAL.getWeight()
                        > PriorityLevel.HIGH.getWeight()
                && PriorityLevel.HIGH.getWeight()
                        > PriorityLevel.LOW.getWeight());
        check("RoleType maps seeded role_name",
                RoleType.valueOf("RESCUE_OFFICER") == RoleType.RESCUE_OFFICER);
        check("Enum label readable",
                DisasterStatus.ACTIVE.getLabel().equals("Active"));
    }

    private static void testDomainHelpers() {
        RescueRequest request = new RescueRequest(1L, "Anand Menon",
                "9847000001", "Chundale, Wayanad");
        request.setPeopleCount(4);
        request.setChildrenCount(2);
        check("Vulnerable occupants detected",
                request.hasVulnerableOccupants());

        RescueRequest solo = new RescueRequest();
        solo.setChildrenCount(0);
        solo.setElderlyCount(0);
        check("No vulnerable occupants when counts zero",
                !solo.hasVulnerableOccupants());

        Victim elder = new Victim("Lakshmi Pillai", 67, null);
        Victim adult = new Victim("Abdul Rasheed", 28, null);
        check("isVulnerableAge: senior true, adult false",
                elder.isVulnerableAge() && !adult.isVulnerableAge());

        Disaster ongoing = new Disaster();
        ongoing.setStatus(DisasterStatus.CONTAINED);
        Disaster closed = new Disaster();
        closed.setStatus(DisasterStatus.RESOLVED);
        check("Disaster.isOngoing logic",
                ongoing.isOngoing() && !closed.isOngoing());
    }

    private static void check(String label, boolean ok) {
        if (ok) {
            passed++;
            System.out.println("[PASS] " + label);
        } else {
            failed++;
            System.out.println("[FAIL] " + label);
        }
    }
}
