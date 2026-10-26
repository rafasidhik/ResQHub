package com.resqhub.dao;

import java.sql.Connection;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.resqhub.model.Disaster;
import com.resqhub.model.Shelter;
import com.resqhub.model.User;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying HikariCP connection health and DAO read queries.
 *
 * SAFETY NOTICE:
 * All tests in this suite are strictly READ-ONLY against the configured database (resqhub_web).
 * They verify connectivity, active pools, and seed record retrieval without inserting, updating,
 * or deleting any operational or demo records.
 */
@SpringBootTest
@DisplayName("Database and JDBC Integration Tests")
class DatabaseIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("HikariCP DataSource establishes a valid JDBC connection to MySQL")
    void testDataSourceConnection() throws Exception {
        assertNotNull(dataSource, "DataSource should be autowired by Spring Boot");
        try (Connection con = dataSource.getConnection()) {
            assertNotNull(con, "Connection should not be null");
            assertFalse(con.isClosed(), "Connection should be active");
            assertTrue(con.isValid(2), "Connection should be valid within 2 seconds");
        }
    }

    @Test
    @DisplayName("UserDAO successfully reads seed accounts from database")
    void testUserDaoFindAll() throws Exception {
        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.findAll();

        assertNotNull(users);
        assertFalse(users.isEmpty(), "Users table should have seed data");

        boolean hasAdmin = users.stream().anyMatch(u -> "admin".equals(u.getUsername()));
        assertTrue(hasAdmin, "Users list should contain admin account");
    }

    @Test
    @DisplayName("DisasterDAO successfully loads disaster records")
    void testDisasterDaoFindAll() throws Exception {
        DisasterDAO disasterDAO = new DisasterDAO();
        List<Disaster> disasters = disasterDAO.findAll();

        assertNotNull(disasters);
        assertFalse(disasters.isEmpty(), "Disasters table should contain recorded events");
    }

    @Test
    @DisplayName("ShelterDAO successfully loads relief shelters")
    void testShelterDaoFindAll() throws Exception {
        ShelterDAO shelterDAO = new ShelterDAO();
        List<Shelter> shelters = shelterDAO.findAll();

        assertNotNull(shelters);
        assertFalse(shelters.isEmpty(), "Shelters table should contain relief camps");
    }
}
