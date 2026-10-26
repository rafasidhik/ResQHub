package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Disaster;
import com.resqhub.model.DisasterSeverity;
import com.resqhub.model.DisasterStatus;
import com.resqhub.model.DisasterType;

/** JDBC data access for the disasters table. */
public class DisasterDAO extends BaseDao implements Repository<Disaster> {

    @Override
    public Disaster save(Disaster disaster) throws DataAccessException {
        if (disaster.getId() == null) {
            return insert(disaster);
        }
        return update(disaster);
    }

    private Disaster insert(Disaster d) throws DataAccessException {
        String sql = "INSERT INTO disasters (title, disaster_type, severity, status, "
                + "location, affected_population, start_date, end_date, description, "
                + "reported_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            bindColumns(ps, d, 1);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("No generated id returned for disaster");
                }
                long newId = keys.getLong(1);
                return findById(newId);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save disaster: " + d.getTitle(), e);
        }
    }

    private Disaster update(Disaster d) throws DataAccessException {
        String sql = "UPDATE disasters SET title = ?, disaster_type = ?, severity = ?, "
                + "status = ?, location = ?, affected_population = ?, start_date = ?, "
                + "end_date = ?, description = ?, reported_by = ? WHERE id = ?";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            int idx = bindColumns(ps, d, 1);
            ps.setLong(idx, d.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Disaster update affected " + rows + " rows for id " + d.getId());
            }
            return findById(d.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Could not update disaster " + d.getId(), e);
        }
    }

    /** Shared column binding for INSERT and UPDATE (method overloading by position). */
    private int bindColumns(PreparedStatement ps, Disaster d, int start)
            throws SQLException {
        ps.setString(start, d.getTitle());
        ps.setString(start + 1, enumOrNull(d.getDisasterType()));
        ps.setString(start + 2, enumOrNull(d.getSeverity()));
        ps.setString(start + 3, enumOrNull(d.getStatus()));
        ps.setString(start + 4, d.getLocation());
        ps.setInt(start + 5, d.getAffectedPopulation());
        bindLocalDateTime(ps, start + 6, d.getStartDateTime());
        bindLocalDateTime(ps, start + 7, d.getEndDateTime());
        ps.setString(start + 8, d.getDescription());
        bindNullableLong(ps, start + 9, d.getReportedBy());
        return start + 10;
    }

    @Override
    public Disaster findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM disasters WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load disaster " + id, e);
        }
    }

    @Override
    public List<Disaster> findAll() throws DataAccessException {
        String sql = "SELECT * FROM disasters ORDER BY start_date DESC";
        List<Disaster> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list disasters", e);
        }
    }

    public List<Disaster> findByStatus(DisasterStatus status) throws DataAccessException {
        String sql = "SELECT * FROM disasters WHERE status = ? ORDER BY start_date DESC";
        List<Disaster> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not filter disasters by status", e);
        }
    }

    /** Case-insensitive keyword search across title and location. */
    public List<Disaster> search(String keyword) throws DataAccessException {
        String sql = "SELECT * FROM disasters WHERE LOWER(title) LIKE ? "
                + "OR LOWER(location) LIKE ? ORDER BY start_date DESC";
        String pattern = "%" + keyword.toLowerCase() + "%";
        List<Disaster> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Disaster search failed", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM disasters WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete disaster " + id, e);
        }
    }

    private Disaster mapRow(ResultSet rs) throws SQLException {
        Disaster d = new Disaster();
        d.setId(rs.getLong("id"));
        d.setTitle(rs.getString("title"));
        d.setDisasterType(readEnum(DisasterType.class, rs.getString("disaster_type")));
        d.setSeverity(readEnum(DisasterSeverity.class, rs.getString("severity")));
        d.setStatus(readEnum(DisasterStatus.class, rs.getString("status")));
        d.setLocation(rs.getString("location"));
        d.setAffectedPopulation(rs.getInt("affected_population"));
        d.setStartDateTime(readLocalDateTime(rs, "start_date"));
        d.setEndDateTime(readLocalDateTime(rs, "end_date"));
        d.setDescription(rs.getString("description"));
        d.setReportedBy((Long) getObjectOrNull(rs, "reported_by"));
        d.setCreatedAt(readLocalDateTime(rs, "created_at"));
        d.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return d;
    }

    protected Object getObjectOrNull(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
