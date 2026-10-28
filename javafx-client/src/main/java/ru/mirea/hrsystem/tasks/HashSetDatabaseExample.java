package ru.mirea.hrsystem.tasks;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class HashSetDatabaseExample {
    private static final String URL = "jdbc:postgresql://localhost:5432/hr_system_db";
    private static final String USER = "hr_user";
    private static final String PASSWORD = "hr_password";

    public static void main(String[] args) throws SQLException {
        Set<String> elements = new HashSet<>();
        elements.add("Саня");
        elements.add("Гена");
        elements.add("Антон");

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            createTable(connection);
            insertElements(connection, elements);
        }
    }

    private static void createTable(Connection connection) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS hashset_elements (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "element VARCHAR(255) UNIQUE)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private static void insertElements(Connection connection, Set<String> elements) throws SQLException {
        String sql = "INSERT INTO hashset_elements (element) VALUES (?) ON CONFLICT (element) DO NOTHING";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (String elem : elements) {
                ps.setString(1, elem);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
