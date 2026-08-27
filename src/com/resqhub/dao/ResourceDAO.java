package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.Resource;
import com.resqhub.model.ResourceCategory;
import com.resqhub.model.ResourceStatus;

/** JDBC data access for the resources (inventory) table. */
public class ResourceDAO extends BaseDao implements Repository<Resource> {

    @Override
    public Resource save(Resource r) throws DataAccessException {
        if (r.getId() == null) {
            return insert(r);
        }
        return update(r);
    }

    private Resource insert(Resource r) throws DataAccessException {
        String sql = "INSERT INTO resources (name, code, category, "
                + "available_quantity, minimum_level, unit, description, "
                + "status, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getName());
            ps.setString(2, r.getCode());
            ps.setString(3, enumOrNull(r.getCategory()));
            ps.setInt(4, r.getAvailableQuantity());
            ps.setInt(5, r.getMinimumLevel());
            ps.setString(6, r.getUnit());
            ps.setString(7, r.getDescription());
            ps.setString(8, enumOrNull(r.status()));
            bindNullableLong(ps, 9, r.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for resource");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate resource: code '" + r.getCode()
                                + "' already exists", e);
            }
            throw new DataAccessException("Could not save resource: "
                    + r.getName(), e);
        }
    }

    private Resource update(Resource r) throws DataAccessException {
        String sql = "UPDATE resources SET name = ?, code = ?, category = ?, "
                + "available_quantity = ?, minimum_level = ?, unit = ?, "
                + "description = ?, status = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getName());
            ps.setString(2, r.getCode());
            ps.setString(3, enumOrNull(r.getCategory()));
            ps.setInt(4, r.getAvailableQuantity());
            ps.setInt(5, r.getMinimumLevel());
            ps.setString(6, r.getUnit());
            ps.setString(7, r.getDescription());
            ps.setString(8, enumOrNull(r.status()));
            ps.setLong(9, r.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Resource update affected " + rows + " rows for id "
                                + r.getId());
            }
            return findById(r.getId());
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate resource: code '" + r.getCode()
                                + "' is already in use", e);
            }
            throw new DataAccessException(
                    "Could not update resource " + r.getId(), e);
        }
    }

    /** Persists the derived status for a given resource. */
    public void persistStatus(Resource r) throws DataAccessException {
        String sql = "UPDATE resources SET status = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, enumOrNull(r.status()));
            ps.setLong(2, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not persist status for resource " + r.getId(), e);
        }
    }

    @Override
    public Resource findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM resources WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load resource " + id, e);
        }
    }

    @Override
    public List<Resource> findAll() throws DataAccessException {
        String sql = "SELECT * FROM resources ORDER BY category, name";
        return query(sql, List.of());
    }

    public List<Resource> findByCategory(ResourceCategory category)
            throws DataAccessException {
        String sql = "SELECT * FROM resources WHERE category = ? "
                + "ORDER BY name";
        return query(sql, List.of(category.name()));
    }

    /** Filters on the stored availability status column. */
    public List<Resource> findByStatus(ResourceStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM resources WHERE status = ? ORDER BY name";
        return query(sql, List.of(status.name()));
    }

    /** Both low-stock and out-of-stock resources need attention. */
    public List<Resource> findStockShortages() throws DataAccessException {
        String sql = "SELECT * FROM resources "
                + "WHERE status IN ('LOW_STOCK','OUT_OF_STOCK') ORDER BY name";
        return query(sql, List.of());
    }

    /** Resources whose current quantity is below their minimum level
     *  (computed in SQL from the raw values). */
    public List<Resource> findBelowMinimum() throws DataAccessException {
        String sql = "SELECT * FROM resources "
                + "WHERE available_quantity < minimum_level ORDER BY name";
        return query(sql, List.of());
    }

    /** Case-insensitive keyword search across name, code and description. */
    public List<Resource> search(String keyword) throws DataAccessException {
        String sql = "SELECT * FROM resources WHERE LOWER(name) LIKE ? "
                + "OR LOWER(code) LIKE ? OR LOWER(description) LIKE ? "
                + "ORDER BY category, name";
        String pattern = "%" + keyword.toLowerCase() + "%";
        return query(sql, List.of(pattern, pattern, pattern));
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM resources WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete resource " + id, e);
        }
    }

    private List<Resource> query(String sql, List<Object> params)
            throws DataAccessException {
        List<Resource> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) {
                    ps.setLong(i + 1, (Long) p);
                } else {
                    ps.setString(i + 1, String.valueOf(p));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Resource query failed", e);
        }
    }

    private boolean isDupe(SQLException e) {
        return e.getErrorCode() == 1062;
    }

    private Resource mapRow(ResultSet rs) throws SQLException {
        Resource r = new Resource();
        r.setId(rs.getLong("id"));
        r.setName(rs.getString("name"));
        r.setCode(rs.getString("code"));
        r.setCategory(readEnum(ResourceCategory.class,
                rs.getString("category")));
        r.setAvailableQuantity(rs.getInt("available_quantity"));
        r.setMinimumLevel(rs.getInt("minimum_level"));
        r.setUnit(rs.getString("unit"));
        r.setDescription(rs.getString("description"));
        r.setCreatedBy(getObjectOrNull(rs, "created_by"));
        r.setCreatedAt(readLocalDateTime(rs, "created_at"));
        r.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return r;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
