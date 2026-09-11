package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.Victim;
import com.resqhub.model.VolunteerAvailability;
import com.resqhub.service.AccountDeletionRequestService;
import com.resqhub.service.BloodDonorService;
import com.resqhub.service.DisasterService;
import com.resqhub.service.FoodDistributionService;
import com.resqhub.service.HospitalService;
import com.resqhub.service.RescueRequestService;
import com.resqhub.service.RescueTeamService;
import com.resqhub.service.ResourceService;
import com.resqhub.service.ShelterService;
import com.resqhub.service.VictimService;
import com.resqhub.service.VolunteerService;

/**
 * Live operational numbers for the Overview landing screen.
 * getSnapshot() powers the GUI cards; getSummary() keeps the
 * plain-text format used by integration tests.
 */
public class StatsController {

    /** Immutable count snapshot rendered as overview cards. */
    public static class Snapshot {
        public final int activeDisasters;
        public final int totalDisasters;
        public final int criticalVictims;
        public final int totalVictims;
        public final int pendingRequests;
        public final int criticalRequests;
        public final int availableTeams;
        public final int deployedTeams;
        public final int totalTeams;
        public final int pendingDeletions;

        // cross-module visibility
        public final int totalShelters;
        public final int sheltersAccepting;
        public final int sheltersNearCapacity;
        public final int foodOpenRequests;
        public final int foodShortages;
        public final int bloodShortages;
        public final int bloodOpenRequests;
        public final int eligibleDonors;
        public final int totalHospitals;
        public final int hospitalsAccepting;
        public final int openReferrals;
        public final int totalVolunteers;
        public final int volunteersAvailable;
        public final int totalResources;
        public final int resourcesLow;
        public final int resourcesOut;

        Snapshot(int activeDisasters, int totalDisasters,
                 int criticalVictims, int totalVictims,
                 int pendingRequests, int criticalRequests,
                 int availableTeams, int deployedTeams,
                 int totalTeams, int pendingDeletions,
                 int totalShelters, int sheltersAccepting,
                 int sheltersNearCapacity, int foodOpenRequests,
                 int foodShortages, int bloodShortages,
                 int bloodOpenRequests, int eligibleDonors,
                 int totalHospitals, int hospitalsAccepting,
                 int openReferrals, int totalVolunteers,
                 int volunteersAvailable, int totalResources,
                 int resourcesLow, int resourcesOut) {
            this.activeDisasters = activeDisasters;
            this.totalDisasters = totalDisasters;
            this.criticalVictims = criticalVictims;
            this.totalVictims = totalVictims;
            this.pendingRequests = pendingRequests;
            this.criticalRequests = criticalRequests;
            this.availableTeams = availableTeams;
            this.deployedTeams = deployedTeams;
            this.totalTeams = totalTeams;
            this.pendingDeletions = pendingDeletions;
            this.totalShelters = totalShelters;
            this.sheltersAccepting = sheltersAccepting;
            this.sheltersNearCapacity = sheltersNearCapacity;
            this.foodOpenRequests = foodOpenRequests;
            this.foodShortages = foodShortages;
            this.bloodShortages = bloodShortages;
            this.bloodOpenRequests = bloodOpenRequests;
            this.eligibleDonors = eligibleDonors;
            this.totalHospitals = totalHospitals;
            this.hospitalsAccepting = hospitalsAccepting;
            this.openReferrals = openReferrals;
            this.totalVolunteers = totalVolunteers;
            this.volunteersAvailable = volunteersAvailable;
            this.totalResources = totalResources;
            this.resourcesLow = resourcesLow;
            this.resourcesOut = resourcesOut;
        }
    }

    public ActionResult getSnapshot() {
        try {
            DisasterService disasterService = new DisasterService();
            RescueRequestService requestService = new RescueRequestService();
            RescueTeamService teamService = new RescueTeamService();
            VictimService victimService = new VictimService();

            List<Disaster> disasters = disasterService.getAllDisasters();
            int active = 0;
            for (Disaster disaster : disasters) {
                if (disaster.getStatus() != DisasterStatus.RESOLVED) {
                    active++;
                }
            }

            List<Victim> victims = victimService.getAllVictims();
            int critical = 0;
            for (Victim victim : victims) {
                if (victim.getEmergencyStatus() == EmergencyStatus.CRITICAL) {
                    critical++;
                }
            }

            List<RescueTeam> teams = teamService.getAllTeams();
            int available = 0;
            int deployed = 0;
            for (RescueTeam team : teams) {
                if (team.getAvailabilityStatus()
                        == AvailabilityStatus.AVAILABLE) {
                    available++;
                } else if (team.getAvailabilityStatus()
                        == AvailabilityStatus.DEPLOYED) {
                    deployed++;
                }
            }

            Snapshot snapshot = new Snapshot(active, disasters.size(),
                    critical, victims.size(), requestService.countPending(),
                    requestService.countCritical(),
                    available, deployed, teams.size(),
                    new AccountDeletionRequestService().countPending(),

                    // shelters
                    shelterCountAll(), shelterCountAccepting(), shelterCountNear(),
                    // food
                    foodOpenCount(), foodShortageCount(),
                    // blood
                    bloodShortageCount(), bloodOpenCount(), bloodEligibleCount(),
                    // hospitals
                    hospitalCountAll(), hospitalCountAccepting(),
                    hospitalOpenReferrals(),
                    // volunteers
                    volunteerCountAll(), volunteerCountAvailable(),
                    // resources
                    resourceCountAll(), resourceCountLow(), resourceCountOut());
            return ActionResult.successWithData("Live snapshot", snapshot);
        } catch (DataAccessException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    // ---- module counts (each guarded: a permission gap never breaks the
    // overview - a missing module simply reports zero) -------------------

    private interface CountQuery {
        int run() throws Exception;
    }

    private static int quiet(CountQuery query) {
        try {
            return query.run();
        } catch (Exception e) {
            return 0;
        }
    }

    private static int shelterCountAll() {
        return quiet(() -> new ShelterService().getAllShelters().size());
    }

    private static int shelterCountAccepting() {
        return quiet(() -> new ShelterService().getAcceptingShelters().size());
    }

    private static int shelterCountNear() {
        return quiet(() -> new ShelterService().getNearCapacity().size());
    }

    private static int foodOpenCount() {
        return quiet(() -> new FoodDistributionService().findOpen().size());
    }

    private static int foodShortageCount() {
        return quiet(() -> new FoodDistributionService().shortageRequests().size());
    }

    private static int bloodShortageCount() {
        return quiet(() -> new BloodDonorService().countShortages());
    }

    private static int bloodOpenCount() {
        return quiet(() -> new BloodDonorService().getOpenRequests().size());
    }

    private static int bloodEligibleCount() {
        return quiet(() -> new BloodDonorService().countEligibleAvailable());
    }

    private static int hospitalCountAll() {
        return quiet(() -> new HospitalService().getAllHospitals().size());
    }

    private static int hospitalCountAccepting() {
        return quiet(() -> new HospitalService().findAccepting().size());
    }

    private static int hospitalOpenReferrals() {
        return quiet(() -> new HospitalService().getOpenReferrals().size());
    }

    private static int volunteerCountAll() {
        return quiet(() -> new VolunteerService().getAllVolunteers().size());
    }

    private static int volunteerCountAvailable() {
        return quiet(() -> new VolunteerService()
                .countByAvailability(VolunteerAvailability.AVAILABLE));
    }

    private static int resourceCountAll() {
        return quiet(() -> new ResourceService().countResources());
    }

    private static int resourceCountLow() {
        return quiet(() -> new ResourceService().countLowStock());
    }

    private static int resourceCountOut() {
        return quiet(() -> new ResourceService().countOutOfStock());
    }

    /** Plain-text variant of the snapshot (tests / copy-friendly). */
    public ActionResult getSummary() {
        ActionResult result = getSnapshot();
        if (!result.isSuccess()) {
            return result;
        }
        Snapshot s = (Snapshot) result.getData();
        String text = "ACTIVE DISASTERS      : " + s.activeDisasters
                + "  (of " + s.totalDisasters + " total)\n"
                + "CRITICAL VICTIMS      : " + s.criticalVictims
                + "  (of " + s.totalVictims + " registered)\n"
                + "PENDING RESCUE REQUESTS: " + s.pendingRequests + "\n"
                + "CRITICAL REQUESTS     : " + s.criticalRequests + "\n"
                + "TEAMS AVAILABLE       : " + s.availableTeams + "\n"
                + "TEAMS DEPLOYED        : " + s.deployedTeams
                + "  (of " + s.totalTeams + " total)";
        return ActionResult.successWithData(text, text);
    }
}
