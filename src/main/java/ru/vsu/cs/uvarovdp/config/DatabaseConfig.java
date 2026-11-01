package ru.vsu.cs.uvarovdp.config;

import java.util.Map;
import java.util.Objects;

public record DatabaseConfig(String url, String user, String password) {

    static final String URL_VARIABLE = "DB_URL";
    static final String USER_VARIABLE = "DB_USER";
    static final String PASSWORD_VARIABLE = "DB_PASSWORD";

    static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/library";
    static final String DEFAULT_USER = "library";
    static final String DEFAULT_PASSWORD = "library";

    public DatabaseConfig {
        Objects.requireNonNull(url, "URL базы данных не может быть null");
        Objects.requireNonNull(user, "Пользователь базы данных не может быть null");
        Objects.requireNonNull(password, "Пароль базы данных не может быть null");
    }

    public static DatabaseConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static DatabaseConfig fromEnvironment(Map<String, String> environment) {
        return new DatabaseConfig(
                valueOrDefault(environment, URL_VARIABLE, DEFAULT_URL),
                valueOrDefault(environment, USER_VARIABLE, DEFAULT_USER),
                valueOrDefault(environment, PASSWORD_VARIABLE, DEFAULT_PASSWORD)
        );
    }

    private static String valueOrDefault(Map<String, String> environment, String name, String defaultValue) {
        String value = environment.get(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
