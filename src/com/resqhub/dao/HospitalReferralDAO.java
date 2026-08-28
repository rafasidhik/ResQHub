package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.HospitalFacility;
import com.resqhub.model.HospitalReferral;
import com.resqhub.model.HospitalReferralStatus;

/**
 * JDBC data access for the hospital_referrals table - emergency victim
 * referrals to hospitals (spec section 11). Tracks which beds a referral
 * requires so the system can enforce hospital capacity.
 */
public class HospitalReferralDAO extends BaseDao
        implements Repository<HospitalReferral> {

    @Override
    public HospitalReferral save(HospitalReferral r)
            throws DataAccessException {
        if (r.getId() == null) {
            return insert(r);
        }
        return update(r);
    }

    private HospitalReferral insert(HospitalReferral r)
            throws DataAccessException {
        String sql = "INSERT INTO hospital_referrals (hospital_id, victim_id, "
                + "victim_name, reason, beds_required, required_facilities, "
                + "status, beds_applied, referred_by, referred_at, closed_at, "
                + "notes, disaster_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
                + "?, ?, ?)";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {
            int idx = bindColumns(ps, r, 1);
            ps.setBoolean(idx, r.isBedsApplied());
            bindNullableLong(ps, idx + 1, r.getDisasterId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException(
                            "No generated id for hospital referral");
                }
                return findById(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not save hospital referral", e);
        }
    }

    private HospitalReferral update(HospitalReferral r)
            throws DataAccessException {
        String sql = "UPDATE hospital_referrals SET hospital_id = ?, "
                + "victim_id = ?, victim_name = ?, reason = ?, "
                + "beds_required = ?, required_facilities = ?, status = ?, "
                + "beds_applied = ?, referred_by = ?, referred_at = ?, "
                + "closed_at = ?, notes = ?, disaster_id = ? WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int idx = bindColumns(ps, r, 1);
            ps.setBoolean(idx, r.isBedsApplied());
            bindNullableLong(ps, idx + 1, r.getDisasterId());
            ps.setLong(idx + 2, r.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Referral update affected " + rows + " rows for id "
                                + r.getId());
            }
            return findById(r.getId());
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not update hospital referral " + r.getId(), e);
        }
    }

    private int bindColumns(PreparedStatement ps, HospitalReferral r,
            int start) throws SQLException {
        bindNullableLong(ps, start, r.getHospitalId());
        bindNullableLong(ps, start + 1, r.getVictimId());
        ps.setString(start + 2, r.getVictimName());
        ps.setString(start + 3, r.getReason());
        ps.setInt(start + 4, r.getBedsRequired());
        ps.setString(start + 5, encodeFacilities(r.getRequiredFacilities()));
        ps.setString(start + 6, enumOrNull(r.getStatus()));
        bindNullableLong(ps, start + 7, r.getReferredBy());
        bindLocalDateTime(ps, start + 8, r.getReferredAt());
        bindLocalDateTime(ps, start + 9, r.getClosedAt());
        ps.setString(start + 10, r.getNotes());
        return start + 11;
    }

    @Override
    public HospitalReferral findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM hospital_referrals WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not load hospital referral " + id, e);
        }
    }

    @Override
    public List<HospitalReferral> findAll() throws DataAccessException {
        String sql = "SELECT * FROM hospital_referrals "
                + "ORDER BY referred_at DESC, id DESC";
        return query(sql, List.of());
    }

    public List<HospitalReferral> findByHospital(long hospitalId)
            throws DataAccessException {
        String sql = "SELECT * FROM hospital_referrals WHERE hospital_id = ? "
                + "ORDER BY referred_at DESC, id DESC";
        return query(sql, List.of(hospitalId));
    }

    public List<HospitalReferral> findByVictim(long victimId)
            throws DataAccessException {
        String sql = "SELECT * FROM hospital_referrals WHERE victim_id = ? "
                + "ORDER BY referred_at DESC, id DESC";
        return query(sql, List.of(victimId));
    }

    public List<HospitalReferral> findByStatus(HospitalReferralStatus status)
            throws DataAccessException {
        String sql = "SELECT * FROM hospital_referrals WHERE status = ? "
                + "ORDER BY referred_at DESC, id DESC";
        return query(sql, List.of(enumOrNull(status)));
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM hospital_referrals WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not delete hospital referral " + id, e);
        }
    }

    public List<HospitalReferral> findOpen() throws DataAccessException {
        String sql = "SELECT * FROM hospital_referrals WHERE status IN ("
                + "'PENDING','ACCEPTED','ADMITTED') "
                + "ORDER BY referred_at DESC, id DESC";
        return query(sql, List.of());
    }

    /** Beds committed by open referrals to a hospital. */
    public int sumOpenBeds(long hospitalId) throws DataAccessException {
        String sql = "SELECT COALESCE(SUM(beds_required),0) "
                + "FROM hospital_referrals WHERE hospital_id = ? "
                + "AND status IN ('PENDING','ACCEPTED','ADMITTED')";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, hospitalId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException(
                    "Could not sum open referral beds", e);
        }
    }

    private List<HospitalReferral> query(String sql, List<Object> params)
            throws DataAccessException {
        List<HospitalReferral> result = new ArrayList<>();
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
            throw new DataAccessException("Hospital referral query failed", e);
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

    private HospitalReferral mapRow(ResultSet rs) throws SQLException {
        HospitalReferral r = new HospitalReferral();
        r.setId(rs.getLong("id"));
        r.setHospitalId(getObjectOrNull(rs, "hospital_id"));
        r.setVictimId(getObjectOrNull(rs, "victim_id"));
        r.setVictimName(rs.getString("victim_name"));
        r.setReason(rs.getString("reason"));
        r.setBedsRequired(rs.getInt("beds_required"));
        r.setRequiredFacilities(decodeFacilities(
                rs.getString("required_facilities")));
        r.setStatus(readEnum(HospitalReferralStatus.class,
                rs.getString("status")));
        r.setBedsApplied(rs.getBoolean("beds_applied"));
        r.setReferredBy(getObjectOrNull(rs, "referred_by"));
        r.setReferredAt(readLocalDateTime(rs, "referred_at"));
        r.setClosedAt(readLocalDateTime(rs, "closed_at"));
        r.setNotes(rs.getString("notes"));
        r.setDisasterId(getObjectOrNull(rs, "disaster_id"));
        r.setCreatedAt(readLocalDateTime(rs, "created_at"));
        r.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return r;
    }

    private String encodeFacilities(Set<HospitalFacility> facilities) {
        if (facilities == null || facilities.isEmpty()) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (HospitalFacility f : facilities) {
            if (f != null) {
                names.add(f.name());
            }
        }
        return String.join(",", names);
    }

    private Set<HospitalFacility> decodeFacilities(String value) {
        Set<HospitalFacility> set = new LinkedHashSet<>();
        if (value == null || value.isEmpty()) {
            return set;
        }
        for (String part : value.split(",")) {
            String name = part.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                set.add(HospitalFacility.valueOf(name));
            } catch (IllegalArgumentException ignored) {
                // unknown facility - skip
            }
        }
        return set;
    }

    private Long getObjectOrNull(ResultSet rs, String column)
            throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
