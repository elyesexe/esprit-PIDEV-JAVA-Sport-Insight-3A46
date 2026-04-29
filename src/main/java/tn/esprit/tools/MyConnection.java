package tn.esprit.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MyConnection {
    private static final String HOST = "127.0.0.1";
    private static final int PORT = 3306;
    private static final String DATABASE = "sport_insight";
    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DATABASE + "?useSSL=false&serverTimezone=UTC&connectTimeout=3000&socketTimeout=5000";
    private static final String SERVER_URL = "jdbc:mysql://" + HOST + ":" + PORT + "/?useSSL=false&serverTimezone=UTC&connectTimeout=3000&socketTimeout=5000";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyConnection instance;
    private final Connection connection;

    private MyConnection() throws SQLException {
        connection = openConnection();
        SchemaMigration.ensureFootballDataColumns(connection);
    }

    public static synchronized MyConnection getInstance() throws SQLException {
        if (instance == null || instance.hasInvalidConnection()) {
            instance = new MyConnection();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    private boolean hasInvalidConnection() {
        try {
            return connection == null || connection.isClosed() || !connection.isValid(2);
        } catch (SQLException e) {
            return true;
        }
    }

    private Connection openConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException ex) {
            if (isUnknownDatabase(ex)) {
                createDatabaseIfMissing();
                return DriverManager.getConnection(URL, USER, PASSWORD);
            }
            throw ex;
        }
    }

    private void createDatabaseIfMissing() throws SQLException {
        try (Connection serverConnection = DriverManager.getConnection(SERVER_URL, USER, PASSWORD);
             Statement statement = serverConnection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + DATABASE + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }

    private boolean isUnknownDatabase(SQLException ex) {
        return ex.getErrorCode() == 1049 || (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("unknown database"));
    }
}
