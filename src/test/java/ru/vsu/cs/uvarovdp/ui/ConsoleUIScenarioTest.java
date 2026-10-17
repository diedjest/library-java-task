package ru.vsu.cs.uvarovdp.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.entity.BookStatus;
import ru.vsu.cs.uvarovdp.ex.BusinessRuleException;
import ru.vsu.cs.uvarovdp.ex.BusinessRuleType;
import ru.vsu.cs.uvarovdp.ex.ValidationException;
import ru.vsu.cs.uvarovdp.service.LibraryService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Сквозное тестирование пользовательских сценариев ConsoleUi")
class ConsoleUIScenarioTest {

    private final InputStream standardIn = System.in;
    private final PrintStream standardOut = System.out;

    private Book bloch;
    private Book gamma;
    private Book mcconnell;
    private Book martin;

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

    private Book book(UUID id, String title, String author, String isbn, List<String> genres,
                      BookStatus status, String borrowerName) {
        LocalDateTime now = LocalDateTime.now();
        return new Book(id, title, author, isbn, genres, status, borrowerName, now, now);
    }

    private Book availableBook(String title, String author, String isbn, List<String> genres) {
        return book(UUID.randomUUID(), title, author, isbn, genres, BookStatus.AVAILABLE, null);
    }

    private LibraryService createServiceWithDemoData() {
        bloch = availableBook("Java. Эффективное программирование", "Джошуа Блох", "9785699661084", List.of("Java"));
        gamma = availableBook("Паттерны проектирования", "Эрих Гамма", "9785446101061", List.of("Архитектура ПО"));
        mcconnell = book(UUID.randomUUID(), "Совершенный код", "Стив Макконнелл", "9785750200641",
                List.of("Техническая литература"), BookStatus.BORROWED, "Алексей Смирнов");
        martin = availableBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("Программирование", "Архитектура"));

        LibraryService service = createEmptyService();
        when(service.getAllBooks()).thenReturn(List.of(bloch, gamma, mcconnell, martin));
        for (Book demoBook : List.of(bloch, gamma, mcconnell, martin)) {
            when(service.getBookById(demoBook.getId())).thenReturn(demoBook);
        }
        return service;
    }

    private LibraryService createEmptyService() {
        return mock(LibraryService.class);
    }

    private void assertNoStackTrace(String output) {
        assertFalse(output.contains("Exception"), "Вывод не должен содержать необработанных исключений");
        assertFalse(output.contains("\tat "), "Вывод не должен содержать stack trace");
        assertFalse(output.contains("Непредвиденная ошибка"),
                "Ожидаемые ошибки должны выводиться как «Ошибка: ...», а не как непредвиденные");
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
    @DisplayName("Сценарий 1: ввод 'exit' завершает программу с выводом 'Выход...'")
    void scenario1_exit() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service, "exit");

        assertTrue(output.contains("Выход..."));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 2: ввод некорректных пунктов меню выводит предупреждение")
    void scenario2_invalidMenuItems() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service, "abc", "10", "", "exit");

        assertEquals(3, countOccurrences(output, "Неверный пункт меню"));
        assertTrue(output.contains("Выход..."));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 3: пункт show отображает книги в порядке сервиса с полными жанрами и читателем")
    void scenario3_showAllBooks() {
        LibraryService service = createServiceWithDemoData();

        String output = run(service, "show", "exit");

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
    @DisplayName("Сценарий 4: добавление новой книги (пункт add) передает введенные данные в сервис")
    void scenario4_addBookSuccess() {
        LibraryService service = createServiceWithDemoData();
        when(service.addBook("Экстремальное программирование", "Кент Бек", "978-5-93286-042-7", List.of("Методология")))
                .thenReturn(availableBook("Экстремальное программирование", "Кент Бек", "9785932860427",
                        List.of("Методология")));

        String output = run(service,
                "add",
                "Кент Бек",
                "Экстремальное программирование",
                "978-5-93286-042-7",
                "Методология",
                "",
                "exit"
        );

        assertTrue(output.contains("Книга добавлена: Кент Бек — «Экстремальное программирование»"));
        verify(service).addBook("Экстремальное программирование", "Кент Бек", "978-5-93286-042-7",
                List.of("Методология"));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 5: при ошибке ISBN повторяется только ввод ISBN; пустой ввод отменяет добавление")
    void scenario5_addBookIsbnRetryAndCancel() {
        LibraryService service = createServiceWithDemoData();
        doThrow(new ValidationException("Некорректный формат ISBN"))
                .when(service).checkIsbnAvailable("123", null);
        doThrow(new BusinessRuleException(BusinessRuleType.DUPLICATE_ISBN,
                "Книга с ISBN 9785446109609 уже есть в каталоге"))
                .when(service).checkIsbnAvailable("978-5-4461-0960-9", null);
        when(service.addBook("Рефакторинг", "Мартин Фаулер", "978-5-93286-042-7", List.of("Рефакторинг")))
                .thenReturn(availableBook("Рефакторинг", "Мартин Фаулер", "9785932860427", List.of("Рефакторинг")));

        String outputSuccess = run(service,
                "add",
                "Мартин Фаулер",
                "Рефакторинг",
                "123",
                "978-5-4461-0960-9",
                "978-5-93286-042-7",
                "Рефакторинг",
                "",
                "exit"
        );

        assertEquals(1, countOccurrences(outputSuccess, "Введите автора: "));
        assertEquals(1, countOccurrences(outputSuccess, "Введите название: "));
        assertTrue(outputSuccess.contains("Ошибка: Некорректный формат ISBN"));
        assertTrue(outputSuccess.contains("Ошибка: Книга с ISBN 9785446109609 уже есть в каталоге"));
        assertTrue(outputSuccess.contains("Книга добавлена: Мартин Фаулер — «Рефакторинг»"));
        assertNoStackTrace(outputSuccess);

        LibraryService cancelService = createServiceWithDemoData();
        String outputCancel = run(cancelService,
                "add",
                "Герберт Шилдт",
                "Java. Полное руководство",
                "",
                "exit"
        );

        assertTrue(outputCancel.contains("Добавление отменено"));
        verify(cancelService, never()).addBook(any(), any(), any(), any());
        assertNoStackTrace(outputCancel);
    }

    @Test
    @DisplayName("Сценарий 6: исправление ошибок добавления книги при подтверждении и отмена при отказе")
    void scenario6_addBookGenreRetryAndDecline() {
        LibraryService service = createServiceWithDemoData();
        when(service.addBook("Название Тест", "Автор Тест", "978-5-93286-042-7", List.of("IT", "it")))
                .thenThrow(new ValidationException("Жанры не должны повторяться: it"));
        when(service.addBook("Название Тест", "Автор Тест", "978-5-93286-042-7", List.of("IT")))
                .thenReturn(availableBook("Название Тест", "Автор Тест", "9785932860427", List.of("IT")));

        String outputRetry = run(service,
                "add",
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
                "exit"
        );

        assertTrue(outputRetry.contains("Ошибка: Жанры не должны повторяться: it"));
        assertTrue(outputRetry.contains("Книга добавлена: Автор Тест — «Название Тест»"));
        assertNoStackTrace(outputRetry);

        LibraryService declineService = createServiceWithDemoData();
        when(declineService.addBook("Название Отмена", "Автор Отмена", "978-5-93286-043-4", List.of("IT", "it")))
                .thenThrow(new ValidationException("Жанры не должны повторяться: it"));

        String outputDecline = run(declineService,
                "add",
                "Автор Отмена",
                "Название Отмена",
                "978-5-93286-043-4",
                "IT",
                "it",
                "",
                "нет",
                "exit"
        );

        assertTrue(outputDecline.contains("Ошибка: Жанры не должны повторяться: it"));
        assertTrue(outputDecline.contains("Добавление отменено"));
        assertNoStackTrace(outputDecline);
    }

    @Test
    @DisplayName("Сценарий 7: редактирование книги с сохранением значений по Enter, валидацией ISBN и сменой названия")
    void scenario7_editBook() {
        LibraryService noChangeService = createServiceWithDemoData();
        String outputNoChange = run(noChangeService, "edit", "1", "", "", "", "", "exit");
        assertTrue(outputNoChange.contains("Книга обновлена"));
        verify(noChangeService).editBook(bloch.getId(), "Java. Эффективное программирование", "Джошуа Блох",
                "9785699661084", List.of("Java"));
        assertNoStackTrace(outputNoChange);

        LibraryService titleService = createServiceWithDemoData();
        String outputChangeTitle = run(titleService, "edit", "1", "", "Java 21. Новые горизонты", "", "", "exit");
        assertTrue(outputChangeTitle.contains("Книга обновлена"));
        verify(titleService).editBook(bloch.getId(), "Java 21. Новые горизонты", "Джошуа Блох",
                "9785699661084", List.of("Java"));
        assertNoStackTrace(outputChangeTitle);

        LibraryService isbnService = createServiceWithDemoData();
        doThrow(new BusinessRuleException(BusinessRuleType.DUPLICATE_ISBN,
                "Книга с ISBN 9785446109609 уже есть в каталоге"))
                .when(isbnService).checkIsbnAvailable("978-5-4461-0960-9", bloch.getId());
        String outputIsbnCheck = run(isbnService,
                "edit",
                "1",
                "",
                "",
                "978-5-4461-0960-9",
                "978-5-699-66108-4",
                "",
                "exit"
        );

        assertTrue(outputIsbnCheck.contains("Ошибка: Книга с ISBN 9785446109609 уже есть в каталоге"));
        assertTrue(outputIsbnCheck.contains("Книга обновлена"));
        verify(isbnService).editBook(bloch.getId(), "Java. Эффективное программирование", "Джошуа Блох",
                "978-5-699-66108-4", List.of("Java"));
        assertNoStackTrace(outputIsbnCheck);
    }

    @Test
    @DisplayName("Сценарий 8: удаление книги с подтверждением, запретом для выданных и отменой")
    void scenario8_deleteBook() {
        LibraryService cancelService = createServiceWithDemoData();
        String outputCancel = run(cancelService, "delete", "1", "нет", "exit");
        assertTrue(outputCancel.contains("Удаление отменено"));
        verify(cancelService, never()).deleteBook(any());
        assertNoStackTrace(outputCancel);

        LibraryService borrowedService = createServiceWithDemoData();
        doThrow(new BusinessRuleException(BusinessRuleType.DELETE_BORROWED_BOOK, "Нельзя удалить выданную книгу"))
                .when(borrowedService).deleteBook(mcconnell.getId());
        String outputBorrowed = run(borrowedService, "delete", "3", "да", "exit");
        assertTrue(outputBorrowed.contains("Ошибка: Нельзя удалить выданную книгу"));
        assertNoStackTrace(outputBorrowed);

        LibraryService successService = createServiceWithDemoData();
        String outputSuccess = run(successService, "delete", "1", "да", "exit");
        assertTrue(outputSuccess.contains("Книга удалена"));
        verify(successService).deleteBook(bloch.getId());
        assertNoStackTrace(outputSuccess);
    }

    @Test
    @DisplayName("Сценарий 9: поиск книг по автору (пункт author) и по ISBN (пункт isbn)")
    void scenario9_searchByAuthorAndIsbn() {
        LibraryService service = createServiceWithDemoData();
        when(service.searchByAuthor("март")).thenReturn(List.of(martin));
        when(service.searchByAuthor("РОБЕРТ")).thenReturn(List.of(martin));

        String outputAuthor = run(service,
                "author", "март",
                "author", "РОБЕРТ",
                "author", "Толстой",
                "exit"
        );

        assertTrue(outputAuthor.contains("Роберт Мартин"));
        assertTrue(outputAuthor.contains("Ничего не найдено."));
        assertNoStackTrace(outputAuthor);

        when(service.searchByIsbn("978-5-4461-0960-9")).thenReturn(Optional.of(martin));
        when(service.searchByIsbn("9785446109609")).thenReturn(Optional.of(martin));
        when(service.searchByIsbn("bad-isbn")).thenThrow(new ValidationException("Некорректный формат ISBN"));

        String outputIsbn = run(service,
                "isbn", "978-5-4461-0960-9",
                "isbn", "9785446109609",
                "isbn", "bad-isbn",
                "isbn", "978-5-0000-0000-0",
                "exit"
        );

        assertTrue(outputIsbn.contains("Чистый код"));
        assertTrue(outputIsbn.contains("Ошибка: Некорректный формат ISBN"));
        assertTrue(outputIsbn.contains("Ничего не найдено."));
        assertNoStackTrace(outputIsbn);
    }

    @Test
    @DisplayName("Сценарий 10: добавление жанра (пункт genre), обработка дубликата и достижение лимита 3/3")
    void scenario10_addGenre() {
        LibraryService service = createServiceWithDemoData();
        UUID id = bloch.getId();
        Book withJvm = book(id, bloch.getTitle(), bloch.getAuthor(), bloch.getIsbn(),
                List.of("Java", "JVM"), BookStatus.AVAILABLE, null);
        Book full = book(id, bloch.getTitle(), bloch.getAuthor(), bloch.getIsbn(),
                List.of("Java", "JVM", "Архитектура"), BookStatus.AVAILABLE, null);
        when(service.getBookById(id)).thenReturn(bloch, withJvm, withJvm, full);
        when(service.addGenre(id, "JVM")).thenReturn(withJvm);
        when(service.addGenre(id, "jvm")).thenThrow(new ValidationException("Жанр 'jvm' уже добавлен к книге"));
        when(service.addGenre(id, "Архитектура")).thenReturn(full);

        String output = run(service,
                "genre", "1", "JVM",
                "genre", "1", "jvm",
                "genre", "1", "Архитектура",
                "genre", "1",
                "exit"
        );

        assertTrue(output.contains("Жанр добавлен. Текущие жанры: Java, JVM"));
        assertTrue(output.contains("Ошибка: Жанр 'jvm' уже добавлен к книге"));
        assertTrue(output.contains("Жанр добавлен. Текущие жанры: Java, JVM, Архитектура"));
        assertTrue(output.contains("Достигнут лимит жанров для этой книги."));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 11: выдача (пункт borrow) и возврат книги (пункт return) с подтверждением")
    void scenario11_borrowAndReturn() {
        LibraryService service = createServiceWithDemoData();
        UUID id = bloch.getId();
        Book borrowedBloch = book(id, bloch.getTitle(), bloch.getAuthor(), bloch.getIsbn(),
                bloch.getGenres(), BookStatus.BORROWED, "Иван Иванов");
        when(service.getAllBooks()).thenReturn(
                List.of(bloch, gamma, mcconnell, martin),
                List.of(borrowedBloch, gamma, mcconnell, martin),
                List.of(borrowedBloch, gamma, mcconnell, martin)
        );
        when(service.borrowBook(id, "Иван Иванов")).thenReturn(borrowedBloch);
        when(service.getBookById(id)).thenReturn(borrowedBloch);

        String output = run(service,
                "borrow", "1", "\n   \n", "Иван Иванов",
                "return", "1", "нет",
                "return", "1", "да",
                "exit"
        );

        assertTrue(output.contains("Значение не может быть пустым. Попробуйте снова."));
        assertTrue(output.contains("Книга выдана: Джошуа Блох — «Java. Эффективное программирование» читателю Иван Иванов"));
        assertTrue(output.contains("Возврат отменён"));
        assertTrue(output.contains("Книга возвращена в каталог"));
        assertNoStackTrace(output);

        verify(service).borrowBook(id, "Иван Иванов");
        verify(service, times(1)).returnBook(id);
        verify(service, never()).returnBook(mcconnell.getId());
    }

    @Test
    @DisplayName("Сценарий 11б: список выдачи содержит только книги в наличии, список возврата — только выданные")
    void scenario11b_borrowAndReturnListsAreFiltered() {
        LibraryService service = createServiceWithDemoData();

        String borrowOutput = run(service, "borrow", "99", "exit");

        assertTrue(borrowOutput.contains("Номер книги вне допустимого диапазона (1-3)"),
                "В списке выдачи должны быть только 3 книги в наличии");
        assertTrue(borrowOutput.contains("Чистый код"));
        assertFalse(borrowOutput.contains("Совершенный код"), "Выданная книга не должна предлагаться к выдаче");

        String returnOutput = run(service, "return", "99", "exit");

        assertTrue(returnOutput.contains("Номер книги вне допустимого диапазона (1-1)"),
                "В списке возврата должна быть только 1 выданная книга");
        assertTrue(returnOutput.contains("Совершенный код"));
        assertFalse(returnOutput.contains("Чистый код"), "Книга в наличии не должна предлагаться к возврату");
    }

    @Test
    @DisplayName("Сценарий 12: операции на пустом каталоге выводят соответствующие сообщения")
    void scenario12_emptyCatalog() {
        LibraryService service = createEmptyService();

        String output = run(service, "show", "edit", "delete", "genre", "borrow", "return", "exit");

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
                "edit", "abc",
                "edit", "99",
                "edit", "0",
                "exit"
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

        String output = run(service, "add", "Роберт Мартин");

        assertTrue(output.contains("Ввод завершён. Выход..."));
        assertNoStackTrace(output);
    }

    @Test
    @DisplayName("Сценарий 15: непредвиденная ошибка без сообщения корректно описывается через имя класса")
    void scenario15_unexpectedExceptionHandling() {
        LibraryService service = createEmptyService();
        when(service.getAllBooks()).thenThrow(new RuntimeException());

        String output = run(service, "show", "exit");

        assertTrue(output.contains("Непредвиденная ошибка: RuntimeException"));
        assertTrue(output.contains("Выход..."));
        assertFalse(output.contains("\tat "));
    }
}
