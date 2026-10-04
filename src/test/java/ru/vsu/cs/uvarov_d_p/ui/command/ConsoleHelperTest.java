package ru.vsu.cs.uvarov_d_p.ui.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.ex.InputClosedException;

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

@DisplayName("Тестирование компонента ConsoleHelper")
class ConsoleHelperTest {

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

    private ConsoleHelper createHelper(String input) {
        return new ConsoleHelper(new Scanner(input));
    }

    private String getCapturedOutput() {
        return outputCaptor.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("readLine выводит prompt и возвращает введенную строку")
    void shouldPrintPromptAndReturnLine() {
        ConsoleHelper helper = createHelper("Тестовая строка\n");

        String result = helper.readLine("Введите данные: ");

        assertEquals("Тестовая строка", result);
        assertEquals("Введите данные: ", getCapturedOutput());
    }

    @Test
    @DisplayName("readLine бросает InputClosedException при исчерпании потока и ничего не печатает")
    void shouldThrowInputClosedExceptionOnExhaustedInputWithoutPrinting() {
        ConsoleHelper helper = createHelper("");

        assertThrows(InputClosedException.class, () -> helper.readLine(null));
        assertEquals("", getCapturedOutput());
    }

    @Test
    @DisplayName("readNonBlank пропускает пустые строки с предупреждением и возвращает обрезанное значение")
    void shouldSkipBlankLinesWithWarningAndReturnTrimmedValue() {
        ConsoleHelper helper = createHelper("\n   \n\t\n  Успешный ввод  \n");

        String result = helper.readNonBlank("Введите текст: ");

        assertEquals("Успешный ввод", result);
        assertTrue(getCapturedOutput().contains("⚠ Значение не может быть пустым. Попробуйте снова."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "   \n", "\t\n"})
    @DisplayName("readWithDefault возвращает значение по умолчанию при пустом вводе")
    void shouldReturnDefaultValueOnEmptyInput(String input) {
        ConsoleHelper helper = createHelper(input);

        String result = helper.readWithDefault("Поле: ", "ЗначениеПоУмолчанию");

        assertEquals("ЗначениеПоУмолчанию", result);
    }

    @Test
    @DisplayName("readWithDefault возвращает trim-значение при непустом вводе")
    void shouldReturnTrimmedInputWhenNotEmpty() {
        ConsoleHelper helper = createHelper("  Новое значение  \n");

        String result = helper.readWithDefault("Поле: ", "ЗначениеПоУмолчанию");

        assertEquals("Новое значение", result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"да", "Д", "yes", "Y", " да ", "  YES  "})
    @DisplayName("confirm возвращает true для утвердительных вариантов ответа")
    void shouldReturnTrueForAffirmativeConfirmAnswers(String answer) {
        ConsoleHelper helper = createHelper(answer + "\n");

        assertTrue(helper.confirm("Подтвердить? "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"нет", "n", "", "   ", "ага", "произвольный текст"})
    @DisplayName("confirm возвращает false для отрицательных или некорректных ответов")
    void shouldReturnFalseForNegativeOrUnknownConfirmAnswers(String answer) {
        ConsoleHelper helper = createHelper(answer + "\n");

        assertFalse(helper.confirm("Подтвердить? "));
    }

    @Test
    @DisplayName("readGenres считывает один жанр при пустом втором вводе")
    void shouldReadSingleGenreWhenSecondIsBlank() {
        ConsoleHelper helper = createHelper("Детектив\n\n");

        List<String> genres = helper.readGenres();

        assertEquals(List.of("Детектив"), genres);
    }

    @Test
    @DisplayName("readGenres считывает максимум три жанра и не запрашивает четвертый")
    void shouldReadUpToThreeGenresWithoutAskingForFourth() {
        ConsoleHelper helper = createHelper("Жанр 1\nЖанр 2\nЖанр 3\n");

        List<String> genres = helper.readGenres();

        assertEquals(List.of("Жанр 1", "Жанр 2", "Жанр 3"), genres);
        assertFalse(getCapturedOutput().contains("Жанр 4"));
    }

    @Test
    @DisplayName("readGenres повторяет запрос первого обязательного жанра при пустом вводе")
    void shouldRepeatFirstGenrePromptUntilNonBlank() {
        ConsoleHelper helper = createHelper("\n   \nФантастика\n\n");

        List<String> genres = helper.readGenres();

        assertEquals(List.of("Фантастика"), genres);
        assertTrue(getCapturedOutput().contains("⚠ Значение не может быть пустым. Попробуйте снова."));
    }

    @Test
    @DisplayName("selectBookId возвращает идентификатор книги при корректном номере")
    void shouldReturnBookIdOnValidIndex() {
        Book b1 = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        Book b2 = new Book("Совершенный код", "Стив Макконнелл", "9785750200641", List.of("IT"));
        List<Book> books = List.of(b1, b2);
        ConsoleHelper helper = createHelper("  2  \n");

        UUID selectedId = helper.selectBookId(books, "Выберите: ");

        assertEquals(b2.getId(), selectedId);
    }

    @Test
    @DisplayName("selectBookId возвращает null и предупреждение при нечисловом вводе")
    void shouldReturnNullAndWarnOnNonNumericInput() {
        Book b1 = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        ConsoleHelper helper = createHelper("abc\n");

        UUID selectedId = helper.selectBookId(List.of(b1), "Выберите: ");

        assertNull(selectedId);
        assertTrue(getCapturedOutput().contains("⚠ Введено некорректное число"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "3", "99"})
    @DisplayName("selectBookId возвращает null и сообщение с диапазоном при номере вне границ")
    void shouldReturnNullAndWarnOnOutOfRangeIndex(String invalidIndex) {
        Book b1 = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        Book b2 = new Book("Совершенный код", "Стив Макконнелл", "9785750200641", List.of("IT"));
        List<Book> books = List.of(b1, b2);
        ConsoleHelper helper = createHelper(invalidIndex + "\n");

        UUID selectedId = helper.selectBookId(books, "Выберите: ");

        assertNull(selectedId);
        assertTrue(getCapturedOutput().contains("⚠ Номер книги вне допустимого диапазона (1-2)"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("printBooks выводит только emptyMessage при пустом или null списке")
    void shouldPrintOnlyEmptyMessageWhenBooksEmptyOrNull(List<Book> emptyBooks) {
        ConsoleHelper helper = createHelper("");

        helper.printBooks(emptyBooks, "Каталог пуст.");

        assertEquals("Каталог пуст.", getCapturedOutput().trim());
    }

    @Test
    @DisplayName("printBooks регрессия: полные жанры, читатель, переносы, ограничение по WIDTH и отсутствие хвостовых пробелов")
    void shouldPrintBooksWithFullDetailsAndCorrectFormatting() {
        ConsoleHelper helper = createHelper("");
        Book b1 = new Book("Чистый код", "Роберт Мартин", "9785446109609",
                List.of("Программирование", "Техническая литература", "Рефакторинг"));

        Book b2 = new Book("Совершенный код", "Стив Макконнелл", "9785750200641", List.of("IT"));
        b2.borrow("Алексей Смирнов");

        String longTitle = "Очень длинное название книги превышающее сорок символов для проверки обрезки";
        String longAuthor = "Очень длинное имя автора книги которое должно обрезаться";
        Book b3 = new Book(longTitle, longAuthor, "9785699661084",
                List.of("Длинный жанр из очень большого количества слов для проверки переноса строки без потери и разрыва слов"));

        List<Book> books = List.of(b1, b2, b3);

        helper.printBooks(books, "Каталог пуст.");
        String output = getCapturedOutput();

        assertTrue(output.contains("Программирование"));
        assertTrue(output.contains("Техническая литература"));
        assertTrue(output.contains("Рефакторинг"));

        assertTrue(output.contains("Алексей Смирнов"));

        assertTrue(output.contains("В наличии"));
        assertTrue(output.contains("Выдана"));

        assertTrue(output.contains("…"));

        assertTrue(output.contains("Длинный"));
        assertTrue(output.contains("количества"));
        assertTrue(output.contains("переноса"));

        String[] lines = output.split("\\R");
        for (String line : lines) {
            assertTrue(line.length() <= ConsoleHelper.WIDTH, "Строка превышает WIDTH (" + line.length() + "): " + line);
            assertEquals(line.stripTrailing(), line, "Строка содержит хвостовые пробелы: [" + line + "]");
        }
    }
}