package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.Victim;
import com.resqhub.service.AccountDeletionRequestService;
import com.resqhub.service.DisasterService;
import com.resqhub.service.RescueRequestService;
import com.resqhub.service.RescueTeamService;
import com.resqhub.service.VictimService;

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

        Snapshot(int activeDisasters, int totalDisasters,
                 int criticalVictims, int totalVictims,
                 int pendingRequests, int criticalRequests,
                 int availableTeams, int deployedTeams,
                 int totalTeams, int pendingDeletions) {
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
                    new AccountDeletionRequestService().countPending());
            return ActionResult.successWithData("Live snapshot", snapshot);
        } catch (DataAccessException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
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
