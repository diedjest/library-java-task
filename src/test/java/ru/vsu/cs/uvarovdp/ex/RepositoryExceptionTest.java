package ru.vsu.cs.uvarovdp.ex;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("Тестирование RepositoryException")
class RepositoryExceptionTest {

    @Test
    @DisplayName("Сохраняет сообщение и исходную причину, является AppException")
    void shouldKeepMessageAndCause() {
        SQLException cause = new SQLException("boom");

        RepositoryException exception = new RepositoryException("Не удалось сохранить книгу", cause);

        assertEquals("Не удалось сохранить книгу", exception.getMessage());
        assertSame(cause, exception.getCause());
        assertInstanceOf(AppException.class, exception);
    }
}
