package ru.mirea.hrsystem.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static final String CONFIG_FILE = "database.properties";

    private static final String KEY_URL = "db.url";
    private static final String KEY_USER = "db.user";
    private static final String KEY_PASSWORD = "db.password";
    private static final String KEY_DRIVER = "db.driver";
    private static final String DEFAULT_DRIVER = "org.postgresql.Driver";

    private static final int VALIDATION_TIMEOUT_SEC = 3;

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        this(CONFIG_FILE);
    }

    public DatabaseManager(String configPath) {
        Properties props = loadProperties(configPath);

        this.url = require(props, KEY_URL, configPath);
        this.user = require(props, KEY_USER, configPath);
        this.password = require(props, KEY_PASSWORD, configPath);

        String driver = props.getProperty(KEY_DRIVER, DEFAULT_DRIVER);
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC драйвер не найден: " + driver, e);
        }

        log.info("Конфигурация БД загружена из {}: url={}, user={}", configPath, url, user);
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            boolean valid = connection != null && connection.isValid(VALIDATION_TIMEOUT_SEC);
            if (valid) {
                log.info("Подключение к PostgreSQL успешно: {}", url);
            }
            return valid;
        } catch (SQLException e) {
            log.warn("Не удалось подключиться к PostgreSQL по адресу {} ({}). "
                    + "Приложение перейдёт в автономный режим.", url, e.getMessage());
            return false;
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    private static Properties loadProperties(String configPath) {
        Properties props = new Properties();

        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(configPath)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Конфигурационный файл '" + configPath + "' не найден в classpath. "
                                + "Проверь, что файл src/main/resources/" + configPath + " попал в target/classes");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Ошибка чтения конфигурационного файла '" + configPath + "': " + e.getMessage(), e);
        }

        return props;
    }

    private static String require(Properties props, String key, String configPath) {
        String value = props.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Параметр '" + key + "' не задан в " + configPath);
        }

        return value.trim();
    }
}
