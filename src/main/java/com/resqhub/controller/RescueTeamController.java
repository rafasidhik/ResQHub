package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.TeamEquipment;
import com.resqhub.model.TeamMember;
import com.resqhub.model.TeamOperationalStatus;
import com.resqhub.model.TeamSkill;
import com.resqhub.model.TeamType;
import com.resqhub.service.RescueTeamService;
import com.resqhub.util.InputParser;

/** Rescue team screen controller. */
public class RescueTeamController {

    private final RescueTeamService teamService = new RescueTeamService();

    // ── team CRUD ────────────────────────────────────────────────────

    public ActionResult registerTeam(String teamName, TeamType teamType,
            String leaderName, String contactNumber,
            String memberCountText, String skills,
            String equipment, String baseLocation) {
        try {
            int memberCount = InputParser.parseInt(memberCountText,
                    "Member count");
            RescueTeam team = teamService.registerTeam(teamName, teamType,
                    leaderName, contactNumber, memberCount, skills,
                    equipment, baseLocation);
            return ActionResult.successWithData(
                    "Team registered as #" + team.getId()
                            + " ("
                            + team.getAvailabilityStatus().getLabel()
                            + ")",
                    team);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateTeam(long teamId, String teamName,
            TeamType teamType, String leaderName,
            String contactNumber, String memberCountText,
            String skills, String equipment, String baseLocation) {
        try {
            RescueTeam existing = teamService.getTeam(teamId);
            if (existing == null) {
                return ActionResult.failure(
                        "No team with id " + teamId);
            }
            int memberCount = InputParser.parseInt(memberCountText,
                    "Member count");
            existing.setTeamName(teamName);
            existing.setTeamType(teamType);
            existing.setLeaderName(leaderName);
            existing.setContactNumber(contactNumber);
            existing.setMemberCount(memberCount);
            existing.setSkills(skills);
            existing.setEquipment(equipment);
            existing.setBaseLocation(baseLocation);

            RescueTeam saved = teamService.updateTeam(existing);
            return ActionResult.success(
                    "Team #" + saved.getId() + " updated");
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteTeam(long teamId) {
        try {
            teamService.deleteTeam(teamId);
            return ActionResult.success(
                    "Team #" + teamId + " deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error deleting team: "
                            + e.getMessage());
        }
    }

    // ── availability / operational status ─────────────────────────────

    public ActionResult setAvailability(long teamId,
            AvailabilityStatus status) {
        try {
            RescueTeam updated = teamService.setAvailability(
                    teamId, status);
            return ActionResult.success(updated.getTeamName()
                    + " is now "
                    + updated.getAvailabilityStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult setOperationalStatus(long teamId,
            TeamOperationalStatus status) {
        try {
            RescueTeam updated = teamService.setOperationalStatus(
                    teamId, status);
            return ActionResult.success(updated.getTeamName()
                    + " operational status: "
                    + updated.getOperationalStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    // ── search / filter ──────────────────────────────────────────────

    public List<RescueTeam> searchTeams(String keyword)
            throws DataAccessException {
        return teamService.searchTeams(keyword);
    }

    public List<RescueTeam> getAvailableTeams()
            throws DataAccessException {
        return teamService.getAllTeams();
    }

    public List<RescueTeam> getAllTeams() throws DataAccessException {
        return teamService.getAllTeams();
    }

    // ── team members ─────────────────────────────────────────────────

    public ActionResult addMember(long teamId, String memberName,
            String role, String contactNumber, String specialSkills) {
        try {
            TeamMember member = teamService.addMember(teamId,
                    memberName, role, contactNumber, specialSkills);
            return ActionResult.successWithData(
                    member.getMemberName() + " added to team #"
                            + teamId,
                    member);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateMember(TeamMember member) {
        try {
            TeamMember saved = teamService.updateMember(member);
            return ActionResult.success(
                    saved.getMemberName() + " updated");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteMember(long memberId) {
        try {
            teamService.deleteMember(memberId);
            return ActionResult.success("Member deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public List<TeamMember> getMembers(long teamId)
            throws DataAccessException {
        return teamService.getMembers(teamId);
    }

    // ── team skills ──────────────────────────────────────────────────

    public ActionResult addSkill(long teamId, String skillName,
            String description) {
        try {
            TeamSkill skill = teamService.addSkill(teamId,
                    skillName, description);
            return ActionResult.successWithData(
                    skill.getSkillName() + " added", skill);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteSkill(long skillId) {
        try {
            teamService.deleteSkill(skillId);
            return ActionResult.success("Skill deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public List<TeamSkill> getSkills(long teamId)
            throws DataAccessException {
        return teamService.getSkills(teamId);
    }

    // ── team equipment ───────────────────────────────────────────────

    public ActionResult addEquipment(long teamId,
            String equipmentName, String quantityText,
            String description) {
        try {
            int quantity = InputParser.parseInt(quantityText,
                    "Quantity");
            TeamEquipment eq = teamService.addEquipment(teamId,
                    equipmentName, quantity, description);
            return ActionResult.successWithData(
                    eq.getEquipmentName() + " added", eq);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteEquipment(long equipmentId) {
        try {
            teamService.deleteEquipment(equipmentId);
            return ActionResult.success("Equipment deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public List<TeamEquipment> getEquipment(long teamId)
            throws DataAccessException {
        return teamService.getEquipment(teamId);
    }

    // ── suitability ──────────────────────────────────────────────────

    public List<RescueTeam> findSuitableTeams(RescueRequest request)
            throws DataAccessException {
        return teamService.findSuitableTeams(request);
    }

    // ── display helpers ──────────────────────────────────────────────

    public static String toOption(RescueTeam t) {
        String skills = t.getSkills() == null
                ? "general" : t.getSkills();
        return "#" + t.getId() + " " + t.getTeamName()
                + " (" + t.getMemberCount() + " members, "
                + t.getAvailabilityStatus().getLabel()
                + ", " + skills + ")";
    }

    public static Object[] toRow(RescueTeam t) {
        return new Object[]{
                t.getId(),
                t.getTeamName(),
                t.getTeamType().getLabel(),
                t.getLeaderName(),
                t.getMemberCount(),
                t.getAvailabilityStatus().getLabel(),
                t.getOperationalStatus() == null
                        ? "-" : t.getOperationalStatus().getLabel(),
                t.getBaseLocation() == null
                        ? "-" : t.getBaseLocation()
        };
    }

    public static String[] tableHeaders() {
        return new String[]{"ID", "Team", "Type", "Leader", "Members",
                "Availability", "Operational", "Base"};
    }

    public static Object[] toMemberRow(TeamMember m) {
        return new Object[]{
                m.getId(),
                m.getMemberName(),
                m.getRole(),
                m.getContactNumber() == null
                        ? "-" : m.getContactNumber(),
                m.getSpecialSkills() == null
                        ? "-" : m.getSpecialSkills(),
                m.getAvailability().getLabel()
        };
    }

    public static String[] memberTableHeaders() {
        return new String[]{"ID", "Name", "Role", "Contact",
                "Skills", "Status"};
    }

    public static Object[] toSkillRow(TeamSkill s) {
        return new Object[]{
                s.getId(),
                s.getSkillName(),
                s.getDescription() == null
                        ? "-" : s.getDescription()
        };
    }

    public static String[] skillTableHeaders() {
        return new String[]{"ID", "Skill", "Description"};
    }

    public static Object[] toEquipmentRow(TeamEquipment e) {
        return new Object[]{
                e.getId(),
                e.getEquipmentName(),
                e.getQuantity(),
                e.getDescription() == null
                        ? "-" : e.getDescription()
        };
    }

    public static String[] equipmentTableHeaders() {
        return new String[]{"ID", "Equipment", "Qty",
                "Description"};
    }
}
