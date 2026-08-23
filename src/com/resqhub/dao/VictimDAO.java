package com.resqhub.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.resqhub.exception.DataAccessException;
import com.resqhub.model.EmergencyStatus;
import com.resqhub.model.Gender;
import com.resqhub.model.ShelterStatus;
import com.resqhub.model.Victim;

/** JDBC data access for the victims table. */
public class VictimDAO extends BaseDao implements Repository<Victim> {

    @Override
    public Victim save(Victim victim) throws DataAccessException {
        if (victim.getId() == null) {
            return insert(victim);
        }
        return update(victim);
    }

    private Victim insert(Victim v) throws DataAccessException {
        String sql = "INSERT INTO victims (full_name, age, gender, phone, "
                + "emergency_status, medical_condition, family_info, current_location, "
                + "shelter_status, disaster_id, registered_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql,
                     Statement.RETURN_GENERATED_KEYS)) {

            bindColumns(ps, v);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new DataAccessException("No generated id returned for victim");
                }
                long newId = keys.getLong(1);
                return findById(newId);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not save victim: " + v.getFullName(), e);
        }
    }

    private Victim update(Victim v) throws DataAccessException {
        String sql = "UPDATE victims SET full_name = ?, age = ?, gender = ?, phone = ?, "
                + "emergency_status = ?, medical_condition = ?, family_info = ?, "
                + "current_location = ?, shelter_status = ?, disaster_id = ?, "
                + "registered_by = ? WHERE id = ?";

        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            bindColumns(ps, v);
            ps.setLong(12, v.getId());
            int rows = ps.executeUpdate();
            if (rows != 1) {
                throw new DataAccessException(
                        "Victim update affected " + rows + " rows for id " + v.getId());
            }
            return findById(v.getId());
        } catch (SQLException e) {
            throw new DataAccessException("Could not update victim " + v.getId(), e);
        }
    }

    private void bindColumns(PreparedStatement ps, Victim v) throws SQLException {
        ps.setString(1, v.getFullName());
        ps.setInt(2, v.getAge());
        ps.setString(3, enumOrNull(v.getGender()));
        ps.setString(4, v.getPhone());
        ps.setString(5, enumOrNull(v.getEmergencyStatus()));
        ps.setString(6, v.getMedicalCondition());
        ps.setString(7, v.getFamilyInfo());
        ps.setString(8, v.getCurrentLocation());
        ps.setString(9, enumOrNull(v.getShelterStatus()));
        ps.setLong(10, v.getDisasterId());
        bindNullableLong(ps, 11, v.getRegisteredBy());
    }

    @Override
    public Victim findById(long id) throws DataAccessException {
        String sql = "SELECT * FROM victims WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not load victim " + id, e);
        }
    }

    public List<Victim> findByDisaster(long disasterId) throws DataAccessException {
        String sql = "SELECT * FROM victims WHERE disaster_id = ? ORDER BY full_name";
        List<Victim> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, disasterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list victims for disaster", e);
        }
    }

    @Override
    public List<Victim> findAll() throws DataAccessException {
        String sql = "SELECT * FROM victims ORDER BY created_at DESC";
        List<Victim> result = new ArrayList<>();
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list victims", e);
        }
    }

    @Override
    public boolean deleteById(long id) throws DataAccessException {
        String sql = "DELETE FROM victims WHERE id = ?";
        try (Connection con = openConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setLong(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete victim " + id, e);
        }
    }

    private Victim mapRow(ResultSet rs) throws SQLException {
        Victim v = new Victim();
        v.setId(rs.getLong("id"));
        v.setFullName(rs.getString("full_name"));
        v.setAge(rs.getInt("age"));
        v.setGender(readEnum(Gender.class, rs.getString("gender")));
        v.setPhone(rs.getString("phone"));
        v.setEmergencyStatus(readEnum(EmergencyStatus.class,
                rs.getString("emergency_status")));
        v.setMedicalCondition(rs.getString("medical_condition"));
        v.setFamilyInfo(rs.getString("family_info"));
        v.setCurrentLocation(rs.getString("current_location"));
        v.setShelterStatus(readEnum(ShelterStatus.class,
                rs.getString("shelter_status")));
        v.setDisasterId(rs.getLong("disaster_id"));
        long registeredBy = rs.getLong("registered_by");
        if (!rs.wasNull()) {
            v.setRegisteredBy(registeredBy);
        }
        v.setCreatedAt(readLocalDateTime(rs, "created_at"));
        v.setUpdatedAt(readLocalDateTime(rs, "updated_at"));
        return v;
    }
}
