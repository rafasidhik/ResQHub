package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.AccountStatus;
import com.resqhub.model.RoleType;
import com.resqhub.model.User;

/**
 * JDBC data access for the users table (JOINs roles for role_name).
 */
public class UserDAO extends BaseDao implements Repository<User> {

    private static final String SELECT_COLUMNS =
            "SELECT u.id, u.username, u.password_hash, u.full_name, u.email, "
            + "u.phone, r.role_name, u.account_status, u.failed_login_attempts, "
            + "u.last_login, u.created_at, u.updated_at "
            + "FROM users u JOIN roles r ON r.id = u.role_id ";

    @Override
    public User save(User user) throws DataAccessException {
        if (user.getId() == null) {
            return insert(user);
        }
        return update(user);
    }

    private User insert(User user) throws DataAccessException {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, "
                + "phone, role_id, account_status) VALUES (?, ?, ?, ?, ?, "
                + "(SELECT id FROM roles WHERE role_name = ?), ?)";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole() == null ? null : user.getRole().name());
            ps.setString(7, user.getAccountStatus() == null
                    ? AccountStatus.ACTIVE.name()
                    : user.getAccountStatus().name());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException("User insert affected " + rows + " rows");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("No generated id returned for user");
                }
                long newId = keys.getLong(1);
                return findById(newId);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save user: " + user.getUsername(), e);
        }
    }

    private User update(User user) throws DataAccessException {
        String sql = "UPDATE users SET username = ?, password_hash = ?, full_name = ?, "
                + "email = ?, phone = ?, role_id = (SELECT id FROM roles WHERE role_name = ?), "
                + "account_status = ?, failed_login_attempts = ?, last_login = ? "
                + "WHERE id = ?";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole() == null ? null : user.getRole().name());
            ps.setString(7, user.getAccountStatus() == null
                    ? AccountStatus.ACTIVE.name()
                    : user.getAccountStatus().name());
            ps.setInt(8, user.getFailedLoginAttempts());
            bindLocalDateTime(ps, 9, user.getLastLogin());
            ps.setLong(10, user.getId());

            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException("User update affected " + rows + " rows");
            }
            return findById(user.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Could not update user " + user.getId(), e);
        }
    }

    @Override
    public User findById(long id) throws DataAccessException {
        String sql = SELECT_COLUMNS + "WHERE u.id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load user " + id, e);
        }
    }

    public User findByUsername(String username) throws DataAccessException {
        String sql = SELECT_COLUMNS + "WHERE u.username = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load user by username", e);
        }
    }

    @Override
    public List<User> findAll() throws DataAccessException {
        String sql = SELECT_COLUMNS + "ORDER BY u.username";
        List<User> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list users", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete user " + id, e);
        }
    }

    /** Records a successful login: resets failure counter, stamps last_login. */
    public void recordSuccessfulLogin(long userId) throws DataAccessException {
        String sql = "UPDATE users SET failed_login_attempts = 0, "
                + "last_login = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not record login for " + userId, e);
        }
    }

    /** Bumps the failure counter; returns the new value. */
    public int recordFailedLogin(long userId) throws DataAccessException {
        String sql = "UPDATE users SET failed_login_attempts = failed_login_attempts + 1 "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.executeUpdate();

            try (Statement readBack = con.createStatement();
                 ResultSet rs = readBack.executeQuery(
                         "SELECT failed_login_attempts FROM users WHERE id = " + userId)) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not record failed login for " + userId, e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User(rs.getString("full_name"), rs.getString("phone"));
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setRole(readEnum(RoleType.class, rs.getString("role_name")));
        user.setAccountStatus(readEnum(AccountStatus.class,
                rs.getString("account_status")));
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        user.setLastLogin(readLocalDateTime(rs, "last_login"));
        user.setCreatedAt(readLocalDateTime(rs, "created_at"));
        user.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return user;
    }
}
