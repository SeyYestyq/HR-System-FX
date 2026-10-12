package ru.mirea.hrsystem.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Менеджер подключения к базе данных PostgreSQL.
 * Загружает параметры подключения из resources/database.properties
 * и обеспечивает fail-fast валидацию конфигурации.
 */
public class DatabaseManager {
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String CONFIG_FILE = "database.properties";

    private final String url;
    private final String user;
    private final String password;
    private final String driver;

    public DatabaseManager() {
        this(CONFIG_FILE);
    }

    public DatabaseManager(String configPath) {
        Properties props = new Properties();
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(configPath)) {
            if (in == null) {
                throw new IllegalStateException("Конфигурационный файл '" + configPath + "' не найден в classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Ошибка чтения конфигурационного файла '" + configPath + "'", e);
        }

        this.url = props.getProperty("db.url");
        this.user = props.getProperty("db.user");
        this.password = props.getProperty("db.password");
        this.driver = props.getProperty("db.driver", "org.postgresql.Driver");

        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Параметр 'db.url' не задан в " + configPath);
        }
        if (user == null || user.isBlank()) {
            throw new IllegalStateException("Параметр 'db.user' не задан в " + configPath);
        }
        if (password == null) {
            throw new IllegalStateException("Параметр 'db.password' не задан в " + configPath);
        }

        try {
            Class.forName(this.driver);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC драйвер не найден: " + this.driver, e);
        }
    }

    /**
     * Создает новое подключение к БД.
     * Вызывающая сторона обязана закрыть Connection через try-with-resources.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Проверяет доступность базы данных (ping).
     * Не выбрасывает исключений, возвращает true при успешном подключении.
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (Exception e) {
            log.warn("Не удалось подключиться к БД: {}", e.getMessage());
            return false;
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }
}
