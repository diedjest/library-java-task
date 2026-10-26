package ru.vsu.cs.uvarovdp.ui.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import ru.vsu.cs.uvarovdp.entity.Book;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование компонента BookTablePrinter")
class BookTablePrinterTest {

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

    private String getCapturedOutput() {
        return outputCaptor.toString(StandardCharsets.UTF_8);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("printBooks выводит только emptyMessage при пустом или null списке")
    void shouldPrintOnlyEmptyMessageWhenBooksEmptyOrNull(List<Book> emptyBooks) {
        BookTablePrinter printer = new BookTablePrinter();

        printer.printBooks(emptyBooks, "Каталог пуст.");

        assertEquals("Каталог пуст.", getCapturedOutput().trim());
    }

    @Test
    @DisplayName("printBooks регрессия: полные жанры, читатель, переносы, ограничение по WIDTH и отсутствие хвостовых пробелов")
    void shouldPrintBooksWithFullDetailsAndCorrectFormatting() {
        BookTablePrinter printer = new BookTablePrinter();
        Book b1 = new Book("Чистый код", "Роберт Мартин", "9785446109609",
                List.of("Программирование", "Техническая литература", "Рефакторинг"));

        Book b2 = new Book("Совершенный код", "Стив Макконнелл", "9785750200641", List.of("IT"));
        b2.borrow("Алексей Смирнов");

        String longTitle = "Очень длинное название книги превышающее сорок символов для проверки обрезки";
        String longAuthor = "Очень длинное имя автора книги которое должно обрезаться";
        Book b3 = new Book(longTitle, longAuthor, "9785699661084",
                List.of("Длинный жанр из очень большого количества слов для проверки переноса строки без потери и разрыва слов"));

        List<Book> books = List.of(b1, b2, b3);

        printer.printBooks(books, "Каталог пуст.");
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
            assertTrue(line.length() <= ConsoleOutput.WIDTH, "Строка превышает WIDTH (" + line.length() + "): " + line);
            assertEquals(line.stripTrailing(), line, "Строка содержит хвостовые пробелы: [" + line + "]");
        }
    }
}