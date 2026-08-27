package com.resqhub.service;

import java.util.ArrayList;
import java.util.List;

import com.resqhub.dao.RescueAssignmentDAO;
import com.resqhub.dao.RescueTeamDAO;
import com.resqhub.dao.TeamEquipmentDAO;
import com.resqhub.dao.TeamMemberDAO;
import com.resqhub.dao.TeamSkillDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidTeamDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.RescueRequest;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RoleType;
import com.resqhub.model.TeamEquipment;
import com.resqhub.model.TeamMember;
import com.resqhub.model.TeamOperationalStatus;
import com.resqhub.model.TeamSkill;
import com.resqhub.model.TeamType;
import com.resqhub.util.ValidationUtil;

/**
 * Rescue team registration, availability management, member/skill/equipment
 * management, suitability matching and team statistics.
 * Write access: ADMIN, RESCUE_OFFICER.
 */
public class RescueTeamService {

    private final RescueTeamDAO teamDAO = new RescueTeamDAO();
    private final TeamMemberDAO memberDAO = new TeamMemberDAO();
    private final TeamSkillDAO skillDAO = new TeamSkillDAO();
    private final TeamEquipmentDAO equipmentDAO = new TeamEquipmentDAO();
    private final RescueAssignmentDAO assignmentDAO =
            new RescueAssignmentDAO();
    private final SessionManager session = SessionManager.getInstance();

    // ── team registration ────────────────────────────────────────────

    public RescueTeam registerTeam(String teamName, TeamType teamType,
                                   String leaderName, String contactNumber,
                                   int memberCount, String skills,
                                   String equipment, String baseLocation)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);

        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(teamName)
                || teamName.trim().length() < 3) {
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
            throw new InvalidTeamDataException(
                    String.join("; ", errors));
        }

        for (RescueTeam existing : teamDAO.findAll()) {
            if (existing.getTeamName()
                    .equalsIgnoreCase(teamName.trim())) {
                throw new InvalidTeamDataException(
                        "team name already registered: "
                                + teamName.trim());
            }
        }

        RescueTeam team = new RescueTeam(
                ValidationUtil.clean(teamName), teamType,
                ValidationUtil.clean(leaderName),
                contactNumber.trim());
        team.setMemberCount(memberCount);
        team.setSkills(ValidationUtil.clean(skills));
        team.setEquipment(ValidationUtil.clean(equipment));
        team.setBaseLocation(ValidationUtil.clean(baseLocation));

        return teamDAO.save(team);
    }

    // ── team CRUD ────────────────────────────────────────────────────

    public RescueTeam getTeam(long teamId)
            throws InvalidTeamDataException, DataAccessException {
        RescueTeam team = teamDAO.findById(teamId);
        if (team == null) {
            throw new InvalidTeamDataException(
                    "No rescue team with id " + teamId);
        }
        return team;
    }

    public RescueTeam updateTeam(RescueTeam team)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (team == null || team.getId() == null) {
            throw new InvalidTeamDataException(
                    "Cannot update an unsaved team");
        }
        validateTeamFields(team);
        for (RescueTeam other : teamDAO.findAll()) {
            if (!other.getId().equals(team.getId())
                    && other.getTeamName().equalsIgnoreCase(
                            team.getTeamName().trim())) {
                throw new InvalidTeamDataException(
                        "team name already registered: "
                                + team.getTeamName().trim());
            }
        }
        return teamDAO.save(team);
    }

    public void deleteTeam(long teamId)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN);
        try {
            if (!teamDAO.deleteById(teamId)) {
                throw new InvalidTeamDataException(
                        "No team with id " + teamId);
            }
        } catch (DataAccessException e) {
            throw new InvalidTeamDataException(
                    "Cannot delete team #" + teamId
                            + " - it has rescue assignment history");
        }
    }

    public List<RescueTeam> getAllTeams() throws DataAccessException {
        return teamDAO.findAll();
    }

    // ── availability / operational status ─────────────────────────────

    public RescueTeam setAvailability(long teamId,
            AvailabilityStatus status)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        RescueTeam team = requireExisting(teamId);
        team.setAvailabilityStatus(status);
        return teamDAO.save(team);
    }

    public RescueTeam setOperationalStatus(long teamId,
            TeamOperationalStatus status)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        RescueTeam team = requireExisting(teamId);
        team.setOperationalStatus(status);
        return teamDAO.save(team);
    }

    // ── search / filter ──────────────────────────────────────────────

    public List<RescueTeam> searchTeams(String keyword)
            throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return teamDAO.findAll();
        }
        return teamDAO.search(keyword.trim());
    }

    public List<RescueTeam> filterByAvailability(
            AvailabilityStatus status) throws DataAccessException {
        return teamDAO.findByAvailability(status);
    }

    public List<RescueTeam> filterByOperationalStatus(
            TeamOperationalStatus status) throws DataAccessException {
        return teamDAO.findByOperationalStatus(status);
    }

    // ── team members ─────────────────────────────────────────────────

    public List<TeamMember> getMembers(long teamId)
            throws DataAccessException {
        return memberDAO.findByTeam(teamId);
    }

    public TeamMember addMember(long teamId, String memberName,
            String role, String contactNumber, String specialSkills)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        requireExisting(teamId);

        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(memberName)) {
            errors.add("member name cannot be empty");
        }
        if (!ValidationUtil.requireNonBlank(role)) {
            errors.add("member role cannot be empty");
        }
        if (contactNumber != null && !contactNumber.trim().isEmpty()
                && !ValidationUtil.isValidPhone(contactNumber)) {
            errors.add("contact number must be 10 digits");
        }
        if (!errors.isEmpty()) {
            throw new InvalidTeamDataException(
                    String.join("; ", errors));
        }

        TeamMember member = new TeamMember(teamId,
                ValidationUtil.clean(memberName),
                ValidationUtil.clean(role));
        member.setContactNumber(
                contactNumber == null ? null : contactNumber.trim());
        member.setSpecialSkills(ValidationUtil.clean(specialSkills));
        return memberDAO.save(member);
    }

    public TeamMember updateMember(TeamMember member)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (member == null || member.getId() == null) {
            throw new InvalidTeamDataException(
                    "Cannot update an unsaved member");
        }
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(member.getMemberName())) {
            errors.add("member name cannot be empty");
        }
        if (!ValidationUtil.requireNonBlank(member.getRole())) {
            errors.add("member role cannot be empty");
        }
        if (!errors.isEmpty()) {
            throw new InvalidTeamDataException(
                    String.join("; ", errors));
        }
        return memberDAO.save(member);
    }

    public void deleteMember(long memberId)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (!memberDAO.deleteById(memberId)) {
            throw new InvalidTeamDataException(
                    "No member with id " + memberId);
        }
    }

    // ── team skills ──────────────────────────────────────────────────

    public List<TeamSkill> getSkills(long teamId)
            throws DataAccessException {
        return skillDAO.findByTeam(teamId);
    }

    public TeamSkill addSkill(long teamId, String skillName,
            String description)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        requireExisting(teamId);
        if (!ValidationUtil.requireNonBlank(skillName)) {
            throw new InvalidTeamDataException(
                    "skill name cannot be empty");
        }
        TeamSkill skill = new TeamSkill(teamId,
                ValidationUtil.clean(skillName));
        skill.setDescription(ValidationUtil.clean(description));
        return skillDAO.save(skill);
    }

    public void deleteSkill(long skillId)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (!skillDAO.deleteById(skillId)) {
            throw new InvalidTeamDataException(
                    "No skill with id " + skillId);
        }
    }

    // ── team equipment ───────────────────────────────────────────────

    public List<TeamEquipment> getEquipment(long teamId)
            throws DataAccessException {
        return equipmentDAO.findByTeam(teamId);
    }

    public TeamEquipment addEquipment(long teamId,
            String equipmentName, int quantity, String description)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        requireExisting(teamId);
        if (!ValidationUtil.requireNonBlank(equipmentName)) {
            throw new InvalidTeamDataException(
                    "equipment name cannot be empty");
        }
        if (quantity < 1) {
            throw new InvalidTeamDataException(
                    "quantity must be at least 1");
        }
        TeamEquipment eq = new TeamEquipment(teamId,
                ValidationUtil.clean(equipmentName), quantity);
        eq.setDescription(ValidationUtil.clean(description));
        return equipmentDAO.save(eq);
    }

    public void deleteEquipment(long equipmentId)
            throws UnauthorizedOperationException,
            InvalidTeamDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (!equipmentDAO.deleteById(equipmentId)) {
            throw new InvalidTeamDataException(
                    "No equipment with id " + equipmentId);
        }
    }

    // ── suitability matching ─────────────────────────────────────────

    /**
     * Finds the best team for a rescue request based on: availability,
     * skills match, equipment match, and location proximity.
     * Returns an empty list when no suitable team is available.
     */
    public List<RescueTeam> findSuitableTeams(RescueRequest request)
            throws DataAccessException {

        List<RescueTeam> available = teamDAO.findAvailable();
        List<RescueTeam> suitable = new ArrayList<>();

        String requiredAssistance =
                request.getRequiredAssistance() == null
                        ? "" : request.getRequiredAssistance().toLowerCase();
        String location = request.getLocation() == null
                ? "" : request.getLocation().toLowerCase();

        for (RescueTeam team : available) {
            int score = 0;

            if (team.isAvailable()) {
                score += 10;
            }

            String teamSkills = team.getSkills() == null
                    ? "" : team.getSkills().toLowerCase();
            String[] keywords = requiredAssistance.split("[\\s,;]+");
            for (String kw : keywords) {
                if (kw.length() > 2 && teamSkills.contains(kw)) {
                    score += 5;
                }
            }

            String teamLocation = team.getBaseLocation() == null
                    ? "" : team.getBaseLocation().toLowerCase();
            if (!location.isEmpty() && !teamLocation.isEmpty()
                    && location.contains(teamLocation)
                    || teamLocation.contains(location)) {
                score += 3;
            }

            if (request.isLifeThreatening()
                    || request.isMedicalEmergency()) {
                String type = team.getTeamType() == null
                        ? "" : team.getTeamType().name();
                if ("MEDICAL".equals(type)
                        || "NDRF".equals(type)
                        || "FIRE_RESCUE".equals(type)) {
                    score += 4;
                }
            }

            if (team.getMemberCount() >= request.getPeopleCount()) {
                score += 2;
            }

            if (score > 10) {
                suitable.add(team);
            }
        }

        try {
            suitable.sort((a, b) -> {
                try {
                    int countA = assignmentDAO.countByTeam(a.getId());
                    int countB = assignmentDAO.countByTeam(b.getId());
                    return Integer.compare(countA, countB);
                } catch (DataAccessException e) {
                    return 0;
                }
            });
        } catch (Exception ignored) { }

        return suitable;
    }

    // ── team statistics ──────────────────────────────────────────────

    public int countByAvailability(AvailabilityStatus status)
            throws DataAccessException {
        return teamDAO.findByAvailability(status).size();
    }

    public int countByOperationalStatus(
            TeamOperationalStatus status) throws DataAccessException {
        return teamDAO.findByOperationalStatus(status).size();
    }

    // ── helpers ──────────────────────────────────────────────────────

    public String formatForSelection(RescueTeam team) {
        String skills = team.getSkills() == null
                ? "general" : team.getSkills();
        return team.getTeamName() + " (" + team.getMemberCount()
                + " members, " + skills + ")";
    }

    private RescueTeam requireExisting(long teamId)
            throws InvalidTeamDataException, DataAccessException {
        RescueTeam team = teamDAO.findById(teamId);
        if (team == null) {
            throw new InvalidTeamDataException(
                    "No rescue team with id " + teamId);
        }
        return team;
    }

    private void validateTeamFields(RescueTeam team)
            throws InvalidTeamDataException {
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.requireNonBlank(team.getTeamName())
                || team.getTeamName().trim().length() < 3) {
            errors.add("team name must be at least 3 characters");
        }
        if (team.getTeamType() == null) {
            errors.add("team type must be selected");
        }
        if (!ValidationUtil.isValidName(team.getLeaderName())) {
            errors.add("leader name is invalid");
        }
        if (!ValidationUtil.isValidPhone(team.getContactNumber())) {
            errors.add("contact number must be 10 digits");
        }
        if (!ValidationUtil.isPositive(team.getMemberCount())) {
            errors.add("member count must be at least 1");
        }
        if (!errors.isEmpty()) {
            throw new InvalidTeamDataException(
                    String.join("; ", errors));
        }
    }
}
