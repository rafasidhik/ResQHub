package com.resqhub.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public HomeController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("pageTitle", "ResQHub — Help. Hope. Humanity.");
        
        // Fetch real-time public stats from MySQL
        Integer activeDisasters = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM disasters WHERE status = 'ACTIVE'", Integer.class);
        Integer registeredShelters = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM shelters WHERE status != 'TEMPORARILY_CLOSED'", Integer.class);
        Integer activeVolunteers = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM volunteers WHERE approval_status = 'APPROVED'", Integer.class);
        Integer completedRescues = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM rescue_requests WHERE status = 'COMPLETED'", Integer.class);

        model.addAttribute("activeDisasters", activeDisasters != null ? activeDisasters : 0);
        model.addAttribute("registeredShelters", registeredShelters != null ? registeredShelters : 0);
        model.addAttribute("activeVolunteers", activeVolunteers != null ? activeVolunteers : 0);
        model.addAttribute("completedRescues", completedRescues != null ? completedRescues : 0);

        return "landing";
    }
}
