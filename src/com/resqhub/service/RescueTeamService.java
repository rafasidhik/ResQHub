package com.resqhub.service;

import java.util.ArrayList;
import java.util.List;

import com.resqhub.dao.RescueTeamDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidTeamDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RoleType;
import com.resqhub.model.TeamType;
import com.resqhub.util.ValidationUtil;

/**
 * Rescue team registration and availability management.
 * Write access: ADMIN, RESCUE_OFFICER.
 */
public class RescueTeamService {

    private final RescueTeamDAO teamDAO = new RescueTeamDAO();
    private final SessionManager session = SessionManager.getInstance();

    public RescueTeam registerTeam(String teamName, TeamType teamType,
                                   String leaderName, String contactNumber,
                                   int memberCount, String skills,
                                   String equipment, String baseLocation)
            throws UnauthorizedOperationException, InvalidTeamDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);

        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(teamName) || teamName.trim().length() < 3) {
            errors.add("team name must be at least 3 characters");
        }
        if (teamType == null) {
            errors.add("team type must be selected");
        }
        if (!ValidationUtil.isValidName(leaderName)) {
            errors.add("leader name is invalid");
        }
        if (!ValidationUtil.isValidPhone(contactNumber)) {
            errors.add("contact number must be 10 digits");
        }
        if (!ValidationUtil.isPositive(memberCount)) {
            errors.add("member count must be at least 1");
        }
        if (!errors.isEmpty()) {
            throw new InvalidTeamDataException(String.join("; ", errors));
        }

        for (RescueTeam existing : teamDAO.findAll()) {
            if (existing.getTeamName().equalsIgnoreCase(teamName.trim())) {
                throw new InvalidTeamDataException(
                        "team name already registered: " + teamName.trim());
            }
        }

        RescueTeam team = new RescueTeam(ValidationUtil.clean(teamName), teamType,
                ValidationUtil.clean(leaderName), contactNumber.trim());
        team.setMemberCount(memberCount);
        team.setSkills(ValidationUtil.clean(skills));
        team.setEquipment(ValidationUtil.clean(equipment));
        team.setBaseLocation(ValidationUtil.clean(baseLocation));

        return teamDAO.save(team);
    }

    public RescueTeam setAvailability(long teamId, AvailabilityStatus status)
            throws UnauthorizedOperationException, InvalidTeamDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        RescueTeam team = requireExisting(teamId);
        team.setAvailabilityStatus(status);
        return teamDAO.save(team);
    }

    private RescueTeam requireExisting(long teamId)
            throws InvalidTeamDataException, DataAccessException {
        RescueTeam team = teamDAO.findById(teamId);
        if (team == null) {
            throw new InvalidTeamDataException("No rescue team with id " + teamId);
        }
        return team;
    }

    public List<RescueTeam> getAvailableTeams() throws DataAccessException {
        return teamDAO.findAvailable();
    }

    public List<RescueTeam> getAllTeams() throws DataAccessException {
        return teamDAO.findAll();
    }

    /** Feeds the GUI combo box when assigning a team to a request. */
    public String formatForSelection(RescueTeam team) {
        String skills = team.getSkills() == null ? "general" : team.getSkills();
        return team.getTeamName() + " (" + team.getMemberCount() + " members, "
                + skills + ")";
    }

    /** ADMIN-only hard delete; blocked when assignment history exists. */
    public void deleteTeam(long teamId)
            throws UnauthorizedOperationException, InvalidTeamDataException,
            DataAccessException {

        session.requireRole(RoleType.ADMIN);
        try {
            if (!teamDAO.deleteById(teamId)) {
                throw new InvalidTeamDataException("No team with id " + teamId);
            }
        } catch (DataAccessException e) {
            throw new InvalidTeamDataException(
                    "Cannot delete team #" + teamId
                            + " - it has rescue assignment history");
        }
    }
}
