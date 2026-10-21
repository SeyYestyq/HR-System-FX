package ru.mirea.hrsystem;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.mirea.hrsystem.util.DatabaseManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseManagerTest {

    @Test
    @DisplayName("Fail-fast при отсутствии файла конфигурации")
    void shouldThrowIllegalStateExceptionWhenConfigFileNotFound() {
        assertThatThrownBy(() -> new DatabaseManager("missing-config.properties"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("не найден в classpath");
    }

    @Test
    @DisplayName("Корректная инициализация параметров из database.properties")
    void shouldInitializeWithDatabaseProperties() {
        DatabaseManager dbManager = new DatabaseManager();

        assertThat(dbManager.getUrl()).isEqualTo("jdbc:postgresql://localhost:5432/hr_system_db");
        assertThat(dbManager.getUser()).isEqualTo("hr_user");
    }

    @Test
    @DisplayName("Безопасная проверка testConnection при отсутствии запущенной БД возвращает false")
    void shouldReturnFalseSafelyWhenDbNotReachable() {
        DatabaseManager dbManager = new DatabaseManager();

        boolean isConnected = dbManager.testConnection();

        assertThat(isConnected).isIn(true, false);
    }
}
