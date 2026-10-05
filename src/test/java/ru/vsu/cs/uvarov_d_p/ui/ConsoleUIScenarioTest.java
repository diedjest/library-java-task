package ru.vsu.cs.uvarov_d_p.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.repository.impl.InMemoryBookRepository;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.service.impl.LibraryServiceImpl;
import ru.vsu.cs.uvarov_d_p.util.impl.IsbnValidatorImpl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Сквозное тестирование пользовательских сценариев ConsoleUi")
class ConsoleUIScenarioTest {

    private final InputStream standardIn = System.in;
    private final PrintStream standardOut = System.out;

    @AfterEach
    void tearDown() {
        System.setIn(standardIn);
        System.setOut(standardOut);
    }

    private String run(LibraryService service, String... inputLines) {
        String input = String.join("\n", inputLines) + "\n";
        System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream, true, StandardCharsets.UTF_8));

        new ConsoleUI(service).start();

        return outputStream.toString(StandardCharsets.UTF_8);
    }

    private LibraryService createServiceWithDemoData() {
        LibraryService service = createEmptyService();
        service.addBook(
                "Чистый код",
                "Роберт Мартин",
                "978-5-4461-0960-9",
                List.of("Программирование", "Архитектура")
        );
        Book mcconnell = service.addBook(
                "Совершенный код",
                "Стив Макконнелл",
                "978-5-7502-0064-1",
                List.of("Техническая литература")
        );
        service.borrowBook(mcconnell.getId(), "Алексей Смирнов");

        service.addBook(
                "Паттерны проектирования",
                "Эрих Гамма",
                "978-5-4461-0106-1",
                List.of("Архитектура ПО")
        );
        service.addBook(
                "Java. Эффективное программирование",
                "Джошуа Блох",
                "978-5-699-66108-4",
                List.of("Java")
        );
        return service;
    }

    private LibraryService createEmptyService() {
        return new LibraryServiceImpl(new InMemoryBookRepository(), new IsbnValidatorImpl());
    }

    private void assertNoStackTrace(String output) {
        assertFalse(output.contains("Exception"), "Вывод не должен содержать необработанных исключений");
        assertFalse(output.contains("\tat "), "Вывод не должен содержать stack trace");
        assertFalse(output.contains("✗ Непредвиденная ошибка"),
                "Ожидаемые ошибки должны выводиться как «✗ Ошибка: ...», а не как непредвиденные");
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(target, idx)) != -1) {
            count++;
            idx += target.length();
        }
        return count;
    }

    @Test
    @DisplayName("Сценарий 1: ввод '0' завершает программу с выводом 'До свидания!'")
    void scenario1_exit() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service, "0");

        assertTrue(output.contains("До свидания!"));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 2: ввод некорректных пунктов меню выводит предупреждение")
    void scenario2_invalidMenuItems() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service, "abc", "10", "", "0");

        assertEquals(3, countOccurrences(output, "⚠ Неверный пункт меню"));
        assertTrue(output.contains("До свидания!"));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 3: пункт 1 отображает книги по фамилиям авторов с полными жанрами и читателем")
    void scenario3_showAllBooks() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service, "1", "0");

        int blochIdx = output.indexOf("Блох");
        int gammaIdx = output.indexOf("Гамма");
        int mcconnellIdx = output.indexOf("Макконнелл");
        int martinIdx = output.indexOf("Мартин");

        assertTrue(blochIdx != -1 && gammaIdx != -1 && mcconnellIdx != -1 && martinIdx != -1);
        assertTrue(blochIdx < gammaIdx, "Блох должен идти перед Гамма");
        assertTrue(gammaIdx < mcconnellIdx, "Гамма должен идти перед Макконнелл");
        assertTrue(mcconnellIdx < martinIdx, "Макконнелл должен идти перед Мартин");

        assertTrue(output.contains("Программирование, Архитектура"));
        assertTrue(output.contains("Техническая литература | Читатель: Алексей Смирнов"));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 4: добавление новой книги (пункт 2) и ее отображение в общем списке (пункт 1)")
    void scenario4_addBookSuccess() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service,
                "2",
                "Кент Бек",
                "Экстремальное программирование",
                "978-5-93286-042-7",
                "Методология",
                "",
                "1",
                "0"
        );

        assertTrue(output.contains("Книга добавлена: Кент Бек — «Экстремальное программирование»"));
        assertTrue(output.contains("Экстремальное программирование"));
        assertTrue(output.contains("Кент Бек"));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 5: при ошибке ISBN повторяется только ввод ISBN; пустой ввод отменяет добавление")
    void scenario5_addBookIsbnRetryAndCancel() {
        LibraryService service = createServiceWithDemoData();

        String outputSuccess = run(service,
                "2",
                "Мартин Фаулер",
                "Рефакторинг",
                "123",
                "978-5-4461-0960-9",
                "978-5-93286-042-7",
                "Рефакторинг",
                "",
                "0"
        );

        assertEquals(1, countOccurrences(outputSuccess, "Введите автора: "));
        assertEquals(1, countOccurrences(outputSuccess, "Введите название: "));
        assertTrue(outputSuccess.contains("Ошибка: Некорректный формат ISBN"));
        assertTrue(outputSuccess.contains("Ошибка: Книга с ISBN 9785446109609 уже есть в каталоге"));
        assertTrue(outputSuccess.contains("Книга добавлена: Мартин Фаулер — «Рефакторинг»"));
        assertNoStackTrace(outputSuccess);

        String outputCancel = run(service,
                "2",
                "Герберт Шилдт",
                "Java. Полное руководство",
                "",
                "0"
        );

        assertTrue(outputCancel.contains("Добавление отменено"));
        assertEquals(5, service.getAllBooks().size());
        assertNoStackTrace(outputCancel);
    }

    @Test
    @DisplayName("Сценарий 6: исправление ошибок добавления книги при подтверждении и отмена при отказе")
    void scenario6_addBookGenreRetryAndDecline() {
        LibraryService service = createServiceWithDemoData();

        String outputRetry = run(service,
                "2",
                "Автор Тест",
                "Название Тест",
                "978-5-93286-042-7",
                "IT",
                "it",
                "",
                "да",
                "",
                "",
                "",
                "IT",
                "",
                "0"
        );

        assertTrue(outputRetry.contains("Ошибка: Жанры не должны повторяться: it"));
        assertTrue(outputRetry.contains("Книга добавлена: Автор Тест — «Название Тест»"));
        assertNoStackTrace(outputRetry);

        String outputDecline = run(service,
                "2",
                "Автор Отмена",
                "Название Отмена",
                "978-5-93286-043-4",
                "IT",
                "it",
                "",
                "нет",
                "0"
        );

        assertTrue(outputDecline.contains("Ошибка: Жанры не должны повторяться: it"));
        assertTrue(outputDecline.contains("Добавление отменено"));
        assertNoStackTrace(outputDecline);
    }

    @Test
    @DisplayName("Сценарий 7: редактирование книги с сохранением значений по Enter, валидацией ISBN и сменой названия")
    void scenario7_editBook() {
        LibraryService service = createServiceWithDemoData();

        String outputNoChange = run(service, "3", "1", "", "", "", "", "0");
        assertTrue(outputNoChange.contains("Книга обновлена"));
        Book unchanged = service.getAllBooks().get(0);
        assertEquals("Java. Эффективное программирование", unchanged.getTitle());
        assertEquals("9785699661084", unchanged.getIsbn());
        assertEquals(List.of("Java"), unchanged.getGenres(), "Enter на жанрах должен сохранить прежние жанры");
        assertNoStackTrace(outputNoChange);

        String outputChangeTitle = run(service, "3", "1", "", "Java 21. Новые горизонты", "", "", "0");
        assertTrue(outputChangeTitle.contains("Книга обновлена"));
        assertEquals("Java 21. Новые горизонты", service.getAllBooks().get(0).getTitle());
        assertNoStackTrace(outputChangeTitle);

        String outputIsbnCheck = run(service,
                "3",
                "1",
                "",
                "",
                "978-5-4461-0960-9",
                "978-5-699-66108-4",
                "",
                "0"
        );

        assertTrue(outputIsbnCheck.contains("Ошибка: Книга с ISBN 9785446109609 уже есть в каталоге"));
        assertTrue(outputIsbnCheck.contains("Книга обновлена"));
        assertNoStackTrace(outputIsbnCheck);
    }

    @Test
    @DisplayName("Сценарий 8: удаление книги с подтверждением, запретом для выданных и отменой")
    void scenario8_deleteBook() {
        LibraryService service = createServiceWithDemoData();

        String outputCancel = run(service, "4", "1", "нет", "0");
        assertTrue(outputCancel.contains("Удаление отменено"));
        assertEquals(4, service.getAllBooks().size());
        assertNoStackTrace(outputCancel);

        String outputBorrowed = run(service, "4", "3", "да", "0");
        assertTrue(outputBorrowed.contains("Ошибка: Нельзя удалить выданную книгу"));
        assertEquals(4, service.getAllBooks().size());
        assertNoStackTrace(outputBorrowed);

        String outputSuccess = run(service, "4", "1", "да", "0");
        assertTrue(outputSuccess.contains("Книга удалена"));
        assertEquals(3, service.getAllBooks().size());
        assertFalse(service.getAllBooks().stream().anyMatch(b -> b.getAuthor().contains("Блох")));
        assertNoStackTrace(outputSuccess);
    }

    @Test
    @DisplayName("Сценарий 9: поиск книг по автору (пункт 5) и по ISBN (пункт 6)")
    void scenario9_searchByAuthorAndIsbn() {
        LibraryService service = createServiceWithDemoData();

        String outputAuthor = run(service,
                "5", "март",
                "5", "РОБЕРТ",
                "5", "Толстой",
                "0"
        );

        assertTrue(outputAuthor.contains("Роберт Мартин"));
        assertTrue(outputAuthor.contains("Ничего не найдено."));
        assertNoStackTrace(outputAuthor);

        String outputIsbn = run(service,
                "6", "978-5-4461-0960-9",
                "6", "9785446109609",
                "6", "bad-isbn",
                "6", "978-5-0000-0000-0",
                "0"
        );

        assertTrue(outputIsbn.contains("Чистый код"));
        assertTrue(outputIsbn.contains("✗ Ошибка: Некорректный формат ISBN"));
        assertTrue(outputIsbn.contains("Ничего не найдено."));
        assertNoStackTrace(outputIsbn);
    }

    @Test
    @DisplayName("Сценарий 10: добавление жанра (пункт 7), обработка дубликата и достижение лимита 3/3")
    void scenario10_addGenre() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service,
                "7", "1", "JVM",
                "7", "1", "jvm",
                "7", "1", "Архитектура",
                "7", "1",
                "0"
        );

        assertTrue(output.contains("Жанр добавлен. Текущие жанры: Java, JVM"));
        assertTrue(output.contains("Ошибка: Жанр 'jvm' уже добавлен к книге"));
        assertTrue(output.contains("Жанр добавлен. Текущие жанры: Java, JVM, Архитектура"));
        assertTrue(output.contains("Достигнут лимит жанров для этой книги."));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 11: выдача (пункт 8) и возврат книги (пункт 9) с подтверждением и проверкой итогового состояния")
    void scenario11_borrowAndReturn() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service,
                "8", "1", "\n   \n", "Иван Иванов",
                "9", "1", "нет",
                "9", "1", "да",
                "0"
        );

        assertTrue(output.contains("Значение не может быть пустым. Попробуйте снова."));
        assertTrue(output.contains("Книга выдана: Джошуа Блох — «Java. Эффективное программирование» читателю Иван Иванов"));
        assertTrue(output.contains("Возврат отменён"));
        assertTrue(output.contains("Книга возвращена в каталог"));
        assertNoStackTrace(output);

        Book bloch = service.getAllBooks().get(0);
        assertTrue(bloch.getAuthor().contains("Блох"));
        assertTrue(bloch.isAvailable(), "После возврата книга Блоха должна быть в наличии");
        assertNull(bloch.getBorrowerName());
        assertFalse(service.getAllBooks().get(2).isAvailable(), "Книга Макконнелла должна остаться выданной");
    }

    @Test
    @DisplayName("Сценарий 11б: список выдачи содержит только книги в наличии, список возврата — только выданные")
    void scenario11b_borrowAndReturnListsAreFiltered() {
        LibraryService service = createServiceWithDemoData();

        String borrowOutput = run(service, "8", "99", "0");

        assertTrue(borrowOutput.contains("Номер книги вне допустимого диапазона (1-3)"),
                "В списке выдачи должны быть только 3 книги в наличии");
        assertTrue(borrowOutput.contains("Чистый код"));
        assertFalse(borrowOutput.contains("Совершенный код"), "Выданная книга не должна предлагаться к выдаче");

        String returnOutput = run(service, "9", "99", "0");

        assertTrue(returnOutput.contains("Номер книги вне допустимого диапазона (1-1)"),
                "В списке возврата должна быть только 1 выданная книга");
        assertTrue(returnOutput.contains("Совершенный код"));
        assertFalse(returnOutput.contains("Чистый код"), "Книга в наличии не должна предлагаться к возврату");
    }

    @Test
    @DisplayName("Сценарий 12: операции на пустом каталоге выводят соответствующие сообщения")
    void scenario12_emptyCatalog() {
        LibraryService service = createEmptyService();

        String output = run(service, "1", "3", "4", "7", "8", "9", "0");

        assertEquals(4, countOccurrences(output, "Каталог пуст."));
        assertTrue(output.contains("Нет книг в наличии."));
        assertTrue(output.contains("Нет выданных книг."));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 13: неверный номер книги в списках не приводит к падению")
    void scenario13_invalidBookSelection() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service,
                "3", "abc",
                "3", "99",
                "3", "0",
                "0"
        );

        assertTrue(output.contains("Введено некорректное число"));
        assertTrue(output.contains("Номер книги вне допустимого диапазона (1-4)"));
        assertTrue(output.contains("Выход..."));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 14: неожиданное завершение ввода (EOF) не ломает JVM и выводит прощание")
    void scenario14_inputClosedMidOperation() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service, "2", "Роберт Мартин");

        assertTrue(output.contains("Ввод завершён. Выход..."));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 15: непредвиденная ошибка без сообщения корректно описывается через имя класса")
    void scenario15_unexpectedExceptionHandling() {
        LibraryService proxyService = (LibraryService) Proxy.newProxyInstance(
                LibraryService.class.getClassLoader(),
                new Class<?>[]{LibraryService.class},
                (proxy, method, args) -> {
                    if ("getAllBooks".equals(method.getName())) {
                        throw new RuntimeException();
                    }
                    return null;
                }
        );

        String output = run(proxyService, "1", "0");

        assertTrue(output.contains("Непредвиденная ошибка: RuntimeException"));
        assertTrue(output.contains("Выход..."));
        assertFalse(output.contains("\tat "));
    }
}