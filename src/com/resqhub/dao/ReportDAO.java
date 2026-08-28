package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.ReportFilters;

/**
 * Analytical / reporting data access. Runs the SQL aggregation queries
 * (COUNT, SUM, AVG, MIN, MAX, GROUP BY, HAVING, JOIN, filtering,
 * sorting) that power the Reports &amp; Analytics module.
 *
 * Every method returns the raw result rows; the service layer labels the
 * columns and computes the human-readable summary lines. Filter columns
 * are appended to the WHERE clause only when supplied, so a filter-less
 * run shows the whole dataset (demonstrating filtering + sorting).
 */
public class ReportDAO extends BaseDao {

    // ── overview: cross-module summary cards ─────────────────────────

    /**
     * One row per key metric for the dashboard overview cards.
     * Each row is { label, value }.
     */
    public List<Object[]> overview() throws DataAccessException {
        String sql = """
            SELECT 'ACTIVE DISASTERS' AS m,
                   (SELECT COUNT(*) FROM disasters WHERE status <> 'RESOLVED') AS v
            UNION ALL SELECT 'TOTAL DISASTERS',
                   (SELECT COUNT(*) FROM disasters)
            UNION ALL SELECT 'TOTAL VICTIMS',
                   (SELECT COUNT(*) FROM victims)
            UNION ALL SELECT 'CRITICAL VICTIMS',
                   (SELECT COUNT(*) FROM victims WHERE emergency_status = 'CRITICAL')
            UNION ALL SELECT 'PENDING RESCUES',
                   (SELECT COUNT(*) FROM rescue_requests WHERE status = 'PENDING')
            UNION ALL SELECT 'CRITICAL REQUESTS',
                   (SELECT COUNT(*) FROM rescue_requests WHERE priority = 'CRITICAL'
                    AND status NOT IN ('RESCUED','CANCELLED'))
            UNION ALL SELECT 'TEAMS AVAILABLE',
                   (SELECT COUNT(*) FROM rescue_teams WHERE availability_status = 'AVAILABLE')
            UNION ALL SELECT 'VOLUNTEERS AVAILABLE',
                   (SELECT COUNT(*) FROM volunteers WHERE availability = 'AVAILABLE')
            UNION ALL SELECT 'TOTAL DONORS', (SELECT COUNT(*) FROM donors)
            UNION ALL SELECT 'CASH DONATED (Rs)',
                   (SELECT COALESCE(SUM(amount),0) FROM donations WHERE donation_type = 'CASH')
            UNION ALL SELECT 'MATERIAL UNITS',
                   (SELECT COALESCE(SUM(quantity),0) FROM donations
                    WHERE donation_type = 'MATERIAL')
            ORDER BY m
            """;
        return run(sql, List.of());
    }

    // ── active disaster report ───────────────────────────────────────

    public List<Object[]> disastersByType() throws DataAccessException {
        String sql = "SELECT disaster_type AS type, COUNT(*) AS count, "
                + "SUM(affected_population) AS affected "
                + "FROM disasters GROUP BY disaster_type ORDER BY count DESC";
        return run(sql, List.of());
    }

    public List<Object[]> disastersByStatus() throws DataAccessException {
        String sql = "SELECT status, COUNT(*) AS count, "
                + "SUM(affected_population) AS affected "
                + "FROM disasters GROUP BY status ORDER BY status";
        return run(sql, List.of());
    }

    public List<Object[]> disastersBySeverity() throws DataAccessException {
        String sql = "SELECT severity, COUNT(*) AS count, "
                + "SUM(affected_population) AS affected "
                + "FROM disasters GROUP BY severity ORDER BY count DESC";
        return run(sql, List.of());
    }

    public List<Object[]> disastersByLocation() throws DataAccessException {
        String sql = "SELECT location, COUNT(*) AS count, "
                + "MAX(affected_population) AS max_affected "
                + "FROM disasters GROUP BY location ORDER BY count DESC";
        return run(sql, List.of());
    }

    /** HAVING demonstration: only locations/rows above a size threshold. */
    public List<Object[]> disastersAbovePopulation(int threshold)
            throws DataAccessException {
        String sql = "SELECT title, location, severity, "
                + "SUM(affected_population) AS affected "
                + "FROM disasters "
                + "GROUP BY title, location, severity "
                + "HAVING SUM(affected_population) > ? "
                + "ORDER BY affected DESC";
        return run(sql, List.of(threshold));
    }

    // ── victim statistics ────────────────────────────────────────────

    public List<Object[]> victimsByDisaster(ReportFilters filters)
            throws DataAccessException {
        String sql = """
            SELECT d.title AS disaster,
                   COUNT(v.id) AS total,
                   SUM(v.emergency_status = 'SAFE') AS safe,
                   SUM(v.emergency_status = 'RESCUE_REQUIRED') AS rescue_required,
                   SUM(v.emergency_status = 'INJURED') AS injured,
                   SUM(v.emergency_status = 'CRITICAL') AS critical,
                   SUM(v.emergency_status = 'MISSING') AS missing
            FROM disasters d
            LEFT JOIN victims v ON v.disaster_id = d.id
            """;
        String group = " GROUP BY d.title ORDER BY total DESC";
        if (filters != null && filters.disasterId() != null) {
            sql += " WHERE d.id = ?";
            return run(sql + group, List.of(filters.disasterId()));
        }
        return run(sql + group, List.of());
    }

    public List<Object[]> victimsByStatus() throws DataAccessException {
        String sql = "SELECT emergency_status, COUNT(*) AS total "
                + "FROM victims GROUP BY emergency_status ORDER BY total DESC";
        return run(sql, List.of());
    }

    public List<Object[]> victimsByGender() throws DataAccessException {
        String sql = "SELECT gender, COUNT(*) AS total, "
                + "AVG(age) AS avg_age, MIN(age) AS min_age, MAX(age) AS max_age "
                + "FROM victims GROUP BY gender ORDER BY total DESC";
        return run(sql, List.of());
    }

    // ── rescue request statistics ────────────────────────────────────

    public List<Object[]> requestsByStatus() throws DataAccessException {
        String sql = "SELECT status, COUNT(*) AS total, SUM(people_count) AS people "
                + "FROM rescue_requests GROUP BY status ORDER BY total DESC";
        return run(sql, List.of());
    }

    public List<Object[]> requestsByPriority() throws DataAccessException {
        String sql = "SELECT priority, COUNT(*) AS total "
                + "FROM rescue_requests WHERE priority IS NOT NULL "
                + "GROUP BY priority ORDER BY total DESC";
        return run(sql, List.of());
    }

    /** JOIN demonstration: rescue requests joined with disasters + victims. */
    public List<Object[]> requestsWithDetails(ReportFilters filters)
            throws DataAccessException {
        String sql = """
            SELECT rr.id, rr.location, d.title AS disaster,
                   rr.priority, rr.status, rr.people_count,
                   COALESCE(v.full_name, '-') AS victim
            FROM rescue_requests rr
            JOIN disasters d ON d.id = rr.disaster_id
            LEFT JOIN victims v ON v.id = rr.victim_id
            """;
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (filters != null && filters.disasterId() != null) {
            where.append(" AND rr.disaster_id = ?");
            params.add(filters.disasterId());
        }
        if (filters != null && filters.status() != null) {
            where.append(" AND rr.status = ?");
            params.add(filters.status().toUpperCase());
        }
        if (filters != null && filters.priority() != null) {
            where.append(" AND rr.priority = ?");
            params.add(filters.priority().toUpperCase());
        }
        if (filters != null && filters.location() != null) {
            where.append(" AND rr.location LIKE ?");
            params.add("%" + filters.location() + "%");
        }
        String order = " ORDER BY "
                + "CASE rr.priority WHEN 'CRITICAL' THEN 4 WHEN 'HIGH' THEN 3 "
                + "WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END DESC, rr.id";
        return run(sql + where + order, params);
    }

    // ── rescue performance report ────────────────────────────────────

    public List<Object[]> assignmentsByTeam(ReportFilters filters)
            throws DataAccessException {
        String sql = """
            SELECT t.team_name,
                   COUNT(a.id) AS total,
                   SUM(a.assignment_status = 'COMPLETED') AS completed,
                   SUM(a.assignment_status NOT IN ('COMPLETED','ABORTED')) AS active
            FROM rescue_teams t
            LEFT JOIN rescue_assignments a ON a.rescue_team_id = t.id
            """;
        String group = " GROUP BY t.team_name ORDER BY total DESC";
        if (filters != null && filters.disasterId() != null) {
            String joinSql = sql.replace(
                    "LEFT JOIN rescue_assignments a ON a.rescue_team_id = t.id",
                    "LEFT JOIN rescue_assignments a ON a.rescue_team_id = t.id "
                            + "LEFT JOIN rescue_requests rr ON rr.id = a.rescue_request_id");
            sql = joinSql + " WHERE rr.disaster_id = ? ";
            return run(sql + group, List.of(filters.disasterId()));
        }
        return run(sql + group, List.of());
    }

    public List<Object[]> assignmentsByStatus()
            throws DataAccessException {
        String sql = "SELECT a.assignment_status, COUNT(*) AS total "
                + "FROM rescue_assignments a "
                + "GROUP BY a.assignment_status ORDER BY total DESC";
        return run(sql, List.of());
    }

    public List<Object[]> operationsByDisaster()
            throws DataAccessException {
        String sql = """
            SELECT d.title AS disaster,
                   COUNT(a.id) AS operations,
                   SUM(a.assignment_status = 'COMPLETED') AS completed
            FROM disasters d
            JOIN rescue_requests rr ON rr.disaster_id = d.id
            JOIN rescue_assignments a ON a.rescue_request_id = rr.id
            GROUP BY d.title ORDER BY operations DESC
            """;
        return run(sql, List.of());
    }

    // ── volunteer report ─────────────────────────────────────────────

    public List<Object[]> volunteersByAvailability()
            throws DataAccessException {
        String sql = "SELECT availability, COUNT(*) AS total "
                + "FROM volunteers GROUP BY availability ORDER BY total DESC";
        return run(sql, List.of());
    }

    public List<Object[]> volunteersByRole()
            throws DataAccessException {
        String sql = "SELECT COALESCE(emergency_role, 'GENERAL') AS role, "
                + "COUNT(*) AS total "
                + "FROM volunteers GROUP BY role ORDER BY total DESC";
        return run(sql, List.of());
    }

    public List<Object[]> volunteersByLocation()
            throws DataAccessException {
        String sql = "SELECT location, COUNT(*) AS total "
                + "FROM volunteers GROUP BY location ORDER BY total DESC";
        return run(sql, List.of());
    }

    public List<Object[]> volunteerTaskLoad()
            throws DataAccessException {
        String sql = """
            SELECT v.full_name AS volunteer,
                   SUM(va.status = 'COMPLETED') AS completed,
                   SUM(va.status <> 'COMPLETED') AS active,
                   SUM(va.status = 'COMPLETED' AND 1=1) AS workload
            FROM volunteers v
            LEFT JOIN volunteer_assignments va ON va.volunteer_id = v.id
            GROUP BY v.full_name ORDER BY active DESC
            """;
        return run(sql, List.of());
    }

    // ── donation statistics ──────────────────────────────────────────

    public List<Object[]> donationsByType()
            throws DataAccessException {
        String sql = "SELECT donation_type, COUNT(*) AS total, "
                + "SUM(quantity) AS units "
                + "FROM donations GROUP BY donation_type ORDER BY total DESC";
        return run(sql, List.of());
    }

    public List<Object[]> donationsByStatus()
            throws DataAccessException {
        String sql = "SELECT status, COUNT(*) AS total "
                + "FROM donations GROUP BY status ORDER BY total DESC";
        return run(sql, List.of());
    }

    /** JOIN demonstration: donations with their donor organisation. */
    public List<Object[]> donationsByDonor(ReportFilters filters)
            throws DataAccessException {
        String sql = """
            SELECT dn.full_name AS donor, dn.donor_type,
                   COUNT(d.id) AS donations,
                   COALESCE(SUM(CASE WHEN d.donation_type = 'CASH'
                         THEN d.amount ELSE 0 END), 0) AS cash
            FROM donors dn
            LEFT JOIN donations d ON d.donor_id = dn.id
            """;
        String group = " GROUP BY dn.full_name, dn.donor_type ORDER BY cash DESC";
        if (filters != null && filters.resourceCategory() != null) {
            sql += " WHERE d.donation_type = ?";
            return run(sql + group, List.of(filters.resourceCategory().toUpperCase()));
        }
        return run(sql + group, List.of());
    }

    public List<Object[]> distributionAggregate()
            throws DataAccessException {
        String sql = "SELECT DISTRIBUTED_TO AS where_used, "
                + "COUNT(*) AS distributions, SUM(quantity) AS units "
                + "FROM donation_distributions "
                + "GROUP BY where_used ORDER BY units DESC";
        return run(sql, List.of());
    }

    // ── module data not yet present in this build ────────────────────

    /** Shelters/camps do not yet have a table in this build; we derive a
     *  count of victims placed IN_SHELTER as a placeholder indicator. */
    public List<Object[]> shelterOccupancy() throws DataAccessException {
        String sql = """
            SELECT name, district, max_capacity, current_occupancy,
                   available_capacity, operational_status,
                   ROUND(current_occupancy * 100.0 / max_capacity) AS utilisation
            FROM shelters ORDER BY district, name
            """;
        return run(sql, List.of());
    }

    /** High-level shelter capacity picture for the Reports summary
     *  (COUNT / SUM / AVG / MIN / MAX over the shelters table). */
    public List<Object[]> shelterCapacitySummary() throws DataAccessException {
        String sql = """
            SELECT 'TOTAL SHELTERS' AS metric, COUNT(*) AS value FROM shelters
            UNION ALL
            SELECT 'TOTAL CAPACITY', COALESCE(SUM(max_capacity),0) FROM shelters
            UNION ALL
            SELECT 'CURRENT OCCUPANCY', COALESCE(SUM(current_occupancy),0)
                FROM shelters
            UNION ALL
            SELECT 'AVAILABLE SPACES', COALESCE(SUM(available_capacity),0)
                FROM shelters
            UNION ALL
            SELECT 'FULL SHELTERS', COUNT(*)
                FROM shelters WHERE available_capacity <= 0
            UNION ALL
            SELECT 'NEAR CAPACITY', COUNT(*)
                FROM shelters WHERE max_capacity > 0
                AND available_capacity <= max_capacity * 0.10
                AND operational_status NOT IN ('FULL','CLOSED')
            ORDER BY metric
            """;
        return run(sql, List.of());
    }

    /** Allocation records grouped by status with people totals
     *  (JOIN shelters for the location context). */
    public List<Object[]> allocationByStatus() throws DataAccessException {
        String sql = """
            SELECT a.status,
                   COUNT(*) AS allocations,
                   COALESCE(SUM(a.people_count), 0) AS people,
                   MIN(a.allocated_at) AS first_on,
                   MAX(a.allocated_at) AS last_on
            FROM shelter_allocations a
            JOIN shelters s ON s.id = a.shelter_id
            GROUP BY a.status ORDER BY a.status
            """;
        return run(sql, List.of());
    }

    /** High-level smart allocation picture (metric cards). */
    public List<Object[]> allocationMetrics() throws DataAccessException {
        String sql = """
            SELECT 'TOTAL ALLOCATIONS' AS metric,
                   (SELECT COUNT(*) FROM shelter_allocations) AS value
            UNION ALL SELECT 'ACTIVE / CHECKED IN',
                   (SELECT COUNT(*) FROM shelter_allocations
                    WHERE status IN ('ACTIVE','CHECKED_IN'))
            UNION ALL SELECT 'PENDING', COUNT(*) FROM shelter_allocations
                WHERE status = 'PENDING'
            UNION ALL SELECT 'COMPLETED', COUNT(*) FROM shelter_allocations
                WHERE status = 'COMPLETED'
            UNION ALL SELECT 'CANCELLED', COUNT(*) FROM shelter_allocations
                WHERE status = 'CANCELLED'
            UNION ALL SELECT 'PEOPLE ALLOCATED',
                   (SELECT COALESCE(SUM(people_count),0)
                    FROM shelter_allocations WHERE status IN
                    ('ACTIVE','CHECKED_IN'))
            UNION ALL SELECT 'WAITING VICTIMS',
                   (SELECT COUNT(*) FROM victims
                    WHERE shelter_status <> 'IN_SHELTER')
            UNION ALL SELECT 'FULL SHELTERS',
                   (SELECT COUNT(*) FROM shelters
                    WHERE available_capacity <= 0)
            UNION ALL SELECT 'AVAILABLE SPACES',
                   (SELECT COALESCE(SUM(available_capacity),0) FROM shelters)
            ORDER BY metric
            """;
        return run(sql, List.of());
    }

    public List<Object[]> resourceInventory() throws DataAccessException {
        String sql = """
            SELECT material_name AS resource, SUM(quantity) AS total_units,
                   SUM(CASE WHEN status = 'DISTRIBUTED' THEN quantity ELSE 0 END) AS used
            FROM donations WHERE donation_type = 'MATERIAL'
            GROUP BY material_name ORDER BY total_units DESC
            """;
        return run(sql, List.of());
    }

    // ── resource & inventory reports (real resources table) ───────────

    /** High-level inventory metric cards over the resources table. */
    public List<Object[]> resourceMetrics() throws DataAccessException {
        String sql = """
            SELECT 'TOTAL RESOURCES' AS metric,
                   (SELECT COUNT(*) FROM resources) AS value
            UNION ALL SELECT 'TOTAL UNITS',
                   (SELECT COALESCE(SUM(available_quantity),0) FROM resources)
            UNION ALL SELECT 'LOW STOCK',
                   (SELECT COUNT(*) FROM resources
                    WHERE available_quantity > 0
                      AND available_quantity < minimum_level)
            UNION ALL SELECT 'OUT OF STOCK',
                   (SELECT COUNT(*) FROM resources
                    WHERE available_quantity <= 0)
            UNION ALL SELECT 'STOCK-IN MOVEMENTS',
                   (SELECT COUNT(*) FROM stock_movements
                    WHERE movement_type = 'STOCK_IN')
            UNION ALL SELECT 'STOCK-OUT MOVEMENTS',
                   (SELECT COUNT(*) FROM stock_movements
                    WHERE movement_type = 'STOCK_OUT')
            UNION ALL SELECT 'DISTRIBUTED UNITS',
                   (SELECT COALESCE(SUM(quantity),0)
                    FROM resource_distributions)
            ORDER BY metric
            """;
        return run(sql, List.of());
    }

    /** Inventory grouped by category (COUNT / SUM over resources). */
    public List<Object[]> resourceByCategory() throws DataAccessException {
        String sql = "SELECT category, COUNT(*) AS count, "
                + "COALESCE(SUM(available_quantity),0) AS units "
                + "FROM resources GROUP BY category ORDER BY units DESC";
        return run(sql, List.of());
    }

    /** Resources currently below their minimum level (with the gap). */
    public List<Object[]> resourceLowStock() throws DataAccessException {
        String sql = """
            SELECT name, category, available_quantity, minimum_level, unit,
                   (minimum_level - available_quantity) AS shortfall
            FROM resources
            WHERE available_quantity < minimum_level
            ORDER BY shortfall DESC
            """;
        return run(sql, List.of());
    }

    /** Stock movements netted by type (SUM IN / SUM OUT). */
    public List<Object[]> resourceNetMovement() throws DataAccessException {
        String sql = """
            SELECT movement_type,
                   COUNT(*) AS moves,
                   COALESCE(SUM(quantity),0) AS units
            FROM stock_movements
            GROUP BY movement_type ORDER BY movement_type
            """;
        return run(sql, List.of());
    }

    /** Distribution record aggregated by destination (COUNT / SUM). */
    public List<Object[]> resourceDistributionByDestination()
            throws DataAccessException {
        String sql = """
            SELECT destination,
                   COUNT(*) AS records,
                   COALESCE(SUM(quantity),0) AS units
            FROM resource_distributions
            GROUP BY destination ORDER BY units DESC
            """;
        return run(sql, List.of());
    }

    /** Resource usage grouped by resource (how much each was distributed). */
    public List<Object[]> resourceUsage() throws DataAccessException {
        String sql = """
            SELECT r.name AS resource, r.category,
                   COUNT(d.id) AS distributions,
                   COALESCE(SUM(d.quantity),0) AS units
            FROM resources r
            LEFT JOIN resource_distributions d ON d.resource_id = r.id
            GROUP BY r.name, r.category ORDER BY units DESC
            """;
        return run(sql, List.of());
    }

    // ── food distribution reports (food_requests / food_distributions) ──

    /** High-level food distribution metric cards. */
    public List<Object[]> foodMetrics() throws DataAccessException {
        String sql = """
            SELECT 'TOTAL REQUESTS' AS metric,
                   (SELECT COUNT(*) FROM food_requests) AS value
            UNION ALL SELECT 'PENDING',
                   (SELECT COUNT(*) FROM food_requests
                    WHERE status = 'PENDING')
            UNION ALL SELECT 'APPROVED',
                   (SELECT COUNT(*) FROM food_requests
                    WHERE status = 'APPROVED')
            UNION ALL SELECT 'ALLOCATED',
                   (SELECT COUNT(*) FROM food_requests
                    WHERE status IN ('ALLOCATED','PARTIALLY_FULFILLED'))
            UNION ALL SELECT 'COMPLETED',
                   (SELECT COUNT(*) FROM food_requests
                    WHERE status = 'COMPLETED')
            UNION ALL SELECT 'TOTAL REQUIRED',
                   (SELECT COALESCE(SUM(required_quantity),0)
                    FROM food_requests)
            UNION ALL SELECT 'TOTAL ALLOCATED',
                   (SELECT COALESCE(SUM(allocated_quantity),0)
                    FROM food_requests)
            UNION ALL SELECT 'TOTAL DISTRIBUTED',
                   (SELECT COALESCE(SUM(quantity),0)
                    FROM food_distributions)
            UNION ALL SELECT 'PEOPLE SERVED',
                   (SELECT COALESCE(SUM(beneficiaries_served),0)
                    FROM food_distributions)
            ORDER BY metric
            """;
        return run(sql, List.of());
    }

    /** Requests aggregated by status (COUNT / SUM). */
    public List<Object[]> foodByStatus() throws DataAccessException {
        String sql = """
            SELECT status,
                   COUNT(*) AS requests,
                   COALESCE(SUM(required_quantity),0) AS required
            FROM food_requests GROUP BY status ORDER BY requests DESC
            """;
        return run(sql, List.of());
    }

    /** Requests aggregated by priority (COUNT / SUM). */
    public List<Object[]> foodByPriority() throws DataAccessException {
        String sql = """
            SELECT priority,
                   COUNT(*) AS requests,
                   COALESCE(SUM(required_quantity),0) AS required
            FROM food_requests GROUP BY priority ORDER BY requests DESC
            """;
        return run(sql, List.of());
    }

    /** Requests grouped by disaster (JOIN disasters). */
    public List<Object[]> foodByDisaster() throws DataAccessException {
        String sql = """
            SELECT d.title AS disaster,
                   COUNT(r.id) AS requests,
                   COALESCE(SUM(r.required_quantity),0) AS required,
                   COALESCE(SUM(r.allocated_quantity),0) AS allocated
            FROM food_requests r
            LEFT JOIN disasters d ON d.id = r.disaster_id
            GROUP BY d.title ORDER BY requests DESC
            """;
        return run(sql, List.of());
    }

    /** Requests grouped by distribution location. */
    public List<Object[]> foodByLocation() throws DataAccessException {
        String sql = """
            SELECT location,
                   COUNT(*) AS requests,
                   COALESCE(SUM(required_quantity),0) AS required
            FROM food_requests GROUP BY location ORDER BY requests DESC
            """;
        return run(sql, List.of());
    }

    /** Actual distribution events aggregated by location (COUNT / SUM). */
    public List<Object[]> foodDistributionsByLocation()
            throws DataAccessException {
        String sql = """
            SELECT location,
                   COUNT(*) AS events,
                   COALESCE(SUM(quantity),0) AS units,
                   COALESCE(SUM(beneficiaries_served),0) AS served
            FROM food_distributions
            GROUP BY location ORDER BY units DESC
            """;
        return run(sql, List.of());
    }

    /** Open requests that still owe food (remaining > 0), biggest first. */
    public List<Object[]> foodRemainingRequirements()
            throws DataAccessException {
        String sql = """
            SELECT r.request_code, r.location, r.required_quantity,
                   r.allocated_quantity, r.status,
                   (r.required_quantity -
                    (SELECT COALESCE(SUM(quantity),0)
                     FROM food_distributions d WHERE d.request_id = r.id))
                   AS remaining
            FROM food_requests r
            WHERE r.status NOT IN ('COMPLETED','CANCELLED')
              AND (r.required_quantity -
                   (SELECT COALESCE(SUM(quantity),0)
                    FROM food_distributions d WHERE d.request_id = r.id)) > 0
            ORDER BY remaining DESC
            """;
        return run(sql, List.of());
    }

    private List<Object[]> run(String sql, List<Object> params)
            throws DataAccessException {
        List<Object[]> rows = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Long) {
                    ps.setLong(i + 1, (Long) p);
                } else if (p instanceof Integer) {
                    ps.setInt(i + 1, (Integer) p);
                } else {
                    ps.setString(i + 1, String.valueOf(p));
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                int cols = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int c = 1; c <= cols; c++) {
                        row[c - 1] = rs.getObject(c);
                    }
                    rows.add(row);
                }
            }
            return rows;
        } catch (SQLException e) {
            throw new DataAccessException("Report query failed", e);
        }
    }
}
