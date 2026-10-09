package ru.mirea.hrsystem;

/**
 * Точка входа в приложение без наследования от Application.
 * Необходима для корректного запуска «толстых» JAR-файлов без модульной системы JavaFX.
 */
public class Launcher {
    public static void main(String[] args) {
        HrApplication.main(args);
    }
}
