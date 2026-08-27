package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.TeamEquipment;

/** JDBC data access for the team_equipment table. */
public class TeamEquipmentDAO extends BaseDao
        implements Repository<TeamEquipment> {

    @Override
    public TeamEquipment save(TeamEquipment e) throws DataAccessException {
        if (e.getId() == null) {
            return insert(e);
        }
        return update(e);
    }

    private TeamEquipment insert(TeamEquipment e) throws DataAccessException {
        String sql = "INSERT INTO team_equipment "
                + "(team_id, equipment_name, quantity, description) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, e.getTeamId());
            ps.setString(2, e.getEquipmentName());
            ps.setInt(3, e.getQuantity());
            ps.setString(4, e.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1));
                }
            }
            throw new DataAccessException("No generated equipment id");
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not save team equipment", ex);
        }
    }

    private TeamEquipment update(TeamEquipment e) throws DataAccessException {
        String sql = "UPDATE team_equipment "
                + "SET equipment_name = ?, quantity = ?, description = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, e.getEquipmentName());
            ps.setInt(2, e.getQuantity());
            ps.setString(3, e.getDescription());
            ps.setLong(4, e.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Equipment update affected " + rows + " rows");
            }
            return findById(e.getId());
        } catch (SQLException ex) {
            throw new DataAccessException(
                    "Could not update team equipment", ex);
        }
    }

    @Override
    public TeamEquipment findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM team_equipment WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load equipment " + id, e);
        }
    }

    public List<TeamEquipment> findByTeam(long teamId)
            throws DataAccessException {
        String sql = "SELECT * FROM team_equipment WHERE team_id = ? "
                + "ORDER BY equipment_name";
        List<TeamEquipment> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list equipment for team " + teamId, e);
        }
    }

    /** Find teams that have a piece of equipment whose name matches. */
    public List<Long> findTeamIdsByEquipment(String equipmentName)
            throws DataAccessException {
        String sql = "SELECT DISTINCT team_id FROM team_equipment "
                + "WHERE LOWER(equipment_name) LIKE ?";
        List<Long> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + equipmentName.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getLong("team_id"));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not find teams by equipment", e);
        }
    }

    public void deleteByTeam(long teamId) throws DataAccessException {
        String sql = "DELETE FROM team_equipment WHERE team_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, teamId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete equipment for team " + teamId, e);
        }
    }

    @Override
    public List<TeamEquipment> findAll() throws DataAccessException {
        String sql = "SELECT * FROM team_equipment ORDER BY equipment_name";
        List<TeamEquipment> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list equipment", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM team_equipment WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete equipment " + id, e);
        }
    }

    private TeamEquipment mapRow(ResultSet rs) throws SQLException {
        TeamEquipment e = new TeamEquipment();
        e.setId(rs.getLong("id"));
        e.setTeamId(rs.getLong("team_id"));
        e.setEquipmentName(rs.getString("equipment_name"));
        e.setQuantity(rs.getInt("quantity"));
        e.setDescription(rs.getString("description"));
        e.setCreatedAt(readLocalDateTime(rs, "created_at"));
        e.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return e;
    }
}
