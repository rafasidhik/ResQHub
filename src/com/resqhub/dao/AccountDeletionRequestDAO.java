package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AccountDeletionRequest;
import com.resqhub.model.DeletionRequestStatus;

/**
 * JDBC data access for the account_deletion_requests table.
 */
public class AccountDeletionRequestDAO extends BaseDao
        implements Repository<AccountDeletionRequest> {

    private static final String SELECT_COLUMNS =
            "SELECT id, user_id, status, requested_at, reviewed_by, "
            + "reviewed_at, admin_notes ";

    @Override
    public AccountDeletionRequest save(AccountDeletionRequest request)
            throws DataAccessException {
        if (request.getId() == null) {
            return insert(request);
        }
        return update(request);
    }

    private AccountDeletionRequest insert(AccountDeletionRequest request)
            throws DataAccessException {
        String sql = "INSERT INTO account_deletion_requests "
                + "(user_id, status) VALUES (?, ?)";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, request.getUserId());
            ps.setString(2, request.getStatus().name());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Deletion request insert affected " + rows + " rows");
            }
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for deletion request");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save deletion request", e);
        }
    }

    private AccountDeletionRequest update(AccountDeletionRequest request)
            throws DataAccessException {
        String sql = "UPDATE account_deletion_requests "
                + "SET status = ?, reviewed_by = ?, reviewed_at = ?, "
                + "admin_notes = ? WHERE id = ?";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, request.getStatus().name());
            bindLongOrNull(ps, 2, request.getReviewedBy());
            bindLocalDateTime(ps, 3, request.getReviewedAt());
            ps.setString(4, request.getAdminNotes());
            ps.setLong(5, request.getId());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Deletion request update affected " + rows + " rows");
            }
            return findById(request.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update deletion request " + request.getId(), e);
        }
    }

    @Override
    public AccountDeletionRequest findById(long id)
            throws DataAccessException {
        String sql = SELECT_COLUMNS
                + "FROM account_deletion_requests WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load deletion request " + id, e);
        }
    }

    @Override
    public List<AccountDeletionRequest> findAll()
            throws DataAccessException {
        String sql = SELECT_COLUMNS
                + "FROM account_deletion_requests ORDER BY requested_at DESC";
        List<AccountDeletionRequest> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list deletion requests", e);
        }
        return result;
    }

    public List<AccountDeletionRequest> findByStatus(
            DeletionRequestStatus status) throws DataAccessException {
        String sql = SELECT_COLUMNS
                + "FROM account_deletion_requests WHERE status = ? "
                + "ORDER BY requested_at DESC";
        List<AccountDeletionRequest> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not list deletion requests by status", e);
        }
        return result;
    }

    public AccountDeletionRequest findPendingByUser(long userId)
            throws DataAccessException {
        String sql = SELECT_COLUMNS
                + "FROM account_deletion_requests "
                + "WHERE user_id = ? AND status = 'PENDING'";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not check pending deletion request", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM account_deletion_requests WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete deletion request " + id, e);
        }
    }

    private AccountDeletionRequest mapRow(ResultSet rs) throws SQLException {
        AccountDeletionRequest r = new AccountDeletionRequest();
        r.setId(rs.getLong("id"));
        r.setUserId(rs.getLong("user_id"));
        r.setStatus(DeletionRequestStatus.valueOf(rs.getString("status")));
        r.setRequestedAt(rs.getTimestamp("requested_at")
                .toLocalDateTime());
        long reviewer = rs.getLong("reviewed_by");
        r.setReviewedBy(rs.wasNull() ? null : reviewer);
        r.setReviewedAt(rs.getTimestamp("reviewed_at") == null
                ? null : rs.getTimestamp("reviewed_at").toLocalDateTime());
        r.setAdminNotes(rs.getString("admin_notes"));
        return r;
    }

    private void bindLongOrNull(PreparedStatement ps, int index, Long value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.BIGINT);
        } else {
            ps.setLong(index, value);
        }
    }
}
