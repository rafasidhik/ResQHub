package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.TeamType;
import com.resqhub.service.RescueTeamService;
import com.resqhub.util.InputParser;

/** Rescue team screen controller. */
public class RescueTeamController {

    private final RescueTeamService teamService = new RescueTeamService();

    public ActionResult registerTeam(String teamName, TeamType teamType,
                                     String leaderName, String contactNumber,
                                     String memberCountText, String skills,
                                     String equipment, String baseLocation) {
        try {
            int memberCount = InputParser.parseInt(memberCountText,
                    "Member count");
            RescueTeam team = teamService.registerTeam(teamName, teamType,
                    leaderName, contactNumber, memberCount, skills, equipment,
                    baseLocation);
            return ActionResult.successWithData(
                    "Team registered as #" + team.getId()
                            + " (" + team.getAvailabilityStatus().getLabel() + ")",
                    team);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult setAvailability(long teamId, AvailabilityStatus status) {
        try {
            RescueTeam updated = teamService.setAvailability(teamId, status);
            return ActionResult.success("Team " + updated.getTeamName()
                    + " is now " + updated.getAvailabilityStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteTeam(long teamId) {
        try {
            teamService.deleteTeam(teamId);
            return ActionResult.success("Team #" + teamId + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure("Unexpected error deleting team: "
                    + e.getMessage());
        }
    }

    public List<RescueTeam> getAvailableTeams() throws DataAccessException {
        return teamService.getAvailableTeams();
    }

    public List<RescueTeam> getAllTeams() throws DataAccessException {
        return teamService.getAllTeams();
    }

    /** Combo-box text when choosing a team for assignment. */
    public static String toOption(RescueTeam t) {
        return teamServiceStaticFormat(t);
    }

    private static String teamServiceStaticFormat(RescueTeam t) {
        String skills = t.getSkills() == null ? "general" : t.getSkills();
        return "#" + t.getId() + " " + t.getTeamName()
                + " (" + t.getMemberCount() + " members, "
                + t.getAvailabilityStatus().getLabel() + ", " + skills + ")";
    }

    public static Object[] toRow(RescueTeam t) {
        return new Object[] {
                t.getId(),
                t.getTeamName(),
                t.getTeamType().getLabel(),
                t.getLeaderName(),
                t.getMemberCount(),
                t.getAvailabilityStatus().getLabel(),
                t.getBaseLocation() == null ? "-" : t.getBaseLocation()
        };
    }

    public static String[] tableHeaders() {
        return new String[] {"ID", "Team", "Type", "Leader", "Members",
                "Availability", "Base"};
    }
}
