package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.Disaster;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.Victim;
import com.resqhub.service.DisasterService;
import com.resqhub.service.RescueRequestService;
import com.resqhub.service.RescueTeamService;
import com.resqhub.service.VictimService;

/** Live operational summary for the Overview screen. */
public class StatsController {

    /** Aggregated snapshot; message and payload carry the same text. */
    public ActionResult getSummary() {
        try {
            DisasterService disasterService = new DisasterService();
            RescueRequestService requestService = new RescueRequestService();
            RescueTeamService teamService = new RescueTeamService();
            VictimService victimService = new VictimService();

            List<Disaster> allDisasters = disasterService.getAllDisasters();
            int open = 0;
            for (Disaster disaster : allDisasters) {
                if (!"Resolved".equals(disaster.getStatus().getLabel())) {
                    open++;
                }
            }

            List<Victim> victims = victimService.getAllVictims();
            int critical = 0;
            for (Victim victim : victims) {
                if (victim.getEmergencyStatus()
                        == com.resqhub.model.EmergencyStatus.CRITICAL) {
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

            String text = "OPEN DISASTERS        : " + open
                    + "  (of " + allDisasters.size() + " total)\n"
                    + "CRITICAL VICTIMS      : " + critical
                    + "  (of " + victims.size() + " registered)\n"
                    + "PENDING RESCUE REQUESTS: " + requestService.countPending() + "\n"
                    + "TEAMS AVAILABLE       : " + available + "\n"
                    + "TEAMS DEPLOYED        : " + deployed
                    + "  (of " + teams.size() + " total)";

            return ActionResult.successWithData(text, text);
        } catch (DataAccessException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }
}
