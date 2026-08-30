package com.resqhub.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.resqhub.exception.ResQHubException;

/**
 * SINGLETON PATTERN - the one and only source of JDBC connections.
 *
 * Why a Singleton is appropriate here:
 *  - DB configuration is loaded once from resources/config/db.properties.
 *  - Every DAO in the system needs identical connection settings.
 *  - One instance = one place to change DB settings.
 *
 * Known drawbacks (viva answer): global shared state makes unit testing
 * harder and hides dependencies; a dependency-injection container would
 * solve that but is out of scope for a semester project.
 */
public final class DatabaseConnectionManager {

    private static final String CONFIG_FILE = "config/db.properties";
    private static final String URL_KEY = "db.url";
    private static final String USER_KEY = "db.username";
    private static final String PASSWORD_KEY = "db.password";

    private static DatabaseConnectionManager instance;

    private final String url;
    private final String username;
    private final String password;

    private DatabaseConnectionManager() throws ResQHubException {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnectionManager.class
                .getClassLoader().getResourceAsStream(CONFIG_FILE)) {

            if (in == null) {
                throw new ResQHubException(
                    "Configuration file not found on classpath: " + CONFIG_FILE);
            }
            props.load(in);
        } catch (IOException e) {
            throw new ResQHubException("Could not read database configuration", e);
        }

        this.url = props.getProperty(URL_KEY, "");
        this.username = props.getProperty(USER_KEY, "");
        this.password = props.getProperty(PASSWORD_KEY, "");

        if (url.isEmpty() || username.isEmpty()) {
            throw new ResQHubException(
                URL_KEY + " and " + USER_KEY + " must be set in " + CONFIG_FILE);
        }
    }

    public static synchronized DatabaseConnectionManager getInstance()
            throws ResQHubException {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    /**
     * Returns a fresh open JDBC connection. Callers MUST close it,
     * preferably with try-with-resources.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public String getUrl() {
        return url;
    }
}
