package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

import com.resqhub.config.DatabaseConnectionManager;
import com.resqhub.exception.DataAccessException;
import com.resqhub.exception.ResQHubException;

/**
 * Abstract superclass for all DAOs. Centralises connection opening,
 * nullable-column binding and enum conversion so concrete DAOs only
 * contain their own SQL and row mapping (template-style reuse).
 */
public abstract class BaseDao {

    protected Connection openConnection() throws DataAccessException {
        try {
            return DatabaseConnectionManager.getInstance().getConnection();
        } catch (ResQHubException e) {
            throw new DataAccessException("Database configuration problem", e);
        } catch (SQLException e) {
            throw new DataAccessException("Could not open database connection", e);
        }
    }

    protected void bindNullableLong(PreparedStatement ps, int index, Long value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }

    protected void bindLocalDateTime(PreparedStatement ps, int index, LocalDateTime value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setObject(index, value);
        }
    }

    protected LocalDateTime readLocalDateTime(ResultSet rs, String column)
            throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    /** Converts a DB enum string to its Java enum; unknown values become null. */
    protected <E extends Enum<E>> E readEnum(Class<E> enumType, String dbValue) {
        if (dbValue == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, dbValue);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    protected String enumOrNull(Enum<?> value) {
        return value == null ? null : value.name();
    }

    protected void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignored) {
                // closing an already-broken resource must not mask the real error
            }
        }
    }
}
