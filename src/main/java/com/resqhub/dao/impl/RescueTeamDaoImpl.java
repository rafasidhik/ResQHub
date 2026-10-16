package com.resqhub.dao.impl;

import com.resqhub.dao.RescueTeamDao;
import com.resqhub.model.RescueTeam;
import com.resqhub.model.RescueTeamMember;
import com.resqhub.model.TeamAvailability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class RescueTeamDaoImpl implements RescueTeamDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RescueTeam> teamRowMapper = (rs, rowNum) -> {
        RescueTeam t = new RescueTeam();
        t.setId(rs.getLong("id"));
        t.setTeamIdentifier(rs.getString("team_identifier"));
        t.setName(rs.getString("name"));
        t.setTeamLeader(rs.getString("team_leader"));
        t.setContactNumber(rs.getString("contact_number"));
        t.setSkills(rs.getString("skills"));
        t.setOperatingArea(rs.getString("operating_area"));
        t.setCapacity(rs.getInt("capacity"));
        t.setAvailabilityStatus(TeamAvailability.valueOf(rs.getString("availability_status")));
        t.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) t.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) t.setUpdatedAt(updatedAt.toLocalDateTime());

        try { t.setMemberCount(rs.getInt("member_count")); } catch (Exception ignored) {}
        try {
            long aid = rs.getLong("active_assignment_id");
            if (!rs.wasNull()) t.setActiveAssignmentId(aid);
        } catch (Exception ignored) {}
        try { t.setActiveMissionLocation(rs.getString("active_mission_location")); } catch (Exception ignored) {}

        return t;
    };

    private final RowMapper<RescueTeamMember> memberRowMapper = (rs, rowNum) -> {
        RescueTeamMember m = new RescueTeamMember();
        m.setId(rs.getLong("id"));
        m.setTeamId(rs.getLong("team_id"));
        m.setMemberName(rs.getString("member_name"));
        m.setRoleTitle(rs.getString("role_title"));
        m.setContactNumber(rs.getString("contact_number"));
        m.setSpecialty(rs.getString("specialty"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) m.setCreatedAt(createdAt.toLocalDateTime());
        return m;
    };

    @Autowired
    public RescueTeamDaoImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private String getBaseSelect() {
        return "SELECT t.*, " +
               "(SELECT COUNT(*) FROM rescue_team_members m WHERE m.team_id = t.id) AS member_count, " +
               "a.id AS active_assignment_id, req.location AS active_mission_location " +
               "FROM rescue_teams t " +
               "LEFT JOIN rescue_assignments a ON (a.team_id = t.id AND a.status IN ('ASSIGNED', 'IN_PROGRESS')) " +
               "LEFT JOIN rescue_requests req ON a.request_id = req.id ";
    }

    @Override
    public Optional<RescueTeam> findById(Long id) {
        String sql = getBaseSelect() + "WHERE t.id = ?";
        try {
            RescueTeam team = jdbcTemplate.queryForObject(sql, teamRowMapper, id);
            return Optional.ofNullable(team);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<RescueTeam> findByIdentifier(String identifier) {
        String sql = getBaseSelect() + "WHERE t.team_identifier = ?";
        try {
            RescueTeam team = jdbcTemplate.queryForObject(sql, teamRowMapper, identifier);
            return Optional.ofNullable(team);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Long save(RescueTeam team) {
        String sql = "INSERT INTO rescue_teams (team_identifier, name, team_leader, contact_number, skills, " +
                     "operating_area, capacity, availability_status, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, team.getTeamIdentifier());
            ps.setString(2, team.getName());
            ps.setString(3, team.getTeamLeader());
            ps.setString(4, team.getContactNumber());
            ps.setString(5, team.getSkills());
            ps.setString(6, team.getOperatingArea());
            ps.setInt(7, team.getCapacity());
            ps.setString(8, team.getAvailabilityStatus().name());
            ps.setString(9, team.getStatus());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            return keyHolder.getKey().longValue();
        }
        return null;
    }

    @Override
    public void update(RescueTeam team) {
        String sql = "UPDATE rescue_teams SET name = ?, team_leader = ?, contact_number = ?, skills = ?, " +
                     "operating_area = ?, capacity = ?, availability_status = ?, status = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                team.getName(),
                team.getTeamLeader(),
                team.getContactNumber(),
                team.getSkills(),
                team.getOperatingArea(),
                team.getCapacity(),
                team.getAvailabilityStatus().name(),
                team.getStatus(),
                team.getId());
    }

    @Override
    public void updateAvailability(Long id, TeamAvailability availability) {
        String sql = "UPDATE rescue_teams SET availability_status = ? WHERE id = ?";
        jdbcTemplate.update(sql, availability.name(), id);
    }

    @Override
    public List<RescueTeam> findAll(int page, int size, String search, String availability, String status) {
        StringBuilder sql = new StringBuilder(getBaseSelect() + "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (t.name LIKE ? OR t.team_identifier LIKE ? OR t.team_leader LIKE ? OR t.operating_area LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (availability != null && !availability.trim().isEmpty() && !"ALL".equalsIgnoreCase(availability)) {
            sql.append("AND t.availability_status = ? ");
            params.add(availability.trim());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND t.status = ? ");
            params.add(status.trim());
        }

        sql.append("ORDER BY (t.availability_status = 'AVAILABLE') DESC, t.id ASC LIMIT ? OFFSET ?");
        params.add(size);
        params.add(page * size);

        return jdbcTemplate.query(sql.toString(), teamRowMapper, params.toArray());
    }

    @Override
    public int count(String search, String availability, String status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM rescue_teams t WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.trim().isEmpty()) {
            sql.append("AND (t.name LIKE ? OR t.team_identifier LIKE ? OR t.team_leader LIKE ? OR t.operating_area LIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (availability != null && !availability.trim().isEmpty() && !"ALL".equalsIgnoreCase(availability)) {
            sql.append("AND t.availability_status = ? ");
            params.add(availability.trim());
        }

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            sql.append("AND t.status = ? ");
            params.add(status.trim());
        }

        Integer count = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return count != null ? count : 0;
    }

    @Override
    public List<RescueTeam> findAvailable() {
        String sql = getBaseSelect() + "WHERE t.availability_status = 'AVAILABLE' AND t.status = 'ACTIVE' ORDER BY t.name ASC";
        return jdbcTemplate.query(sql, teamRowMapper);
    }

    @Override
    public List<RescueTeamMember> findMembersByTeamId(Long teamId) {
        String sql = "SELECT * FROM rescue_team_members WHERE team_id = ? ORDER BY id ASC";
        return jdbcTemplate.query(sql, memberRowMapper, teamId);
    }

    @Override
    public void saveMember(RescueTeamMember member) {
        String sql = "INSERT INTO rescue_team_members (team_id, member_name, role_title, contact_number, specialty) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, member.getTeamId(), member.getMemberName(), member.getRoleTitle(), member.getContactNumber(), member.getSpecialty());
    }

    @Override
    public void deleteMember(Long memberId) {
        String sql = "DELETE FROM rescue_team_members WHERE id = ?";
        jdbcTemplate.update(sql, memberId);
    }

    @Override
    public int getNextSequence() {
        String sql = "SELECT COUNT(*) FROM rescue_teams";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return (count != null ? count : 0) + 1;
    }
}
