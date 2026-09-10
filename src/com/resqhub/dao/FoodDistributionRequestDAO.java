package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.BeneficiaryType;
import com.resqhub.model.FoodDistributionRequest;
import com.resqhub.model.FoodRequestStatus;
import com.resqhub.model.PriorityLevel;

/**
 * JDBC data access for the food_requests table (food distribution
 * request lifecycle: create / view / update / allocate / complete).
 */
public class FoodDistributionRequestDAO extends BaseDao
        implements Repository<FoodDistributionRequest> {

    @Override
    public FoodDistributionRequest save(FoodDistributionRequest r)
            throws DataAccessException {
        if (r.getId() == null) {
            return insert(r);
        }
        return update(r);
    }

    private FoodDistributionRequest insert(FoodDistributionRequest r)
            throws DataAccessException {
        String sql = "INSERT INTO food_requests (request_code, disaster_id, "
                + "location, beneficiary_type, beneficiaries, "
                + "required_quantity, priority, status, description, "
                + "requested_at, created_by, allocated_quantity, "
                + "allocated_resource_id, allocated_at, allocated_by, "
                + "assigned_volunteer_id, assigned_at, completed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getRequestCode());
            bindNullableLong(ps, 2, r.getDisasterId());
            ps.setString(3, r.getLocation());
            ps.setString(4, enumOrNull(r.getBeneficiaryType()));
            ps.setInt(5, r.getBeneficiaries());
            ps.setInt(6, r.getRequiredQuantity());
            ps.setString(7, enumOrNull(r.getPriority()));
            ps.setString(8, enumOrNull(r.getStatus()));
            ps.setString(9, r.getDescription());
            bindLocalDateTime(ps, 10, r.getRequestedAt());
            bindNullableLong(ps, 11, r.getCreatedBy());
            ps.setInt(12, r.getAllocatedQuantity());
            bindNullableLong(ps, 13, r.getAllocatedResourceId());
            bindLocalDateTime(ps, 14, r.getAllocatedAt());
            bindNullableLong(ps, 15, r.getAllocatedBy());
            bindNullableLong(ps, 16, r.getAssignedVolunteerId());
            bindLocalDateTime(ps, 17, r.getAssignedAt());
            bindLocalDateTime(ps, 18, r.getCompletedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for food request");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate food request: code '" + r.getRequestCode()
                                + "' already exists", e);
            }
            throw new DataAccessException(
                    "Could not save food request: " + r.getRequestCode(), e);
        }
    }

    private FoodDistributionRequest update(FoodDistributionRequest r)
            throws DataAccessException {
        String sql = "UPDATE food_requests SET request_code = ?, disaster_id = ?, "
                + "location = ?, beneficiary_type = ?, beneficiaries = ?, "
                + "required_quantity = ?, priority = ?, status = ?, "
                + "description = ?, allocated_quantity = ?, "
                + "allocated_resource_id = ?, allocated_at = ?, allocated_by = ?, "
                + "assigned_volunteer_id = ?, assigned_at = ?, completed_at = ? "
                + "WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getRequestCode());
            bindNullableLong(ps, 2, r.getDisasterId());
            ps.setString(3, r.getLocation());
            ps.setString(4, enumOrNull(r.getBeneficiaryType()));
            ps.setInt(5, r.getBeneficiaries());
            ps.setInt(6, r.getRequiredQuantity());
            ps.setString(7, enumOrNull(r.getPriority()));
            ps.setString(8, enumOrNull(r.getStatus()));
            ps.setString(9, r.getDescription());
            ps.setInt(10, r.getAllocatedQuantity());
            bindNullableLong(ps, 11, r.getAllocatedResourceId());
            bindLocalDateTime(ps, 12, r.getAllocatedAt());
            bindNullableLong(ps, 13, r.getAllocatedBy());
            bindNullableLong(ps, 14, r.getAssignedVolunteerId());
            bindLocalDateTime(ps, 15, r.getAssignedAt());
            bindLocalDateTime(ps, 16, r.getCompletedAt());
            ps.setLong(17, r.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Food request update affected " + rows + " rows for id "
                                + r.getId());
            }
            return findById(r.getId());
        } catch (SQLException e) {
            if (isDupe(e)) {
                throw new DataAccessException(
                        "Duplicate food request: code '" + r.getRequestCode()
                                + "' is already in use", e);
            }
            throw new DataAccessException(
                    "Could not update food request " + r.getId(), e);
        }
    }

    @Override
    public FoodDistributionRequest findById(long id)
            throws DataAccessException {
        String sql = "SELECT * FROM food_requests WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load food request " + id, e);
        }
    }

    @Override
    public List<FoodDistributionRequest> findAll() throws DataAccessException {
        String sql = "SELECT * FROM food_requests "
                + "ORDER BY CASE priority "
                + "WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' THEN 1 "
                + "WHEN 'MEDIUM' THEN 2 ELSE 3 END, requested_at DESC, id DESC";
        return query(sql, List.of());
    }

    public List<FoodDistributionRequest> findByStatus(
            FoodRequestStatus status) throws DataAccessException {
        String sql = "SELECT * FROM food_requests WHERE status = ? "
                + "ORDER BY requested_at DESC";
        return query(sql, List.of(status.name()));
    }

    public List<FoodDistributionRequest> findByPriority(PriorityLevel priority)
            throws DataAccessException {
        String sql = "SELECT * FROM food_requests WHERE priority = ? "
                + "ORDER BY requested_at DESC";
        return query(sql, List.of(priority.name()));
    }

    public List<FoodDistributionRequest> findByDisaster(long disasterId)
            throws DataAccessException {
        String sql = "SELECT * FROM food_requests WHERE disaster_id = ? "
                + "ORDER BY requested_at DESC";
        return query(sql, List.of(disasterId));
    }

    public List<FoodDistributionRequest> findByLocation(String location)
            throws DataAccessException {
        String sql = "SELECT * FROM food_requests WHERE LOWER(location) "
                + "LIKE ? ORDER BY requested_at DESC";
        return query(sql, List.of("%" + location.toLowerCase() + "%"));
    }

    /** Open (not completed/cancelled) requests, highest priority first. */
    public List<FoodDistributionRequest> findOpen() throws DataAccessException {
        String sql = "SELECT * FROM food_requests "
                + "WHERE status NOT IN ('COMPLETED','CANCELLED') "
                + "ORDER BY CASE priority WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' "
                + "THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, requested_at";
        return query(sql, List.of());
    }

    /** Case-insensitive keyword search across code, location, description. */
    public List<FoodDistributionRequest> search(String keyword)
            throws DataAccessException {
        String sql = "SELECT * FROM food_requests WHERE LOWER(request_code) "
                + "LIKE ? OR LOWER(location) LIKE ? OR LOWER(description) "
                + "LIKE ? ORDER BY requested_at DESC";
        String pattern = "%" + keyword.toLowerCase() + "%";
        return query(sql, List.of(pattern, pattern, pattern));
    }

    /**
     * Combined filter by any subset of keyword / disaster / location /
     * status / priority (null = no filter).
     */
    public List<FoodDistributionRequest> filter(String keyword,
            Long disasterId, String location, FoodRequestStatus status,
            PriorityLevel priority) throws DataAccessException {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM food_requests WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (disasterId != null) {
            sql.append(" AND disaster_id = ?");
            params.add(disasterId);
        }
        if (location != null && !location.isEmpty()) {
            sql.append(" AND LOWER(location) LIKE ?");
            params.add("%" + location.toLowerCase() + "%");
        }
        if (status != null) {
            sql.append(" AND status = ?");
            params.add(status.name());
        }
        if (priority != null) {
            sql.append(" AND priority = ?");
            params.add(priority.name());
        }
        if (keyword != null && !keyword.isEmpty()) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            sql.append(" AND (LOWER(request_code) LIKE ? OR "
                    + "LOWER(location) LIKE ? OR LOWER(description) LIKE ?)");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" ORDER BY CASE priority WHEN 'CRITICAL' THEN 0 WHEN 'HIGH' "
                + "THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, requested_at DESC");
        return query(sql.toString(), params);
    }

    /** Count of open requests for a given shelter location (for re-sync). */
    public List<FoodDistributionRequest> findOpenByLocation(String location)
            throws DataAccessException {
        String sql = "SELECT * FROM food_requests WHERE status NOT IN "
                + "('COMPLETED','CANCELLED') AND LOWER(location) = ? "
                + "ORDER BY requested_at ASC";
        return query(sql, List.of(location.toLowerCase().trim()));
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM food_requests WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete food request " + id, e);
        }
    }

    /** Total distributed quantity for a single request. */
    public int sumDistributed(long requestId) throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(quantity),0) FROM food_distributions "
                + "WHERE request_id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not sum distributed for request " + requestId, e);
        }
    }

    private List<FoodDistributionRequest> query(String sql, List<Object> params)
            throws DataAccessException {
        List<FoodDistributionRequest> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                bindParam(ps, i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Food request query failed", e);
        }
    }

    private void bindParam(PreparedStatement ps, int index, Object value)
            throws SQLException {
        if (value instanceof Long) {
            ps.setLong(index, (Long) value);
        } else if (value instanceof Integer) {
            ps.setInt(index, (Integer) value);
        } else {
            ps.setString(index, String.valueOf(value));
        }
    }

    private boolean isDupe(SQLException e) {
        return e.getErrorCode() == 1062;
    }

    private FoodDistributionRequest mapRow(ResultSet rs) throws SQLException {
        FoodDistributionRequest r = new FoodDistributionRequest();
        r.setId(rs.getLong("id"));
        r.setRequestCode(rs.getString("request_code"));
        r.setDisasterId(getObjectOrNull(rs, "disaster_id"));
        r.setLocation(rs.getString("location"));
        r.setBeneficiaryType(readEnum(BeneficiaryType.class,
                rs.getString("beneficiary_type")));
        r.setBeneficiaries(rs.getInt("beneficiaries"));
        r.setRequiredQuantity(rs.getInt("required_quantity"));
        r.setPriority(readEnum(PriorityLevel.class, rs.getString("priority")));
        r.setStatus(readEnum(FoodRequestStatus.class, rs.getString("status")));
        r.setDescription(rs.getString("description"));
        beginHydrate(r, rs);
        return r;
    }

    private void beginHydrate(FoodDistributionRequest r, ResultSet rs)
            throws SQLException {
        r.setRequestedAt(readLocalDateTime(rs, "requested_at"));
        r.setCreatedBy(getObjectOrNull(rs, "created_by"));
        r.setAllocatedQuantity(rs.getInt("allocated_quantity"));
        r.setAllocatedResourceId(getObjectOrNull(rs, "allocated_resource_id"));
        r.setAllocatedAt(readLocalDateTime(rs, "allocated_at"));
        r.setAllocatedBy(getObjectOrNull(rs, "allocated_by"));
        r.setAssignedVolunteerId(getObjectOrNull(rs, "assigned_volunteer_id"));
        r.setAssignedAt(readLocalDateTime(rs, "assigned_at"));
        r.setCompletedAt(readLocalDateTime(rs, "completed_at"));
        r.setCreatedAt(readLocalDateTime(rs, "created_at"));
        r.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
