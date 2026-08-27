package com.resqhub.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.resqhub.dao.VolunteerActivityDAO;
import com.resqhub.dao.VolunteerAssignmentDAO;
import com.resqhub.dao.VolunteerDAO;
import com.resqhub.dao.VolunteerSkillDAO;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.InvalidVolunteerDataException;
import com.resqhub.exception.UnauthorizedOperationException;
import com.resqhub.model.EmergencyRole;
import com.resqhub.model.Volunteer;
import com.resqhub.model.VolunteerActivity;
import com.resqhub.model.VolunteerAssignment;
import com.resqhub.model.VolunteerAvailability;
import com.resqhub.model.VolunteerSkill;
import com.resqhub.model.VolunteerTaskStatus;
import com.resqhub.model.RoleType;
import com.resqhub.util.ValidationUtil;

/**
 * Volunteer registration, skills, availability, smart assignment,
 * workload tracking and activity history.
 * Write access: ADMIN, RESCUE_OFFICER. Volunteers may read their own
 * tasks and update availability.
 */
public class VolunteerService {

    private final VolunteerDAO volunteerDAO = new VolunteerDAO();
    private final VolunteerSkillDAO skillDAO = new VolunteerSkillDAO();
    private final VolunteerAssignmentDAO assignmentDAO =
            new VolunteerAssignmentDAO();
    private final VolunteerActivityDAO activityDAO =
            new VolunteerActivityDAO();
    private final SessionManager session = SessionManager.getInstance();

    private static final int MAX_ACTIVE_TASKS = 5;

    // ── registration ─────────────────────────────────────────────────

    public Volunteer registerVolunteer(String fullName, String contactNumber,
            String email, String location, String skills,
            VolunteerAvailability availability,
            EmergencyRole emergencyRole, int maxWorkload)
            throws UnauthorizedOperationException,
            InvalidVolunteerDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);

        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.isValidName(fullName)) {
            errors.add("full name is invalid");
        }
        if (!ValidationUtil.isValidPhone(contactNumber)) {
            errors.add("contact number must be 10 digits");
        }
        if (email != null && !email.trim().isEmpty()
                && !ValidationUtil.isValidEmail(email)) {
            errors.add("email is invalid");
        }
        if (!ValidationUtil.requireNonBlank(location)) {
            errors.add("location cannot be empty");
        }
        if (maxWorkload < 1) {
            errors.add("max workload must be at least 1");
        }
        if (!errors.isEmpty()) {
            throw new InvalidVolunteerDataException(
                    String.join("; ", errors));
        }

        for (Volunteer existing : volunteerDAO.findAll()) {
            if (existing.getContactNumber().equals(contactNumber.trim())) {
                throw new InvalidVolunteerDataException(
                        "volunteer already registered with contact "
                                + contactNumber.trim());
            }
        }

        Volunteer v = new Volunteer(ValidationUtil.clean(fullName),
                contactNumber.trim(), ValidationUtil.clean(location));
        v.setEmail(email == null ? null : email.trim());
        v.setSkills(ValidationUtil.clean(skills));
        v.setAvailability(availability == null
                ? VolunteerAvailability.AVAILABLE : availability);
        v.setEmergencyRole(emergencyRole);
        v.setMaxWorkload(maxWorkload);
        // Link to the current auth user if it is a VOLUNTEER role account
        v.setUserId(session.hasRole(RoleType.VOLUNTEER)
                ? session.currentUserId() : null);

        return volunteerDAO.save(v);
    }

    // ── CRUD ─────────────────────────────────────────────────────────

    public Volunteer getVolunteer(long id)
            throws InvalidVolunteerDataException, DataAccessException {
        Volunteer v = volunteerDAO.findById(id);
        if (v == null) {
            throw new InvalidVolunteerDataException(
                    "No volunteer with id " + id);
        }
        return v;
    }

    public Volunteer updateVolunteer(Volunteer v)
            throws UnauthorizedOperationException,
            InvalidVolunteerDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (v == null || v.getId() == null) {
            throw new InvalidVolunteerDataException(
                    "Cannot update an unsaved volunteer");
        }
        List<String> errors = new ArrayList<>();
        if (!ValidationUtil.isValidName(v.getFullName())) {
            errors.add("full name is invalid");
        }
        if (!ValidationUtil.isValidPhone(v.getContactNumber())) {
            errors.add("contact number must be 10 digits");
        }
        if (!ValidationUtil.requireNonBlank(v.getLocation())) {
            errors.add("location cannot be empty");
        }
        if (!errors.isEmpty()) {
            throw new InvalidVolunteerDataException(
                    String.join("; ", errors));
        }
        return volunteerDAO.save(v);
    }

    public void deleteVolunteer(long id)
            throws UnauthorizedOperationException,
            InvalidVolunteerDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN);
        if (!volunteerDAO.deleteById(id)) {
            throw new InvalidVolunteerDataException(
                    "No volunteer with id " + id);
        }
    }

    public List<Volunteer> getAllVolunteers() throws DataAccessException {
        return volunteerDAO.findAll();
    }

    // ── availability / role / location updates ───────────────────────

    /** Both officers and the volunteer themselves can update availability. */
    public Volunteer setAvailability(long volunteerId,
            VolunteerAvailability availability)
            throws UnauthorizedOperationException,
            InvalidVolunteerDataException, DataAccessException {

        if (!session.hasRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER)
                && isSelfVolunteer(volunteerId)) {
            throw new UnauthorizedOperationException(
                    "You can only update your own availability");
        }
        Volunteer v = getVolunteer(volunteerId);
        VolunteerAvailability old = v.getAvailability();
        v.setAvailability(availability);
        Volunteer saved = volunteerDAO.save(v);
        logActivity(saved.getId(), "AVAILABILITY_CHANGED",
                "Availability changed from "
                        + (old == null ? "-" : old.getLabel())
                        + " to " + availability.getLabel());
        return saved;
    }

    private boolean isSelfVolunteer(long volunteerId) {
        Volunteer current = session.getCurrentVolunteer();
        return current != null && current.getId().equals(volunteerId);
    }

    // ── skills ───────────────────────────────────────────────────────

    public List<VolunteerSkill> getSkills(long volunteerId)
            throws DataAccessException {
        return skillDAO.findByVolunteer(volunteerId);
    }

    public VolunteerSkill addSkill(long volunteerId, String skillName)
            throws UnauthorizedOperationException,
            InvalidVolunteerDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        getVolunteer(volunteerId);
        if (!ValidationUtil.requireNonBlank(skillName)) {
            throw new InvalidVolunteerDataException(
                    "skill name cannot be empty");
        }
        VolunteerSkill skill = new VolunteerSkill(volunteerId,
                ValidationUtil.clean(skillName));
        return skillDAO.save(skill);
    }

    public void deleteSkill(long skillId)
            throws UnauthorizedOperationException,
            InvalidVolunteerDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        if (!skillDAO.deleteById(skillId)) {
            throw new InvalidVolunteerDataException(
                    "No skill with id " + skillId);
        }
    }

    // ── search / filter ──────────────────────────────────────────────

    public List<Volunteer> searchVolunteers(String keyword)
            throws DataAccessException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return volunteerDAO.findAll();
        }
        return volunteerDAO.search(keyword.trim());
    }

    public List<Volunteer> filterByAvailability(
            VolunteerAvailability a) throws DataAccessException {
        return volunteerDAO.findByAvailability(a);
    }

    public List<Volunteer> filterByRole(EmergencyRole role)
            throws DataAccessException {
        return volunteerDAO.findByEmergencyRole(role);
    }

    // ── assignments ──────────────────────────────────────────────────

    /**
     * Smart assignment engine. Ranks available volunteers by skill match,
     * location proximity, current workload and emergency role; returns in
     * fitness order. Never returns unavailable or overloaded volunteers.
     */
    public List<VolunteerScore> findSuitableVolunteers(String taskSkills,
            String taskLocation, int taskPriority, int requiredCount)
            throws DataAccessException {

        List<Volunteer> candidates = volunteerDAO.findAvailable();
        List<VolunteerScore> scored = new ArrayList<>();

        String[] requiredSkills = taskSkills == null
                ? new String[0] : taskSkills.toLowerCase()
                        .split("[\\s,;]+");

        for (Volunteer v : candidates) {
            if (v.getAvailability() != VolunteerAvailability.AVAILABLE) {
                continue;
            }
            if (assignmentDAO.countActive(v.getId())
                    >= Math.min(v.getMaxWorkload(), MAX_ACTIVE_TASKS)) {
                continue;
            }

            int score = 0;
            String volunteerSkills = v.getSkills() == null
                    ? "" : v.getSkills().toLowerCase();

            int matched = 0;
            for (String skill : requiredSkills) {
                if (skill.length() > 2
                        && volunteerSkills.contains(skill)) {
                    matched++;
                }
            }
            if (matched > 0 || requiredSkills.length == 0) {
                score += Math.min(matched, 5);
                if (matched > 0) {
                    score += 6;
                }
            } else {
                continue;
            }

            score += 10 - Math.min(assignmentDAO.countActive(v.getId()), 10);

            String volLoc = v.getLocation() == null
                    ? "" : v.getLocation().toLowerCase();
            if (taskLocation != null && !taskLocation.isEmpty()
                    && !volLoc.isEmpty()
                    && (taskLocation.toLowerCase().contains(volLoc)
                        || volLoc.contains(taskLocation.toLowerCase()))) {
                score += 5;
            }

            if (v.getEmergencyRole() != null
                    && v.getEmergencyRole() != EmergencyRole.GENERAL) {
                if (hasRoleKeyword(v.getEmergencyRole(), requiredSkills)) {
                    score += 3;
                }
            }

            scored.add(new VolunteerScore(v, score,
                    assignmentDAO.countActive(v.getId())));
        }

        scored.sort(Comparator.comparingInt(VolunteerScore::score)
                .reversed());

        if (requiredCount > 0 && scored.size() > requiredCount) {
            return scored.subList(0, requiredCount);
        }
        return scored;
    }

    private boolean hasRoleKeyword(EmergencyRole role,
                                   String[] requiredSkills) {
        if (requiredSkills == null) {
            return false;
        }
        String roleText = role.name().toLowerCase();
        for (String skill : requiredSkills) {
            if (roleText.contains(skill)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Assign a task to a specific volunteer after validating they are
     * available and not overloaded. Records assignment activity.
     */
    public VolunteerAssignment assignTask(long volunteerId,
            String taskName, String description, String location,
            int priority)
            throws UnauthorizedOperationException,
            InvalidVolunteerDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        Volunteer v = getVolunteer(volunteerId);

        if (v.getAvailability() != VolunteerAvailability.AVAILABLE) {
            throw new InvalidVolunteerDataException(
                    v.getFullName() + " is not available");
        }
        int active = assignmentDAO.countActive(volunteerId);
        if (active >= Math.min(v.getMaxWorkload(), MAX_ACTIVE_TASKS)) {
            throw new InvalidVolunteerDataException(
                    v.getFullName() + " is at maximum workload ("
                            + active + " active tasks)");
        }
        if (!ValidationUtil.requireNonBlank(taskName)) {
            throw new InvalidVolunteerDataException(
                    "task name cannot be empty");
        }

        VolunteerAssignment a = new VolunteerAssignment();
        a.setVolunteerId(volunteerId);
        a.setTaskName(ValidationUtil.clean(taskName));
        a.setDescription(ValidationUtil.clean(description));
        a.setLocation(ValidationUtil.clean(location));
        a.setPriority(priority);
        a.setStatus(VolunteerTaskStatus.ASSIGNED);
        VolunteerAssignment saved = assignmentDAO.save(a);

        logActivity(volunteerId, "TASK_ASSIGNED",
                "Task assigned: " + taskName);

        if (v.getAvailability() == VolunteerAvailability.AVAILABLE
                && active + 1 >= v.getMaxWorkload()) {
            v.setAvailability(VolunteerAvailability.BUSY);
            volunteerDAO.save(v);
        }
        return saved;
    }

    /** Best-match auto assignment: picks the top candidate for the task. */
    public VolunteerAssignment smartAssign(String taskName,
            String description, String taskLocation, String taskSkills,
            int priority)
            throws UnauthorizedOperationException,
            InvalidVolunteerDataException, DataAccessException {

        session.requireRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER);
        List<VolunteerScore> candidates = findSuitableVolunteers(
                taskSkills, taskLocation, priority, 1);
        if (candidates.isEmpty()) {
            throw new InvalidVolunteerDataException(
                    "No suitable available volunteer for this task");
        }
        Volunteer best = candidates.get(0).volunteer();
        return assignTask(best.getId(), taskName, description,
                taskLocation, priority);
    }

    // ── task status updates (volunteer self-service) ─────────────────

    /**
     * Volunteer updates the status of one of their own tasks.
     * Completing a task frees up workload and logs activity.
     */
    public VolunteerAssignment updateTaskStatus(long assignmentId,
            VolunteerTaskStatus newStatus)
            throws UnauthorizedOperationException,
            InvalidVolunteerDataException, DataAccessException {

        VolunteerAssignment a = assignmentDAO.findById(assignmentId);
        if (a == null) {
            throw new InvalidVolunteerDataException(
                    "No assignment with id " + assignmentId);
        }
        if (!session.hasRole(RoleType.ADMIN, RoleType.RESCUE_OFFICER)
                && !isSelfVolunteer(a.getVolunteerId())) {
            throw new UnauthorizedOperationException(
                    "You can only update your own tasks");
        }

        if (!isForwardTransition(a.getStatus(), newStatus)) {
            throw new InvalidVolunteerDataException(
                    "Invalid transition from "
                            + a.getStatus().getLabel() + " to "
                            + newStatus.getLabel());
        }

        a.setStatus(newStatus);
        if (newStatus == VolunteerTaskStatus.COMPLETED) {
            a.setCompletedAt(java.time.LocalDateTime.now());
        }
        VolunteerAssignment saved = assignmentDAO.save(a);

        String activityType;
        switch (newStatus) {
            case ACCEPTED -> activityType = "TASK_ACCEPTED";
            case IN_PROGRESS -> activityType = "TASK_STARTED";
            case COMPLETED -> activityType = "TASK_COMPLETED";
            default -> activityType = "TASK_UPDATED";
        }
        String desc = activityType.replace('_', ' ')
                + ": " + a.getTaskName();
        logActivity(a.getVolunteerId(), activityType, desc);

        if (newStatus == VolunteerTaskStatus.COMPLETED) {
            int remaining = assignmentDAO.countActive(a.getVolunteerId());
            if (remaining == 0) {
                Volunteer v = getVolunteer(a.getVolunteerId());
                if (v.getAvailability() == VolunteerAvailability.BUSY) {
                    v.setAvailability(VolunteerAvailability.AVAILABLE);
                    volunteerDAO.save(v);
                    logActivity(v.getId(), "AVAILABILITY_CHANGED",
                            "Availability changed to "
                                    + v.getAvailability().getLabel());
                }
            }
        }
        return saved;
    }

    private boolean isForwardTransition(VolunteerTaskStatus from,
            VolunteerTaskStatus to) {
        if (from == to) {
            return true;
        }
        return switch (from) {
            case ASSIGNED -> to == VolunteerTaskStatus.ACCEPTED
                    || to == VolunteerTaskStatus.IN_PROGRESS
                    || to == VolunteerTaskStatus.COMPLETED;
            case ACCEPTED -> to == VolunteerTaskStatus.IN_PROGRESS
                    || to == VolunteerTaskStatus.COMPLETED;
            case IN_PROGRESS -> to == VolunteerTaskStatus.COMPLETED;
            case COMPLETED -> false;
        };
    }

    // ── workload / tasks / activity ──────────────────────────────────

    public List<VolunteerAssignment> getTasks(long volunteerId)
            throws DataAccessException {
        return assignmentDAO.findByVolunteer(volunteerId);
    }

    public List<VolunteerAssignment> getMyTasks()
            throws UnauthorizedOperationException, DataAccessException {
        Volunteer current = session.getCurrentVolunteer();
        if (current == null) {
            throw new UnauthorizedOperationException(
                    "No volunteer profile linked to this account");
        }
        return assignmentDAO.findByVolunteer(current.getId());
    }

    public int getWorkload(long volunteerId) throws DataAccessException {
        return assignmentDAO.countActive(volunteerId);
    }

    public List<VolunteerActivity> getActivity(long volunteerId)
            throws DataAccessException {
        return activityDAO.findByVolunteer(volunteerId);
    }

    // ── statistics ───────────────────────────────────────────────────

    public int countByAvailability(VolunteerAvailability a)
            throws DataAccessException {
        return volunteerDAO.findByAvailability(a).size();
    }

    public int countCompletedTasks() throws DataAccessException {
        return assignmentDAO.findByStatus(VolunteerTaskStatus.COMPLETED)
                .size();
    }

    public int countActiveAssignments() throws DataAccessException {
        int total = 0;
        for (Volunteer v : volunteerDAO.findAll()) {
            total += assignmentDAO.countActive(v.getId());
        }
        return total;
    }

    // ── helpers ──────────────────────────────────────────────────────

    private void logActivity(long volunteerId, String type,
                             String description) throws DataAccessException {
        VolunteerActivity activity = new VolunteerActivity();
        activity.setVolunteerId(volunteerId);
        activity.setActivityType(type);
        activity.setDescription(description);
        try {
            activityDAO.save(activity);
        } catch (DataAccessException ignored) {
            // activity logging must not fail the primary operation
        }
    }

    /** Value holder for ranked volunteer candidates. */
    public record VolunteerScore(Volunteer volunteer, int score,
                                 int activeTasks) {
    }
}
