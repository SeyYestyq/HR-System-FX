package ru.mirea.hrsystem.util;

import java.sql.Connection;

/**
 * ============================================================================
 * ПРАКТИКА ПО ТЕМЕ: УПРАВЛЕНИЕ ПОДКЛЮЧЕНИЕМ К БД И СВОЙСТВАМИ
 * ИСПОЛНИТЕЛЬ: Эдик (Задача T2)
 * ============================================================================
 * 
 * Тебе нужно скрыть хардкод паролей и логинов от базы данных.
 * 
 * ----------------------------------------------------------------------------
 * КАК ДОЛЖНА РАБОТАТЬ ПРОГРАММА:
 * ----------------------------------------------------------------------------
 * 1. Прочитать файл `database.properties` через `ClassLoader.getResourceAsStream`.
 * 2. Инициализировать подключение к БД (`DriverManager.getConnection`) используя
 *    прочитанные данные.
 * 3. Если базы нет или пароль неверный — выбросить понятное исключение (fail-fast).
 * 
 * Если нужна теория — она в самом конце файла.
 * 
 * ----------------------------------------------------------------------------
 * ШПАРГАЛКА (Код из преподского проекта Student Hub):
 * ----------------------------------------------------------------------------
 * /*
 *     // В student-hub-suite-rebuilt конфигурация БД спрятана в application.yml (Spring).
 *     // Но в рамках чистого JDBC это делается так:
 *     Properties props = new Properties();
 *     try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream("database.properties")) {
 *         props.load(in);
 *     }
 *     Connection conn = DriverManager.getConnection(props.getProperty("url"), props);
 * *\/
 */
public class DatabaseManager {
    /**
     * Заглушка для сборки UI. Реальное чтение {@code database.properties} делает Эдик.
     * Пока метод возвращает {@code false}, приложение работает на стабе.
     */
    public boolean testConnection() {
        return false;
    }

    public Connection getConnection() {
        throw new UnsupportedOperationException("Подключение к PostgreSQL ещё не реализовано");
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
