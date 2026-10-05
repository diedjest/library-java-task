package ru.vsu.cs.uvarov_d_p.util.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.vsu.cs.uvarov_d_p.util.IsbnValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование валидатора IsbnValidatorImpl")
class IsbnValidatorImplTest {

    private final IsbnValidator validator = new IsbnValidatorImpl();

    @Test
    @DisplayName("normalize возвращает пустую строку при null на входе")
    void shouldReturnEmptyStringWhenNormalizingNull() {
        String normalized = validator.normalize(null);

        assertEquals("", normalized);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "978-5-4461-0960-9 | 9785446109609",
            "0 306 40615 2     | 0306406152",
            "0-306-40615-x     | 030640615X",
            "9785446109609     | 9785446109609",
            "030640615X        | 030640615X",
            "'  - -   '        | ''",
            "''                | ''"
    })
    @DisplayName("normalize корректно удаляет пробелы и дефисы, переводя буквы в верхний регистр")
    void shouldNormalizeVariousIsbnFormats(String input, String expected) {
        String result = validator.normalize(input);

        assertEquals(expected, result);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "978-5-4461-0960-9",
            "0-306-40615-x",
            "0 306 40615 2",
            "9785446109609",
            "030640615X",
            "   - -  "
    })
    @DisplayName("normalize идемпотентен: повторный вызов не меняет результат")
    void shouldBeIdempotentWhenNormalizing(String input) {
        String firstPass = validator.normalize(input);
        String secondPass = validator.normalize(firstPass);

        assertEquals(firstPass, secondPass);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9785446109609",
            "978-5-4461-0960-9",
            "978 5 4461 0960 9",
            "978 - 5 - 4461 - 0960 - 9",
            "0306406152",
            "0-306-40615-2",
            "0 306 40615 2",
            "030640615X",
            "0-306-40615-X",
            "030640615x",
            "0-306-40615-x"
    })
    @DisplayName("isValid возвращает true для корректных ISBN-10 и ISBN-13 в различных форматах")
    void shouldReturnTrueForValidIsbnFormats(String validIsbn) {
        boolean valid = validator.isValid(validIsbn);

        assertTrue(valid);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n", "---", " - - "})
    @DisplayName("isValid возвращает false для null, пустых строк и строк только из разделителей")
    void shouldReturnFalseForNullEmptyOrSeparatorsOnly(String invalidInput) {
        boolean valid = validator.isValid(invalidInput);

        assertFalse(valid);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123456789",
            "12345678901",
            "123456789012",
            "12345678901234",
            "123-45-678"
    })
    @DisplayName("isValid возвращает false для недопустимой длины символов")
    void shouldReturnFalseForInvalidLengths(String invalidLengthIsbn) {
        boolean valid = validator.isValid(invalidLengthIsbn);

        assertFalse(valid);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "X306406152",
            "03064X6152",
            "978544610960X",
            "978544610960x",
            "978544610X609",
            "030640615A",
            "978544610960A",
            "97854#6109609",
            "030640615Х",
            "978544610960В",
            "٩٧٨٥٤٤٦١٠٩٦٠٩"
    })
    @DisplayName("isValid возвращает false при недопустимых символах или неверной позиции 'X'")
    void shouldReturnFalseForInvalidCharactersOrPositions(String invalidIsbn) {
        boolean valid = validator.isValid(invalidIsbn);

        assertFalse(valid);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "978-5-4461-0960-9",
            "0-306-40615-x",
            "0 306 40615 2"
    })
    @DisplayName("isValid возвращает true и для исходного, и для нормализованного значения")
    void shouldBeConsistentBetweenNormalizeAndIsValid(String rawIsbn) {
        String normalized = validator.normalize(rawIsbn);

        assertTrue(validator.isValid(rawIsbn));
        assertTrue(validator.isValid(normalized));
    }

    @Test
    @DisplayName("ISBN-13 с неверной контрольной суммой считается валидным")
    void shouldReturnTrueForIsbn13WithInvalidChecksum() {
        String isbnWithWrongChecksum = "978-5-4461-0960-0";

        boolean result = validator.isValid(isbnWithWrongChecksum);

        assertTrue(result, "Валидатор должен возвращать true, так как контрольная сумма не вычисляется");
    }
}