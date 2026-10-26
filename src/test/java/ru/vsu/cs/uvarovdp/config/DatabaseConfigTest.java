package ru.vsu.cs.uvarovdp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Тестирование DatabaseConfig")
class DatabaseConfigTest {

    @Test
    @DisplayName("Без переменных окружения используются значения по умолчанию")
    void shouldUseDefaultsWhenEnvironmentIsEmpty() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment(Map.of());

        assertEquals("jdbc:postgresql://localhost:5432/library", config.url());
        assertEquals("library", config.user());
        assertEquals("library", config.password());
    }

    @Test
    @DisplayName("Переменные окружения переопределяют значения по умолчанию")
    void shouldReadValuesFromEnvironment() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment(Map.of(
                "DB_URL", "jdbc:postgresql://db:5432/other",
                "DB_USER", "reader",
                "DB_PASSWORD", "secret"
        ));

        assertEquals("jdbc:postgresql://db:5432/other", config.url());
        assertEquals("reader", config.user());
        assertEquals("secret", config.password());
    }

    @Test
    @DisplayName("Пустые переменные окружения заменяются значениями по умолчанию")
    void shouldFallBackToDefaultsForBlankValues() {
        DatabaseConfig config = DatabaseConfig.fromEnvironment(Map.of("DB_URL", "  ", "DB_USER", ""));

        assertEquals("jdbc:postgresql://localhost:5432/library", config.url());
        assertEquals("library", config.user());
    }

    @Test
    @DisplayName("Конструктор отклоняет null")
    void shouldRejectNullValues() {
        assertThrows(NullPointerException.class, () -> new DatabaseConfig(null, "user", "password"));
        assertThrows(NullPointerException.class, () -> new DatabaseConfig("url", null, "password"));
        assertThrows(NullPointerException.class, () -> new DatabaseConfig("url", "user", null));
    }
}
