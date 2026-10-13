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
 * <p>
 * Загружает параметры подключения из {@code resources/database.properties}
 * и обеспечивает fail-fast валидацию конфигурации.
 * <p>
 * Контракт:
 * - Конструктор — fail-fast: падает сразу, если конфига нет или в нём не хватает
 *   обязательных ключей. Молчаливый {@code null} недопустим.
 * - {@link #getConnection()} — соединение для DAO. Проброс SQLException наружу, чтобы DAO
 *   мог превратить его в {@code DatabaseException} в своём {@code catch}.
 * - {@link #testConnection()} — мягкая проверка для выбора режима работы приложения:
 *   БД поднята — реальные DAO, БД недоступна — стабы. Исключений не бросает.
 *
 * Если нужна теория — она в самом конце файла.
 */
public class DatabaseManager {
    /** SLF4J, а не System.out: логи уходят в консоль приложения, а не в stdout. */
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    /** Ресурс читается из classpath, поэтому и лежит в src/main/resources. */
    private static final String CONFIG_FILE = "database.properties";

    private static final String KEY_URL = "db.url";
    private static final String KEY_USER = "db.user";
    private static final String KEY_PASSWORD = "db.password";
    private static final String KEY_DRIVER = "db.driver";
    private static final String DEFAULT_DRIVER = "org.postgresql.Driver";

    /** Таймаут проверки соединения, секунды. */
    private static final int VALIDATION_TIMEOUT_SEC = 3;

    /** Значения подключения, прочитанные один раз при создании объекта. */
    private final String url;
    private final String user;
    private final String password;

    /**
     * Читает конфиг из {@code database.properties} и валидирует его.
     *
     * @throws IllegalStateException если {@code database.properties} отсутствует в classpath
     *                               или в нём пустое/отсутствующее значение — fail-fast
     *                               с понятным текстом вместо NPE.
     */
    public DatabaseManager() {
        this(CONFIG_FILE);
    }

    /**
     * Конструктор с явным указанием пути к файлу конфигурации.
     * Удобен для тестов, когда нужно подменить конфиг.
     *
     * @param configPath путь к файлу конфигурации в classpath
     */
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

    /**
     * Открывает новое соединение с БД по параметрам из конфига.
     * Соединение закрывает вызывающий код через try-with-resources.
     *
     * @throws SQLException если БД недоступна либо логин/пароль неверны
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Мягкая проверка доступности БД — вызывается один раз при старте приложения.
     * Не бросает исключений: недоступная БД означает автономный режим со стабами,
     * а не падение программы. Отсутствие конфига, наоборот, — фатально (fail-fast
     * в конструкторе), потому что это ошибка сборки, а не окружения.
     *
     * @return {@code true}, если соединение установлено
     */
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

    /** URL из конфига. Нужен UI для показа, к какой базе шло подключение. */
    public String getUrl() {
        return url;
    }

    /** Имя пользователя из конфига. */
    public String getUser() {
        return user;
    }

    /**
     * Читает файл конфигурации из classpath.
     *
     * @param configPath путь к ресурсу в classpath
     * @throws IllegalStateException если ресурса нет или он не читается
     */
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

    /**
     * Достаёт обязательный ключ или падает с fail-fast.
     *
     * @param props      properties-файл
     * @param key        имя ключа в конфиге
     * @param configPath путь к конфигу (для сообщения об ошибке)
     * @return непустое значение ключа
     * @throws IllegalStateException если ключ отсутствует или его значение пустое
     */
    private static String require(Properties props, String key, String configPath) {
        String value = props.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Параметр '" + key + "' не задан в " + configPath);
        }

        return value.trim();
    }
}
/*
 * ============================================================================
 * ТЕОРИЯ: ClassLoader и Properties
 * ============================================================================
 * 1. В скомпилированном проекте ресурсы (файлы .properties, .xml, .css)
 * запаковываются внутрь JAR-файла. Читать их через `new File("...")` нельзя.
 * Правильный способ: `getClassLoader().getResourceAsStream("имя_файла")`.
 * 2. java.util.Properties — удобный класс для чтения настроек в формате `ключ=значение`.
 */
