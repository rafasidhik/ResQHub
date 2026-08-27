package com.resqhub.controller;

import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;
import com.resqhub.model.EmergencyRole;
import com.resqhub.model.Volunteer;
import com.resqhub.model.VolunteerActivity;
import com.resqhub.model.VolunteerAssignment;
import com.resqhub.model.VolunteerAvailability;
import com.resqhub.model.VolunteerSkill;
import com.resqhub.model.VolunteerTaskStatus;
import com.resqhub.service.VolunteerService;
import com.resqhub.util.InputParser;

/** Volunteer management screen controller. */
public class VolunteerController {

    private final VolunteerService volunteerService =
            new VolunteerService();

    // ── registration ─────────────────────────────────────────────────

    public ActionResult registerVolunteer(String fullName,
            String contactNumber, String email, String location,
            String skills, VolunteerAvailability availability,
            EmergencyRole role, String maxWorkloadText) {
        try {
            int maxWorkload = InputParser.parseInt(maxWorkloadText,
                    "Max workload");
            Volunteer v = volunteerService.registerVolunteer(
                    fullName, contactNumber, email, location, skills,
                    availability, role, maxWorkload);
            return ActionResult.successWithData(
                    "Volunteer registered as #" + v.getId()
                            + " (" + v.getAvailability().getLabel() + ")",
                    v);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateVolunteer(long id, String fullName,
            String contactNumber, String email, String location,
            String skills, VolunteerAvailability availability,
            EmergencyRole role, String maxWorkloadText) {
        try {
            Volunteer existing = volunteerService.getVolunteer(id);
            int maxWorkload = InputParser.parseInt(maxWorkloadText,
                    "Max workload");
            existing.setFullName(fullName);
            existing.setContactNumber(contactNumber);
            existing.setEmail(email);
            existing.setLocation(location);
            existing.setSkills(skills);
            existing.setAvailability(availability);
            existing.setEmergencyRole(role);
            existing.setMaxWorkload(maxWorkload);
            Volunteer saved = volunteerService.updateVolunteer(existing);
            return ActionResult.success(
                    "Volunteer #" + saved.getId() + " updated");
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteVolunteer(long id) {
        try {
            volunteerService.deleteVolunteer(id);
            return ActionResult.success("Volunteer deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult setAvailability(long id,
            VolunteerAvailability availability) {
        try {
            Volunteer v = volunteerService.setAvailability(id,
                    availability);
            return ActionResult.success(v.getFullName()
                    + " is now " + v.getAvailability().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    // ── skills ───────────────────────────────────────────────────────

    public ActionResult addSkill(long volunteerId, String skillName) {
        try {
            VolunteerSkill s = volunteerService.addSkill(volunteerId,
                    skillName);
            return ActionResult.successWithData(
                    s.getSkillName() + " added", s);
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult deleteSkill(long skillId) {
        try {
            volunteerService.deleteSkill(skillId);
            return ActionResult.success("Skill deleted");
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public List<VolunteerSkill> getSkills(long volunteerId)
            throws DataAccessException {
        return volunteerService.getSkills(volunteerId);
    }

    // ── assignments ──────────────────────────────────────────────────

    public ActionResult assignTask(long volunteerId, String taskName,
            String description, String location, String priorityText) {
        try {
            int priority = InputParser.parseInt(priorityText,
                    "Priority");
            VolunteerAssignment a = volunteerService.assignTask(
                    volunteerId, taskName, description, location,
                    priority);
            return ActionResult.successWithData(
                    "Task assigned: " + a.getTaskName(), a);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult smartAssign(String taskName, String description,
            String taskLocation, String taskSkills,
            String priorityText) {
        try {
            int priority = InputParser.parseInt(priorityText,
                    "Priority");
            VolunteerAssignment a = volunteerService.smartAssign(
                    taskName, description, taskLocation, taskSkills,
                    priority);
            return ActionResult.successWithData(
                    "Best-fit volunteer assigned to "
                            + a.getTaskName(), a);
        } catch (NumberFormatException e) {
            return ActionResult.failure(e.getMessage());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public ActionResult updateTaskStatus(long assignmentId,
            VolunteerTaskStatus status) {
        try {
            VolunteerAssignment a = volunteerService.updateTaskStatus(
                    assignmentId, status);
            return ActionResult.success(
                    a.getTaskName() + " is now "
                            + a.getStatus().getLabel());
        } catch (ResQHubException e) {
            return ActionResult.failure(e.getMessage());
        } catch (Exception e) {
            return ActionResult.failure(
                    "Unexpected error: " + e.getMessage());
        }
    }

    public List<VolunteerAssignment> getTasks(long volunteerId)
            throws DataAccessException {
        return volunteerService.getTasks(volunteerId);
    }

    public List<VolunteerAssignment> getMyTasks()
            throws ResQHubException, DataAccessException {
        return volunteerService.getMyTasks();
    }

    public int getWorkload(long volunteerId) throws DataAccessException {
        return volunteerService.getWorkload(volunteerId);
    }

    public List<VolunteerService.VolunteerScore> findSuitableVolunteers(
            String taskSkills, String taskLocation, int taskPriority,
            int count) throws DataAccessException {
        return volunteerService.findSuitableVolunteers(taskSkills,
                taskLocation, taskPriority, count);
    }

    // ── activity ─────────────────────────────────────────────────────

    public List<VolunteerActivity> getActivity(long volunteerId)
            throws DataAccessException {
        return volunteerService.getActivity(volunteerId);
    }

    // ── queries ──────────────────────────────────────────────────────

    public List<Volunteer> getAllVolunteers() throws DataAccessException {
        return volunteerService.getAllVolunteers();
    }

    public int getCompletedTaskCount() throws DataAccessException {
        return volunteerService.countCompletedTasks();
    }

    public List<Volunteer> searchVolunteers(String keyword)
            throws DataAccessException {
        return volunteerService.searchVolunteers(keyword);
    }

    public Volunteer getVolunteer(long id) throws ResQHubException,
            DataAccessException {
        return volunteerService.getVolunteer(id);
    }

    // ── display helpers ──────────────────────────────────────────────

    public static Object[] toRow(Volunteer v) {
        return new Object[]{
                v.getId(),
                v.getFullName(),
                v.getContactNumber(),
                v.getLocation() == null ? "-" : v.getLocation(),
                v.getEmergencyRole() == null
                        ? "-" : v.getEmergencyRole().getLabel(),
                v.getAvailability().getLabel()
        };
    }

    public static String[] tableHeaders() {
        return new String[]{"ID", "Name", "Contact", "Location",
                "Role", "Availability"};
    }

    public static Object[] toSkillRow(VolunteerSkill s) {
        return new Object[]{s.getId(), s.getSkillName()};
    }

    public static String[] skillTableHeaders() {
        return new String[]{"ID", "Skill"};
    }

    public static Object[] toTaskRow(VolunteerAssignment a) {
        return new Object[]{
                a.getId(),
                a.getTaskName(),
                a.getLocation() == null ? "-" : a.getLocation(),
                a.getPriority(),
                a.getStatus().getLabel(),
                a.getAssignedAt() == null ? "-" : a.getAssignedAt()
        };
    }

    public static String[] taskTableHeaders() {
        return new String[]{"ID", "Task", "Location", "Priority",
                "Status", "Assigned"};
    }

    public static Object[] toActivityRow(VolunteerActivity a) {
        return new Object[]{
                a.getActivityType(),
                a.getDescription(),
                a.getActivityTime() == null ? "-" : a.getActivityTime()
        };
    }

    public static String[] activityTableHeaders() {
        return new String[]{"Activity", "Description", "Time"};
    }
}
