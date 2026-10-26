package com.resqhub.spring.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.VolunteerController;
import com.resqhub.dao.UserDAO;
import com.resqhub.model.EmergencyRole;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;
import com.resqhub.model.Volunteer;
import com.resqhub.model.VolunteerActivity;
import com.resqhub.model.VolunteerAssignment;
import com.resqhub.model.VolunteerAvailability;
import com.resqhub.model.VolunteerSkill;
import com.resqhub.model.VolunteerTaskStatus;
import com.resqhub.service.SessionManager;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class VolunteerWebController extends BaseWebController {

    private final VolunteerController volunteerController = new VolunteerController();
    private final UserDAO userDao = new UserDAO();

    @GetMapping("/app/volunteers")
    public String listOrDetail(@RequestParam(value = "id", required = false) Long id, Model model) throws Exception {
        model.addAttribute("availabilities", WebUtil.options(VolunteerAvailability.class));
        model.addAttribute("emergencyRoles", WebUtil.options(EmergencyRole.class));
        model.addAttribute("taskStatuses", WebUtil.options(VolunteerTaskStatus.class));

        if (id == null) {
            List<Volunteer> list = volunteerController.getAllVolunteers();
            model.addAttribute("items", list);
            model.addAttribute("headers", VolunteerController.tableHeaders());
            model.addAttribute("rows", rows(list, VolunteerController::toRow));
            model.addAttribute("pageTitle", "Volunteers");
            return "volunteers";
        }

        Volunteer v = volunteerController.getVolunteer(id);
        if (v == null) {
            model.addAttribute("errorTitle", "Volunteer not found");
            model.addAttribute("errorMessage", "Volunteer #" + id + " does not exist.");
            return "error";
        }

        List<VolunteerAssignment> tasks = volunteerController.getTasks(id);
        List<VolunteerSkill> skills = volunteerController.getSkills(id);
        List<VolunteerActivity> activity = volunteerController.getActivity(id);
        List<User> volUsers = userDao.findByRole(RoleType.VOLUNTEER);

        model.addAttribute("v", v);
        model.addAttribute("volunteerUsers", opts(volUsers, User::getId, u -> u.getUsername() + " — " + u.getFullName()));
        model.addAttribute("linkedUser", v.getUserId() == null ? null : volUsers.stream()
                .filter(u -> u.getId().equals(v.getUserId())).findFirst().orElse(null));
        model.addAttribute("workload", volunteerController.getWorkload(id));
        model.addAttribute("tasks", tasks);
        model.addAttribute("taskRows", rows(tasks, VolunteerController::toTaskRow));
        model.addAttribute("taskHeaders", VolunteerController.taskTableHeaders());
        model.addAttribute("skills", skills);
        model.addAttribute("skillRows", rows(skills, VolunteerController::toSkillRow));
        model.addAttribute("skillHeaders", VolunteerController.skillTableHeaders());
        model.addAttribute("activityRows", rows(activity, VolunteerController::toActivityRow));
        model.addAttribute("activityHeaders", VolunteerController.activityTableHeaders());
        model.addAttribute("pageTitle", v.getFullName());
        return "volunteer-detail";
    }

    @PostMapping("/app/volunteers")
    public String handleVolunteerAction(@RequestParam("action") String action,
                                        HttpServletRequest req,
                                        RedirectAttributes ra) throws Exception {
        ActionResult r;
        String idParam = req.getParameter("id");
        String detailRedirect = idParam != null && !idParam.isBlank() ? "/app/volunteers?id=" + idParam : "/app/volunteers";

        switch (action) {
            case "create" -> {
                r = volunteerController.registerVolunteer(
                        WebUtil.str(req, "fullName"),
                        WebUtil.str(req, "contactNumber"),
                        WebUtil.opt(req, "email"),
                        WebUtil.str(req, "location"),
                        WebUtil.opt(req, "skills"),
                        WebUtil.optEnum(req, "availability", VolunteerAvailability.class, "Availability"),
                        WebUtil.optEnum(req, "emergencyRole", EmergencyRole.class, "Emergency role"),
                        WebUtil.str(req, "maxWorkload")
                );
                return handleResult(r, ra, "/app/volunteers");
            }
            case "update" -> r = volunteerController.updateVolunteer(
                    WebUtil.reqLong(req, "id", "Volunteer id"),
                    WebUtil.str(req, "fullName"),
                    WebUtil.str(req, "contactNumber"),
                    WebUtil.opt(req, "email"),
                    WebUtil.str(req, "location"),
                    WebUtil.opt(req, "skills"),
                    WebUtil.optEnum(req, "availability", VolunteerAvailability.class, "Availability"),
                    WebUtil.optEnum(req, "emergencyRole", EmergencyRole.class, "Emergency role"),
                    WebUtil.str(req, "maxWorkload")
            );
            case "delete" -> {
                r = volunteerController.deleteVolunteer(WebUtil.reqLong(req, "id", "Volunteer id"));
                return handleResult(r, ra, "/app/volunteers");
            }
            case "availability" -> r = volunteerController.setAvailability(
                    WebUtil.reqLong(req, "id", "Volunteer id"),
                    WebUtil.reqEnum(req, "availability", VolunteerAvailability.class, "Availability")
            );
            case "addSkill" -> r = volunteerController.addSkill(
                    WebUtil.reqLong(req, "id", "Volunteer id"),
                    WebUtil.str(req, "skillName")
            );
            case "deleteSkill" -> r = volunteerController.deleteSkill(WebUtil.reqLong(req, "skillId", "Skill id"));
            case "assignTask" -> r = volunteerController.assignTask(
                    WebUtil.reqLong(req, "id", "Volunteer id"),
                    WebUtil.str(req, "taskName"),
                    WebUtil.opt(req, "description"),
                    WebUtil.str(req, "location"),
                    WebUtil.str(req, "priority")
            );
            case "link" -> r = volunteerController.linkUserAccount(
                    WebUtil.reqLong(req, "id", "Volunteer id"),
                    WebUtil.optLong(req, "userId", "Account")
            );
            case "smartAssign" -> r = volunteerController.smartAssign(
                    WebUtil.str(req, "taskName"),
                    WebUtil.opt(req, "description"),
                    WebUtil.str(req, "location"),
                    WebUtil.str(req, "taskSkills"),
                    WebUtil.str(req, "priority")
            );
            case "taskStatus" -> r = volunteerController.updateTaskStatus(
                    WebUtil.reqLong(req, "assignmentId", "Assignment"),
                    WebUtil.reqEnum(req, "status", VolunteerTaskStatus.class, "Task status")
            );
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:" + detailRedirect;
            }
        }
        return handleResult(r, ra, detailRedirect);
    }

    @GetMapping("/app/my-tasks")
    public String myTasks(Model model) throws Exception {
        Volunteer me = SessionManager.getInstance().getCurrentVolunteer();
        model.addAttribute("availabilities", WebUtil.options(VolunteerAvailability.class));
        if (me != null) {
            List<VolunteerAssignment> tasks = volunteerController.getMyTasks();
            model.addAttribute("me", me);
            model.addAttribute("tasks", tasks);
            model.addAttribute("taskRows", rows(tasks, VolunteerController::toTaskRow));
            model.addAttribute("taskHeaders", VolunteerController.taskTableHeaders());
            model.addAttribute("workload", volunteerController.getWorkload(me.getId()));
        }
        model.addAttribute("pageTitle", "My Tasks");
        return "my-tasks";
    }

    @PostMapping("/app/my-tasks")
    public String handleMyTasksAction(@RequestParam("action") String action,
                                      HttpServletRequest req,
                                      RedirectAttributes ra) throws Exception {
        ActionResult r;
        switch (action) {
            case "availability" -> {
                Volunteer me = SessionManager.getInstance().getCurrentVolunteer();
                if (me == null) {
                    ra.addFlashAttribute("flashType", "danger");
                    ra.addFlashAttribute("flashMessage", "No volunteer profile is linked to this account");
                    return "redirect:/app/my-tasks";
                }
                r = volunteerController.setAvailability(
                        me.getId(),
                        WebUtil.reqEnum(req, "availability", VolunteerAvailability.class, "Availability")
                );
            }
            case "taskStatus" -> r = volunteerController.updateTaskStatus(
                    WebUtil.reqLong(req, "assignmentId", "Assignment"),
                    WebUtil.reqEnum(req, "status", VolunteerTaskStatus.class, "Task status")
            );
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:/app/my-tasks";
            }
        }
        return handleResult(r, ra, "/app/my-tasks");
    }
}
