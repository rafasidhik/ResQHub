package com.resqhub.util;

/**
 * Text-based location proximity scoring for donors vs blood-request
 * locations when exact GPS coordinates are not available (spec: Location-
 * Based Matching, Donor Ranking).
 *
 * Uses substring, token and district-level heuristics to assign a
 * proximity score between 0 (very distant) and 100 (exact/same location).
 */
public final class LocationProximityUtil {

    private LocationProximityUtil() { }

    /**
     * Proximity score 0-100 between two free-text locations.
     * 100 = exact or containment match; 75 = same district/city token;
     * 50 = share a meaningful word; 25 = different location but same
     * state/region; 0 = completely different or blank.
     */
    public static int score(String donorLoc, String requestLoc) {
        if (donorLoc == null || requestLoc == null) {
            return 0;
        }
        String a = donorLoc.trim().toLowerCase();
        String b = requestLoc.trim().toLowerCase();
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        if (a.equals(b)) {
            return 100;
        }
        if (a.contains(b) || b.contains(a)) {
            return 95;
        }
        // Token overlap (district, city, etc.)
        String[] tokensA = a.split("[,\\s]+");
        String[] tokensB = b.split("[,\\s]+");
        int shared = 0;
        for (String ta : tokensA) {
            if (ta.length() < 3) continue;
            for (String tb : tokensB) {
                if (tb.length() < 3) continue;
                if (ta.equals(tb) || ta.contains(tb) || tb.contains(ta)) {
                    shared++;
                    break;
                }
            }
        }
        if (shared >= 2) {
            return 75;
        }
        if (shared == 1) {
            return 50;
        }
        // Last resort: check for Kerala / state-level commonality
        if (a.contains("kerala") && b.contains("kerala")) {
            return 25;
        }
        return 0;
    }

    /**
     * Rank label for UI display.
     */
    public static String rankLabel(int score) {
        if (score >= 90) return "Same Location";
        if (score >= 70) return "Nearby";
        if (score >= 45) return "Same District";
        if (score >= 20) return "Same Region";
        return "Distant";
    }
}
