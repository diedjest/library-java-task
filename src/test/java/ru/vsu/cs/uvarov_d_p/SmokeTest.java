package ru.vsu.cs.uvarov_d_p;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.vsu.cs.uvarov_d_p.domain.BookConstraints;
import ru.vsu.cs.uvarov_d_p.util.impl.IsbnValidatorImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Дымовое тестирование конфигурации проекта")
class SmokeTest {

    @Test
    @DisplayName("Контекст и базовые классы проекта успешно инициализируются")
    void shouldVerifyBasicConfigurationAndClasses() {
        IsbnValidatorImpl validator = new IsbnValidatorImpl();

        boolean isValid = validator.isValid("978-5-4461-0960-9");

        assertTrue(isValid, "Валидатор должен подтверждать корректный ISBN-13");
        assertEquals(3, BookConstraints.MAX_GENRES, "Лимит жанров должен быть равен 3");
    }
}