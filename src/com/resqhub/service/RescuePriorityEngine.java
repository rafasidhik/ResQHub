package com.resqhub.service;

import java.util.Vector;

import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.PriorityLevel;
import com.resqhub.model.RescueRequest;

/**
 * RESCUE PRIORITY ALGORITHM.
 *
 * Each rescue request is scored by summing the output of independent
 * scoring rules; the total maps onto CRITICAL / HIGH / MEDIUM / LOW.
 *
 * Scoring model:
 *   life-threatening condition ........ 40
 *   medical emergency ................. 28
 *   trapped under debris .............. 18
 *   children among the trapped ........ 10
 *   elderly among the trapped .........  8
 *   crowd size: >= 5 people ........... 10
 *   crowd size: >= 2 people ............ 5
 *   disaster severity weight x 2 ....  2-8
 *
 * Classification thresholds: >=55 CRITICAL, >=38 HIGH, >=16 MEDIUM, else LOW.
 *
 * Design notes (viva points):
 *  - Rules are objects behind a nested interface -> DYNAMIC METHOD DISPATCH:
 *    the engine loop calls apply() and each concrete rule responds.
 *  - OPEN/CLOSED PRINCIPLE: a new rule (e.g. flood-water depth) is one new
 *    nested class registered in the constructor - the engine loop never changes.
 *  - Vector stores the rules: a deliberate, documented use of the legacy
 *    synchronized collection from the syllabus (single-threaded here).
 */
public class RescuePriorityEngine {

    public static final int CRITICAL_THRESHOLD = 55;
    public static final int HIGH_THRESHOLD = 38;
    public static final int MEDIUM_THRESHOLD = 16;

    public static final int WEIGHT_LIFE_THREATENING = 40;
    public static final int WEIGHT_MEDICAL_EMERGENCY = 28;
    public static final int WEIGHT_TRAPPED_DEBRIS = 18;
    public static final int WEIGHT_CHILDREN_PRESENT = 10;
    public static final int WEIGHT_ELDERLY_PRESENT = 8;
    public static final int WEIGHT_CROWD_LARGE = 10;
    public static final int WEIGHT_CROWD_SMALL = 5;
    public static final int SEVERITY_MULTIPLIER = 2;

    private final Vector<PriorityRule> rules = new Vector<>();

    public RescuePriorityEngine() {
        rules.add(new LifeThreateningRule());
        rules.add(new MedicalEmergencyRule());
        rules.add(new TrappedDebrisRule());
        rules.add(new VulnerableOccupantsRule());
        rules.add(new CrowdSizeRule());
        rules.add(new DisasterSeverityRule());
    }

    /** Total weighted score of a request in the context of its disaster. */
    public int score(RescueRequest request, Disaster disaster) {
        int total = 0;
        for (PriorityRule rule : rules) {
            total += rule.apply(request, disaster);
        }
        return total;
    }

    /** Maps a raw score to the four-level priority scale. */
    public PriorityLevel classify(int score) {
        if (score >= CRITICAL_THRESHOLD) {
            return PriorityLevel.CRITICAL;
        }
        if (score >= HIGH_THRESHOLD) {
            return PriorityLevel.HIGH;
        }
        if (score >= MEDIUM_THRESHOLD) {
            return PriorityLevel.MEDIUM;
        }
        return PriorityLevel.LOW;
    }

    public PriorityLevel evaluate(RescueRequest request, Disaster disaster) {
        return classify(score(request, disaster));
    }

    /** Human-readable score breakdown - used by the GUI details dialog. */
    public String explain(RescueRequest request, Disaster disaster) {
        StringBuilder sb = new StringBuilder("Priority breakdown:\n");
        for (PriorityRule rule : rules) {
            sb.append(String.format("  %-28s %+d%n",
                    rule.getLabel(), rule.apply(request, disaster)));
        }
        int total = score(request, disaster);
        sb.append("TOTAL SCORE: ").append(total)
          .append(" -> ").append(classify(total).getLabel());
        return sb.toString();
    }

    /** Contract every scoring rule fulfils (nested interface - engine-private). */
    private interface PriorityRule {
        int apply(RescueRequest request, Disaster disaster);

        String getLabel();
    }

    private static class LifeThreateningRule implements PriorityRule {
        @Override
        public int apply(RescueRequest request, Disaster disaster) {
            return request.isLifeThreatening() ? WEIGHT_LIFE_THREATENING : 0;
        }

        @Override
        public String getLabel() {
            return "Life-threatening condition";
        }
    }

    private static class MedicalEmergencyRule implements PriorityRule {
        @Override
        public int apply(RescueRequest request, Disaster disaster) {
            return request.isMedicalEmergency() ? WEIGHT_MEDICAL_EMERGENCY : 0;
        }

        @Override
        public String getLabel() {
            return "Medical emergency";
        }
    }

    private static class TrappedDebrisRule implements PriorityRule {
        @Override
        public int apply(RescueRequest request, Disaster disaster) {
            return request.isTrappedUnderDebris() ? WEIGHT_TRAPPED_DEBRIS : 0;
        }

        @Override
        public String getLabel() {
            return "Trapped under debris";
        }
    }

    private static class VulnerableOccupantsRule implements PriorityRule {
        @Override
        public int apply(RescueRequest request, Disaster disaster) {
            int score = 0;
            if (request.getChildrenCount() > 0) {
                score += WEIGHT_CHILDREN_PRESENT;
            }
            if (request.getElderlyCount() > 0) {
                score += WEIGHT_ELDERLY_PRESENT;
            }
            return score;
        }

        @Override
        public String getLabel() {
            return "Children / elderly present";
        }
    }

    private static class CrowdSizeRule implements PriorityRule {
        @Override
        public int apply(RescueRequest request, Disaster disaster) {
            if (request.getPeopleCount() >= 5) {
                return WEIGHT_CROWD_LARGE;
            }
            if (request.getPeopleCount() >= 2) {
                return WEIGHT_CROWD_SMALL;
            }
            return 0;
        }

        @Override
        public String getLabel() {
            return "Crowd size";
        }
    }

    private static class DisasterSeverityRule implements PriorityRule {
        @Override
        public int apply(RescueRequest request, Disaster disaster) {
            if (disaster == null || disaster.getSeverity() == null) {
                return 0;
            }
            return severityPoints(disaster.getSeverity());
        }

        private int severityPoints(DisasterSeverity severity) {
            return severity.getWeight() * SEVERITY_MULTIPLIER;
        }

        @Override
        public String getLabel() {
            return "Disaster severity";
        }
    }
}
