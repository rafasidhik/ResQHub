package com.resqhub.spring.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.RescueTeamController;
import com.resqhub.model.AvailabilityStatus;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.TeamEquipment;
import com.resqhub.model.TeamMember;
import com.resqhub.model.TeamOperationalStatus;
import com.resqhub.model.TeamSkill;
import com.resqhub.model.TeamType;
import com.resqhub.web.WebUtil;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/app/teams")
public class RescueTeamWebController extends BaseWebController {

    private final RescueTeamController teamController = new RescueTeamController();

    @GetMapping
    public String listOrDetail(@RequestParam(value = "id", required = false) Long id, Model model) throws Exception {
        List<RescueTeam> teams = teamController.getAllTeams();
        model.addAttribute("types", WebUtil.options(TeamType.class));
        model.addAttribute("availabilities", WebUtil.options(AvailabilityStatus.class));
        model.addAttribute("operationals", WebUtil.options(TeamOperationalStatus.class));

        if (id == null) {
            model.addAttribute("items", teams);
            model.addAttribute("headers", RescueTeamController.tableHeaders());
            model.addAttribute("rows", rows(teams, RescueTeamController::toRow));
            model.addAttribute("pageTitle", "Rescue Teams");
            return "teams";
        }

        RescueTeam team = teams.stream().filter(t -> t.getId().equals(id)).findFirst().orElse(null);
        if (team == null) {
            model.addAttribute("errorTitle", "Team not found");
            model.addAttribute("errorMessage", "Rescue team #" + id + " does not exist.");
            return "error";
        }

        List<TeamMember> members = teamController.getMembers(id);
        List<TeamSkill> skills = teamController.getSkills(id);
        List<TeamEquipment> equipment = teamController.getEquipment(id);

        model.addAttribute("team", team);
        model.addAttribute("members", members);
        model.addAttribute("memberRows", rows(members, RescueTeamController::toMemberRow));
        model.addAttribute("memberHeaders", RescueTeamController.memberTableHeaders());
        model.addAttribute("skills", skills);
        model.addAttribute("skillRows", rows(skills, RescueTeamController::toSkillRow));
        model.addAttribute("skillHeaders", RescueTeamController.skillTableHeaders());
        model.addAttribute("equipment", equipment);
        model.addAttribute("equipmentRows", rows(equipment, RescueTeamController::toEquipmentRow));
        model.addAttribute("equipmentHeaders", RescueTeamController.equipmentTableHeaders());
        model.addAttribute("pageTitle", team.getTeamName());
        return "team-detail";
    }

    @PostMapping
    public String handleAction(@RequestParam("action") String action,
                               HttpServletRequest req,
                               RedirectAttributes ra) throws Exception {
        ActionResult r;
        String idParam = req.getParameter("id");
        String detailRedirect = idParam != null && !idParam.isBlank() ? "/app/teams?id=" + idParam : "/app/teams";

        switch (action) {
            case "create" -> {
                r = teamController.registerTeam(
                        WebUtil.str(req, "teamName"),
                        WebUtil.optEnum(req, "teamType", TeamType.class, "Team type"),
                        WebUtil.str(req, "leaderName"),
                        WebUtil.str(req, "contactNumber"),
                        WebUtil.str(req, "memberCount"),
                        WebUtil.opt(req, "skills"),
                        WebUtil.opt(req, "equipment"),
                        WebUtil.str(req, "baseLocation")
                );
                return handleResult(r, ra, "/app/teams");
            }
            case "update" -> r = teamController.updateTeam(
                    WebUtil.reqLong(req, "id", "Team id"),
                    WebUtil.str(req, "teamName"),
                    WebUtil.optEnum(req, "teamType", TeamType.class, "Team type"),
                    WebUtil.str(req, "leaderName"),
                    WebUtil.str(req, "contactNumber"),
                    WebUtil.str(req, "memberCount"),
                    WebUtil.opt(req, "skills"),
                    WebUtil.opt(req, "equipment"),
                    WebUtil.str(req, "baseLocation")
            );
            case "delete" -> {
                r = teamController.deleteTeam(WebUtil.reqLong(req, "id", "Team id"));
                return handleResult(r, ra, "/app/teams");
            }
            case "availability" -> r = teamController.setAvailability(
                    WebUtil.reqLong(req, "id", "Team id"),
                    WebUtil.reqEnum(req, "availability", AvailabilityStatus.class, "Availability")
            );
            case "operational" -> r = teamController.setOperationalStatus(
                    WebUtil.reqLong(req, "id", "Team id"),
                    WebUtil.reqEnum(req, "operational", TeamOperationalStatus.class, "Operational status")
            );
            case "addMember" -> r = teamController.addMember(
                    WebUtil.reqLong(req, "id", "Team id"),
                    WebUtil.str(req, "memberName"),
                    WebUtil.str(req, "role"),
                    WebUtil.opt(req, "contactNumber"),
                    WebUtil.opt(req, "specialSkills")
            );
            case "updateMember" -> {
                long teamId = WebUtil.reqLong(req, "id", "Team id");
                long memberId = WebUtil.reqLong(req, "memberId", "Member id");
                TeamMember m = teamController.getMembers(teamId).stream()
                        .filter(x -> x.getId() == memberId).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Member not found in this team"));
                m.setMemberName(WebUtil.str(req, "memberName"));
                m.setRole(WebUtil.str(req, "role"));
                m.setContactNumber(WebUtil.opt(req, "contactNumber"));
                m.setSpecialSkills(WebUtil.opt(req, "specialSkills"));
                m.setAvailability(WebUtil.reqEnum(req, "availability", AvailabilityStatus.class, "Availability"));
                r = teamController.updateMember(m);
            }
            case "deleteMember" -> r = teamController.deleteMember(WebUtil.reqLong(req, "memberId", "Member id"));
            case "addSkill" -> r = teamController.addSkill(
                    WebUtil.reqLong(req, "id", "Team id"),
                    WebUtil.str(req, "skillName"),
                    WebUtil.opt(req, "description")
            );
            case "deleteSkill" -> r = teamController.deleteSkill(WebUtil.reqLong(req, "skillId", "Skill id"));
            case "addEquipment" -> r = teamController.addEquipment(
                    WebUtil.reqLong(req, "id", "Team id"),
                    WebUtil.str(req, "equipmentName"),
                    WebUtil.str(req, "quantity"),
                    WebUtil.opt(req, "description")
            );
            case "deleteEquipment" -> r = teamController.deleteEquipment(WebUtil.reqLong(req, "equipmentId", "Equipment id"));
            default -> {
                ra.addFlashAttribute("flashType", "danger");
                ra.addFlashAttribute("flashMessage", "Unknown action: " + action);
                return "redirect:" + detailRedirect;
            }
        }
        return handleResult(r, ra, detailRedirect);
    }
}
