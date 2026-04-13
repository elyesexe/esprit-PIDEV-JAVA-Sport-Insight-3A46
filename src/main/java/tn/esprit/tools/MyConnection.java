package tn.esprit.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyConnection {
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/sport_insight?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static MyConnection instance;
    private final Connection connection;

    private MyConnection() throws SQLException {
        connection = DriverManager.getConnection(URL, USER, PASSWORD);
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
}
