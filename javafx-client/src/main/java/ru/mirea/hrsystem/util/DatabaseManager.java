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
 * ============================================================================
 * ПРАКТИКА ПО ТЕМЕ: УПРАВЛЕНИЕ ПОДКЛЮЧЕНИЕМ К БД И СВОЙСТВАМИ
 * ИСПОЛНИТЕЛЬ: Эдик (Задача T2)
 * ============================================================================
 *
 * ----------------------------------------------------------------------------
 * КАК РАБОТАЕТ ПРОГРАММА:
 * ----------------------------------------------------------------------------
 * 1. Прочитать файл `database.properties` через `ClassLoader.getResourceAsStream`.
 * 2. Инициализировать подключение к БД (`DriverManager.getConnection`) используя
 *    прочитанные данные.
 * 3. Если базы нет или пароль неверный — выбросить понятное исключение (fail-fast).
 *
 * Разделение ответственности:
 * - Конструктор — fail-fast: падает сразу, если конфига нет или в нём не хватает
 *   обязательных ключей. Молчаливый `null` недопустим.
 * - `getConnection()` — соединение для DAO. Проброс SQLException наружу, чтобы DAO
 *   мог превратить его в `DatabaseException` в своём `catch`.
 * - `testConnection()` — мягкая проверка для выбора режима работы приложения:
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

    /** Таймаут проверки соединения, секунды. */
    private static final int VALIDATION_TIMEOUT_SEC = 3;

    /** Значения подключения, прочитанные один раз при создании объекта. */
    private final String url;
    private final String user;
    private final String password;

    /**
     * Читает конфиг и валидирует его.
     *
     * @throws IllegalStateException если {@code database.properties} отсутствует в classpath
     *                               или в нём пустое/отсутствующее значение — fail-fast
     *                               с понятным текстом вместо NPE.
     */
    public DatabaseManager() {
        Properties props = loadProperties();

        this.url = require(props, KEY_URL);
        this.user = require(props, KEY_USER);
        this.password = require(props, KEY_PASSWORD);

        log.info("Конфигурация БД загружена из {}: url={}, user={}", CONFIG_FILE, url, user);
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

    /**
     * Читает {@code database.properties} из classpath.
     *
     * @throws IllegalStateException если ресурса нет или он не читается
     */
    private static Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "database.properties не найден в classpath (" + CONFIG_FILE + "). "
                                + "Проверь, что файл src/main/resources/" + CONFIG_FILE + " попал в target/classes");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Не удалось прочитать " + CONFIG_FILE + " из classpath: " + e.getMessage(), e);
        }

        return props;
    }

    /**
     * Достаёт обязательный ключ или падает с fail-fast.
     *
     * @param key имя ключа в {@code database.properties}
     * @return непустое значение ключа
     * @throws IllegalStateException если ключ отсутствует или его значение пустое
     */
    private static String require(Properties props, String key) {
        String value = props.getProperty(key);

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "database.properties не найден или " + key + " не задан");
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