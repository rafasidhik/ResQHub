package com.resqhub.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.resqhub.exception.ResQHubException;

/**
 * SINGLETON - the one and only source of JDBC connections.
 *
 * WEB MIGRATION NOTES
 *  - Still hands out a FRESH connection per call (callers close it with
 *    try-with-resources), so no connection is ever shared between users.
 *  - Configuration is looked up in this order, first match wins:
 *      1. environment variables RESQHUB_DB_URL / RESQHUB_DB_USER /
 *         RESQHUB_DB_PASSWORD
 *      2. the file named by the system property -Dresqhub.config=/path
 *      3. $CATALINA_BASE/conf/resqhub.properties  (outside the WAR)
 *      4. config/db.properties on the classpath (WEB-INF/classes, which
 *         Tomcat never serves to browsers)
 *  - The password is never logged.
 */
public final class DatabaseConnectionManager {

    private static final String CLASSPATH_FILE = "config/db.properties";
    private static final String URL_KEY = "db.url";
    private static final String USER_KEY = "db.username";
    private static final String PASSWORD_KEY = "db.password";

    private static DatabaseConnectionManager instance;
    private static javax.sql.DataSource springDataSource;

    private final String url;
    private final String username;
    private final String password;

    public static synchronized void setDataSource(javax.sql.DataSource ds) {
        springDataSource = ds;
    }

    private DatabaseConnectionManager() throws ResQHubException {
        Properties props = loadProperties();

        String envUrl = System.getenv("RESQHUB_DB_URL");
        String envUser = System.getenv("RESQHUB_DB_USER");
        String envPass = System.getenv("RESQHUB_DB_PASSWORD");

        this.url = envUrl != null ? envUrl : props.getProperty(URL_KEY, "");
        this.username = envUser != null ? envUser : props.getProperty(USER_KEY, "");
        this.password = envPass != null ? envPass : props.getProperty(PASSWORD_KEY, "");

        if (url.isEmpty() || username.isEmpty()) {
            throw new ResQHubException(
                    "Database is not configured. Set " + URL_KEY + " and "
                    + USER_KEY + " (see README, step 'Database credentials').");
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ResQHubException("MySQL JDBC driver is missing from the WAR", e);
        }
    }

    private static Properties loadProperties() throws ResQHubException {
        Properties props = new Properties();
        try {
            Path external = externalConfigFile();
            if (external != null) {
                try (InputStream in = Files.newInputStream(external)) {
                    props.load(in);
                }
                return props;
            }
            try (InputStream in = DatabaseConnectionManager.class
                    .getClassLoader().getResourceAsStream(CLASSPATH_FILE)) {
                if (in != null) {
                    props.load(in);
                }
            }
        } catch (IOException e) {
            throw new ResQHubException("Could not read database configuration", e);
        }
        return props;
    }

    private static Path externalConfigFile() {
        String explicit = System.getProperty("resqhub.config");
        if (explicit != null && !explicit.isBlank()) {
            Path p = Paths.get(explicit);
            if (Files.isReadable(p)) {
                return p;
            }
        }
        String base = System.getProperty("catalina.base");
        if (base != null) {
            Path p = Paths.get(base, "conf", "resqhub.properties");
            if (Files.isReadable(p)) {
                return p;
            }
        }
        return null;
    }

    public static synchronized DatabaseConnectionManager getInstance()
            throws ResQHubException {
        if (instance == null) {
            instance = new DatabaseConnectionManager();
        }
        return instance;
    }

    /** Returns a fresh open JDBC connection. Callers MUST close it. */
    public Connection getConnection() throws SQLException {
        if (springDataSource != null) {
            return springDataSource.getConnection();
        }
        return DriverManager.getConnection(url, username, password);
    }
}
