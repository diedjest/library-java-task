package ru.vsu.cs.uvarov_d_p.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.entity.BookStatus;
import ru.vsu.cs.uvarov_d_p.ex.BusinessRuleException;
import ru.vsu.cs.uvarov_d_p.ex.NotFoundException;
import ru.vsu.cs.uvarov_d_p.ex.ValidationException;
import ru.vsu.cs.uvarov_d_p.repository.BookRepository;
import ru.vsu.cs.uvarov_d_p.repository.impl.InMemoryBookRepository;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.util.IsbnValidator;
import ru.vsu.cs.uvarov_d_p.util.impl.IsbnValidatorImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование сервиса LibraryServiceImpl")
class LibraryServiceImplTest {

    private BookRepository repository;
    private IsbnValidator validator;
    private LibraryService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookRepository();
        validator = new IsbnValidatorImpl();
        service = new LibraryServiceImpl(repository, validator);
    }

    private record BookSnapshot(String title, String author, String isbn, List<String> genres,
                                BookStatus status, String borrowerName, LocalDateTime updatedAt) {
    }

    private BookSnapshot snapshot(Book book) {
        return new BookSnapshot(
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                List.copyOf(book.getGenres()),
                book.getStatus(),
                book.getBorrowerName(),
                book.getUpdatedAt()
        );
    }

    private void assertBookUnchanged(BookSnapshot before, UUID bookId) {
        assertEquals(before, snapshot(service.getBookById(bookId)), "Состояние книги не должно измениться");
    }

    @Test
    @DisplayName("Конструктор выбрасывает NullPointerException при передаче null зависимостей")
    void shouldThrowNullPointerExceptionWhenDependenciesAreNull() {
        NullPointerException exRepo = assertThrows(
                NullPointerException.class,
                () -> new LibraryServiceImpl(null, validator)
        );
        assertEquals("Репозиторий книг не может быть null", exRepo.getMessage());

        NullPointerException exValidator = assertThrows(
                NullPointerException.class,
                () -> new LibraryServiceImpl(repository, null)
        );
        assertEquals("Валидатор ISBN не может быть null", exValidator.getMessage());
    }

    @Test
    @DisplayName("addBook успешно сохраняет книгу с нормализованным ISBN и статусом AVAILABLE")
    void shouldAddBookSuccessfully() {
        String title = "Чистый код";
        String author = "Роберт Мартин";
        String rawIsbn = "978-5-4461-0960-9";
        List<String> genres = List.of("IT");

        Book book = service.addBook(title, author, rawIsbn, genres);

        assertNotNull(book);
        assertEquals("9785446109609", book.getIsbn());
        assertEquals(BookStatus.AVAILABLE, book.getStatus());
        assertNull(book.getBorrowerName());
        assertEquals(List.of(book), service.getAllBooks());
    }

    @Test
    @DisplayName("addBook выбрасывает ValidationException при некорректном формате ISBN, каталог пуст")
    void shouldThrowValidationExceptionWhenAddingBookWithInvalidIsbn() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.addBook("Название", "Автор", "invalid-isbn", List.of("IT"))
        );
        assertEquals("Некорректный формат ISBN", ex.getMessage());
        assertTrue(service.getAllBooks().isEmpty());
    }

    @Test
    @DisplayName("addBook выбрасывает BusinessRuleException при дубликате ISBN в разном форматировании")
    void shouldThrowBusinessRuleExceptionWhenAddingDuplicateIsbnInDifferentFormat() {
        service.addBook("Книга 1", "Автор 1", "978-5-4461-0960-9", List.of("IT"));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.addBook("Книга 2", "Автор 2", "9785446109609", List.of("Проза"))
        );
        assertEquals("Книга с ISBN 9785446109609 уже есть в каталоге", ex.getMessage());
        assertEquals(1, service.getAllBooks().size());
    }

    @Test
    @DisplayName("addBook выбрасывает ValidationException при невалидных данных сущности, каталог пуст")
    void shouldThrowValidationExceptionWhenAddingBookWithInvalidEntityFields() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.addBook("   ", "Автор", "9785446109609", List.of("IT"))
        );
        assertEquals("Название книги не может быть пустым", ex.getMessage());
        assertTrue(service.getAllBooks().isEmpty());
    }

    @Test
    @DisplayName("editBook успешно изменяет данные книги")
    void shouldEditBookSuccessfully() {
        Book original = service.addBook("Старое название", "Старый автор", "9785446109609", List.of("IT"));

        Book updated = service.editBook(original.getId(), "Новое название", "Новый автор", "9785750200641", List.of("Наука"));

        assertEquals("Новое название", updated.getTitle());
        assertEquals("Новый автор", updated.getAuthor());
        assertEquals("9785750200641", updated.getIsbn());
        assertEquals(List.of("Наука"), updated.getGenres());
    }

    @Test
    @DisplayName("editBook допускает собственный ISBN книги в другом формате записи")
    void shouldAllowOwnIsbnInDifferentFormatOnEdit() {
        Book original = service.addBook("Название", "Автор", "9785446109609", List.of("IT"));

        Book updated = service.editBook(original.getId(), "Новое название", "Автор", "978-5-4461-0960-9", List.of("IT"));

        assertEquals("9785446109609", updated.getIsbn());
        assertEquals("Новое название", updated.getTitle());
    }

    @Test
    @DisplayName("editBook выбрасывает BusinessRuleException при попытке занять ISBN другой книги, поля не меняются")
    void shouldThrowBusinessRuleExceptionWhenEditingWithAnotherBooksIsbn() {
        service.addBook("Книга 1", "Автор 1", "9785446109609", List.of("IT"));
        Book book2 = service.addBook("Книга 2", "Автор 2", "9785750200641", List.of("IT"));
        BookSnapshot before = snapshot(book2);

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.editBook(book2.getId(), "Измененная", "Новый автор", "978-5-4461-0960-9", List.of("Наука"))
        );
        assertEquals("Книга с ISBN 9785446109609 уже есть в каталоге", ex.getMessage());

        assertBookUnchanged(before, book2.getId());
    }

    @Test
    @DisplayName("editBook выбрасывает ValidationException при неверном формате ISBN, книга не меняется")
    void shouldThrowValidationExceptionWhenEditingWithInvalidIsbnFormat() {
        Book book = service.addBook("Название", "Автор", "9785446109609", List.of("IT"));
        BookSnapshot before = snapshot(book);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.editBook(book.getId(), "Новое", "Новый автор", "bad-isbn", List.of("Наука"))
        );
        assertEquals("Некорректный формат ISBN", ex.getMessage());

        assertBookUnchanged(before, book.getId());
    }

    @Test
    @DisplayName("editBook атомарен: при невалидных title или genres ни одно поле книги не изменяется")
    void shouldMaintainAtomicityWhenEditFailsValidation() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        BookSnapshot before = snapshot(book);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.editBook(book.getId(), "Новое название", "Новый автор", "9785750200641", List.of("1", "2", "3", "4"))
        );
        assertEquals("Количество жанров не может превышать 3", ex.getMessage());

        assertBookUnchanged(before, book.getId());
    }

    @Test
    @DisplayName("editBook выбрасывает NotFoundException для неизвестного ID и null")
    void shouldThrowNotFoundExceptionWhenEditingNonExistingOrNullId() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException exUnknown = assertThrows(
                NotFoundException.class,
                () -> service.editBook(unknownId, "Title", "Author", "9785446109609", List.of("IT"))
        );
        assertEquals("Книга с ID " + unknownId + " не найдена", exUnknown.getMessage());

        NotFoundException exNull = assertThrows(
                NotFoundException.class,
                () -> service.editBook(null, "Title", "Author", "9785446109609", List.of("IT"))
        );
        assertEquals("Идентификатор книги не может быть null", exNull.getMessage());
    }

    @Test
    @DisplayName("editBook сохраняет статус и имя читателя выданной книги")
    void shouldPreserveStatusAndBorrowerWhenEditingBorrowedBook() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        service.borrowBook(book.getId(), "Алексей");

        Book edited = service.editBook(book.getId(), "Чистый код 2", "Роберт Мартин", "9785446109609", List.of("IT", "Архитектура"));

        assertEquals(BookStatus.BORROWED, edited.getStatus());
        assertEquals("Алексей", edited.getBorrowerName());
    }

    @Test
    @DisplayName("editBook сохраняет жанры, если ему передан текущий список жанров самой книги")
    void shouldKeepGenresWhenEditingWithBooksOwnGenresList() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT", "Архитектура"));

        Book edited = service.editBook(book.getId(), "Новое название", "Роберт Мартин", "9785446109609", book.getGenres());

        assertEquals("Новое название", edited.getTitle());
        assertEquals(List.of("IT", "Архитектура"), edited.getGenres());
    }

    @Test
    @DisplayName("deleteBook удаляет доступную книгу")
    void shouldDeleteAvailableBook() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        service.deleteBook(book.getId());

        assertTrue(service.getAllBooks().isEmpty());
    }

    @Test
    @DisplayName("deleteBook выбрасывает BusinessRuleException для выданной книги и оставляет ее в каталоге")
    void shouldThrowBusinessRuleExceptionWhenDeletingBorrowedBook() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        service.borrowBook(book.getId(), "Алексей");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.deleteBook(book.getId()));
        assertEquals("Нельзя удалить выданную книгу", ex.getMessage());
        assertEquals(1, service.getAllBooks().size());
    }

    @Test
    @DisplayName("deleteBook успешно удаляет книгу после ее возврата")
    void shouldDeleteBookAfterItIsReturned() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        service.borrowBook(book.getId(), "Алексей");
        service.returnBook(book.getId());

        service.deleteBook(book.getId());

        assertTrue(service.getAllBooks().isEmpty());
    }

    @Test
    @DisplayName("deleteBook выбрасывает NotFoundException для неизвестного ID и null")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistingOrNullId() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException exUnknown = assertThrows(NotFoundException.class, () -> service.deleteBook(unknownId));
        assertEquals("Книга с ID " + unknownId + " не найдена", exUnknown.getMessage());

        NotFoundException exNull = assertThrows(NotFoundException.class, () -> service.deleteBook(null));
        assertEquals("Идентификатор книги не может быть null", exNull.getMessage());
    }

    @Test
    @DisplayName("addGenre успешно добавляет жанр к существующей книге")
    void shouldAddGenreSuccessfully() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        Book updated = service.addGenre(book.getId(), "  Архитектура  ");

        assertEquals(List.of("IT", "Архитектура"), updated.getGenres());
    }

    @Test
    @DisplayName("addGenre выбрасывает ValidationException при добавлении дубликата без учета регистра")
    void shouldThrowValidationExceptionWhenAddingDuplicateGenre() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.addGenre(book.getId(), "it"));
        assertEquals("Жанр 'it' уже добавлен к книге", ex.getMessage());
    }

    @Test
    @DisplayName("addGenre выбрасывает BusinessRuleException при добавлении четвертого жанра")
    void shouldThrowBusinessRuleExceptionWhenAddingFourthGenre() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("1", "2", "3"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.addGenre(book.getId(), "4"));
        assertEquals("Нельзя добавить больше 3 жанров", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("addGenre выбрасывает ValidationException при пустом названии жанра")
    void shouldThrowValidationExceptionWhenAddingBlankGenre(String blankGenre) {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        ValidationException ex = assertThrows(ValidationException.class, () -> service.addGenre(book.getId(), blankGenre));
        assertEquals("Название жанра не может быть пустым", ex.getMessage());
    }

    @Test
    @DisplayName("addGenre выбрасывает NotFoundException для неизвестного ID книги")
    void shouldThrowNotFoundExceptionWhenAddingGenreToNonExistingBook() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.addGenre(unknownId, "IT"));
        assertEquals("Книга с ID " + unknownId + " не найдена", ex.getMessage());
    }

    @Test
    @DisplayName("borrowBook переводит книгу в BORROWED и сохраняет имя читателя")
    void shouldBorrowBookSuccessfully() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        Book borrowed = service.borrowBook(book.getId(), "  Алексей  ");

        assertEquals(BookStatus.BORROWED, borrowed.getStatus());
        assertEquals("Алексей", borrowed.getBorrowerName());
    }

    @Test
    @DisplayName("borrowBook выбрасывает BusinessRuleException при повторной выдаче")
    void shouldThrowBusinessRuleExceptionWhenBorrowingAlreadyBorrowedBook() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        service.borrowBook(book.getId(), "Алексей");

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.borrowBook(book.getId(), "Иван"));
        assertEquals("Книгу нельзя выдать: текущий статус — Выдана", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("borrowBook выбрасывает BusinessRuleException при пустом читателе, статус прежний")
    void shouldThrowBusinessRuleExceptionWhenBorrowingWithBlankName(String blankName) {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.borrowBook(book.getId(), blankName));
        assertEquals("Имя читателя не может быть пустым", ex.getMessage());

        Book reloaded = service.getBookById(book.getId());
        assertEquals(BookStatus.AVAILABLE, reloaded.getStatus());
        assertNull(reloaded.getBorrowerName());
    }

    @Test
    @DisplayName("borrowBook выбрасывает NotFoundException для неизвестного ID")
    void shouldThrowNotFoundExceptionWhenBorrowingNonExistingBook() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.borrowBook(unknownId, "Иван"));
        assertEquals("Книга с ID " + unknownId + " не найдена", ex.getMessage());
    }

    @Test
    @DisplayName("returnBook переводит книгу в AVAILABLE и сбрасывает читателя")
    void shouldReturnBookSuccessfully() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        service.borrowBook(book.getId(), "Алексей");

        Book returned = service.returnBook(book.getId());

        assertEquals(BookStatus.AVAILABLE, returned.getStatus());
        assertNull(returned.getBorrowerName());
    }

    @Test
    @DisplayName("returnBook выбрасывает BusinessRuleException, если книга не была выдана")
    void shouldThrowBusinessRuleExceptionWhenReturningAvailableBook() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.returnBook(book.getId()));
        assertEquals("Книгу нельзя вернуть: текущий статус — В наличии", ex.getMessage());
    }

    @Test
    @DisplayName("returnBook выбрасывает NotFoundException для неизвестного ID книги")
    void shouldThrowNotFoundExceptionWhenReturningNonExistingBook() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.returnBook(unknownId));
        assertEquals("Книга с ID " + unknownId + " не найдена", ex.getMessage());
    }

    @Test
    @DisplayName("getBookById возвращает книгу по существующему ID")
    void shouldReturnBookByExistingId() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        Book found = service.getBookById(book.getId());

        assertEquals(book, found);
    }

    @Test
    @DisplayName("getBookById выбрасывает NotFoundException для неизвестного ID и null")
    void shouldThrowNotFoundExceptionWhenGettingBookByUnknownOrNullId() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException exUnknown = assertThrows(NotFoundException.class, () -> service.getBookById(unknownId));
        assertEquals("Книга с ID " + unknownId + " не найдена", exUnknown.getMessage());

        NotFoundException exNull = assertThrows(NotFoundException.class, () -> service.getBookById(null));
        assertEquals("Идентификатор книги не может быть null", exNull.getMessage());
    }

    @Test
    @DisplayName("getAllBooks возвращает неизменяемый список книг, отсортированный по фамилиям авторов")
    void shouldReturnUnmodifiableListSortedByAuthorSurname() {
        Book martin = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        Book bloch = service.addBook("Java", "Джошуа Блох", "9785699661084", List.of("IT"));

        List<Book> books = service.getAllBooks();

        assertEquals(List.of(bloch, martin), books);
        assertThrows(UnsupportedOperationException.class, () -> books.add(martin));
    }

    @Test
    @DisplayName("searchByAuthor ищет по части имени и фамилии без учета регистра, а при пустом запросе возвращает все книги")
    void shouldSearchByAuthorCorrectly() {
        Book martin = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        Book bloch = service.addBook("Java", "Джошуа Блох", "9785699661084", List.of("IT"));

        assertEquals(List.of(martin), service.searchByAuthor("март"));
        assertEquals(List.of(martin), service.searchByAuthor("РОБЕРТ"));
        assertEquals(List.of(bloch), service.searchByAuthor("Блох"));
        assertEquals(List.of(bloch, martin), service.searchByAuthor(""));
        assertEquals(List.of(bloch, martin), service.searchByAuthor(null));
        assertTrue(service.searchByAuthor("Толстой").isEmpty());
    }

    @Test
    @DisplayName("searchByIsbn находит книгу в другом формате записи, возвращает empty или выбрасывает ValidationException")
    void shouldSearchByIsbnWithVariousFormats() {
        Book book = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        Optional<Book> found = service.searchByIsbn("978-5-4461-0960-9");
        assertTrue(found.isPresent());
        assertEquals(book, found.get());

        Optional<Book> notFound = service.searchByIsbn("978-5-7502-0064-1");
        assertTrue(notFound.isEmpty());

        ValidationException exBad = assertThrows(ValidationException.class, () -> service.searchByIsbn("123"));
        assertEquals("Некорректный формат ISBN", exBad.getMessage());

        ValidationException exNull = assertThrows(ValidationException.class, () -> service.searchByIsbn(null));
        assertEquals("Некорректный формат ISBN", exNull.getMessage());
    }

    @Test
    @DisplayName("checkIsbnAvailable валидирует доступность ISBN и не изменяет каталог")
    void shouldCheckIsbnAvailabilityWithoutModifyingCatalog() {
        Book existing = service.addBook("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        List<BookSnapshot> before = service.getAllBooks().stream().map(this::snapshot).toList();

        service.checkIsbnAvailable("978-5-7502-0064-1", null);

        ValidationException exFormat = assertThrows(
                ValidationException.class,
                () -> service.checkIsbnAvailable("invalid", null)
        );
        assertEquals("Некорректный формат ISBN", exFormat.getMessage());

        BusinessRuleException exTaken = assertThrows(
                BusinessRuleException.class,
                () -> service.checkIsbnAvailable("978-5-4461-0960-9", null)
        );
        assertEquals("Книга с ISBN 9785446109609 уже есть в каталоге", exTaken.getMessage());

        service.checkIsbnAvailable("978-5-4461-0960-9", existing.getId());

        List<BookSnapshot> after = service.getAllBooks().stream().map(this::snapshot).toList();
        assertEquals(before, after, "checkIsbnAvailable не должен менять каталог");
    }
}