package ru.vsu.cs.uvarovdp.ui.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.ex.InputClosedException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование компонента ConsoleInput")
class ConsoleInputTest {

    private final PrintStream standardOut = System.out;
    private ByteArrayOutputStream outputCaptor;

    @BeforeEach
    void setUp() {
        outputCaptor = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputCaptor, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(standardOut);
    }

    private ConsoleInput createInput(String input) {
        return new ConsoleInput(new Scanner(input));
    }

    private String getCapturedOutput() {
        return outputCaptor.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("readLine выводит prompt и возвращает введенную строку")
    void shouldPrintPromptAndReturnLine() {
        ConsoleInput input = createInput("Тестовая строка\n");

        String result = input.readLine("Введите данные: ");

        assertEquals("Тестовая строка", result);
        assertEquals("Введите данные: ", getCapturedOutput());
    }

    @Test
    @DisplayName("readLine бросает InputClosedException при исчерпании потока и ничего не печатает")
    void shouldThrowInputClosedExceptionOnExhaustedInputWithoutPrinting() {
        ConsoleInput input = createInput("");

        assertThrows(InputClosedException.class, () -> input.readLine(null));
        assertEquals("", getCapturedOutput());
    }

    @Test
    @DisplayName("readNonBlank пропускает пустые строки с предупреждением и возвращает обрезанное значение")
    void shouldSkipBlankLinesWithWarningAndReturnTrimmedValue() {
        ConsoleInput input = createInput("\n   \n\t\n  Успешный ввод  \n");

        String result = input.readNonBlank("Введите текст: ");

        assertEquals("Успешный ввод", result);
        assertTrue(getCapturedOutput().contains("Значение не может быть пустым. Попробуйте снова."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "   \n", "\t\n"})
    @DisplayName("readWithDefault возвращает значение по умолчанию при пустом вводе")
    void shouldReturnDefaultValueOnEmptyInput(String text) {
        ConsoleInput input = createInput(text);

        String result = input.readWithDefault("Поле: ", "ЗначениеПоУмолчанию");

        assertEquals("ЗначениеПоУмолчанию", result);
    }

    @Test
    @DisplayName("readWithDefault возвращает trim-значение при непустом вводе")
    void shouldReturnTrimmedInputWhenNotEmpty() {
        ConsoleInput input = createInput("  Новое значение  \n");

        String result = input.readWithDefault("Поле: ", "ЗначениеПоУмолчанию");

        assertEquals("Новое значение", result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"да", "Д", "yes", "Y", " да ", "  YES  "})
    @DisplayName("confirm возвращает true для утвердительных вариантов ответа")
    void shouldReturnTrueForAffirmativeConfirmAnswers(String answer) {
        ConsoleInput input = createInput(answer + "\n");

        assertTrue(input.confirm("Подтвердить? "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"нет", "n", "", "   ", "ага", "произвольный текст"})
    @DisplayName("confirm возвращает false для отрицательных или некорректных ответов")
    void shouldReturnFalseForNegativeOrUnknownConfirmAnswers(String answer) {
        ConsoleInput input = createInput(answer + "\n");

        assertFalse(input.confirm("Подтвердить? "));
    }

    @Test
    @DisplayName("readGenres считывает один жанр при пустом втором вводе")
    void shouldReadSingleGenreWhenSecondIsBlank() {
        ConsoleInput input = createInput("Детектив\n\n");

        List<String> genres = input.readGenres();

        assertEquals(List.of("Детектив"), genres);
    }

    @Test
    @DisplayName("readGenres считывает максимум три жанра и не запрашивает четвертый")
    void shouldReadUpToThreeGenresWithoutAskingForFourth() {
        ConsoleInput input = createInput("Жанр 1\nЖанр 2\nЖанр 3\n");

        List<String> genres = input.readGenres();

        assertEquals(List.of("Жанр 1", "Жанр 2", "Жанр 3"), genres);
        assertFalse(getCapturedOutput().contains("Жанр 4"));
    }

    @Test
    @DisplayName("readGenres повторяет запрос первого обязательного жанра при пустом вводе")
    void shouldRepeatFirstGenrePromptUntilNonBlank() {
        ConsoleInput input = createInput("\n   \nФантастика\n\n");

        List<String> genres = input.readGenres();

        assertEquals(List.of("Фантастика"), genres);
        assertTrue(getCapturedOutput().contains("Значение не может быть пустым. Попробуйте снова."));
    }

    @Test
    @DisplayName("selectBookId возвращает идентификатор книги при корректном номере")
    void shouldReturnBookIdOnValidIndex() {
        Book b1 = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        Book b2 = new Book("Совершенный код", "Стив Макконнелл", "9785750200641", List.of("IT"));
        List<Book> books = List.of(b1, b2);
        ConsoleInput input = createInput("  2  \n");

        UUID selectedId = input.selectBookId(books, "Выберите: ");

        assertEquals(b2.getId(), selectedId);
    }

    @Test
    @DisplayName("selectBookId возвращает null и предупреждение при нечисловом вводе")
    void shouldReturnNullAndWarnOnNonNumericInput() {
        Book b1 = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        ConsoleInput input = createInput("abc\n");

        UUID selectedId = input.selectBookId(List.of(b1), "Выберите: ");

        assertNull(selectedId);
        assertTrue(getCapturedOutput().contains("Введено некорректное число"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "3", "99"})
    @DisplayName("selectBookId возвращает null и сообщение с диапазоном при номере вне границ")
    void shouldReturnNullAndWarnOnOutOfRangeIndex(String invalidIndex) {
        Book b1 = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        Book b2 = new Book("Совершенный код", "Стив Макконнелл", "9785750200641", List.of("IT"));
        List<Book> books = List.of(b1, b2);
        ConsoleInput input = createInput(invalidIndex + "\n");

        UUID selectedId = input.selectBookId(books, "Выберите: ");

        assertNull(selectedId);
        assertTrue(getCapturedOutput().contains("Номер книги вне допустимого диапазона (1-2)"));
    }
}
