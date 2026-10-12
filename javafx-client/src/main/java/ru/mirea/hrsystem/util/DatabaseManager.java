package ru.mirea.hrsystem.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.hrsystem.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(2);
        } catch (DatabaseException exception) {
            if (exception.getCause() instanceof SQLException) {
                log.warn("PostgreSQL недоступен: {}", exception.getMessage());
                return false;
            }
            throw exception;
        } catch (SQLException exception) {
            log.warn("PostgreSQL недоступен", exception);
            return false;
        }
    }

    public Connection getConnection() {
        Properties file = loadProperties();
        String url = required(file, "db.url");
        String user = required(file, "db.user");
        String password = required(file, "db.password");
        String driver = file.getProperty("db.driver", "org.postgresql.Driver");
        try {
            Class.forName(driver);
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException exception) {
            throw new DatabaseException("Драйвер PostgreSQL не найден: " + driver, exception);
        } catch (SQLException exception) {
            throw new DatabaseException("Не удалось подключиться к PostgreSQL", exception);
        }
    }

    private Properties loadProperties() {
        try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new DatabaseException("Файл database.properties не найден. Приложение не может стартовать.");
            }
            Properties properties = new Properties();
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new DatabaseException("Не удалось прочитать database.properties", exception);
        }
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DatabaseException("В database.properties нет значения " + key);
        }
        return value.trim();
    }
}
