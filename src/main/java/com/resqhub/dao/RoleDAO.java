package com.resqhub.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.RoleType;

/**
 * Lookup-table access for roles. Deliberately does not implement
 * Repository<T>: roles are reference data managed by the schema seed.
 */
public class RoleDAO extends BaseDao {

    /** Returns the roles.id for a RoleType, or null if missing. */
    public Long findIdByRoleName(RoleType role) throws DataAccessException {
        String sql = "SELECT id FROM roles WHERE role_name = ?";
        try (PreparedStatement ps = openConnection().prepareStatement(sql)) {
            ps.setString(1, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Role lookup failed", e);
        }
    }
}
