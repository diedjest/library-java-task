package ru.vsu.cs.uvarovdp.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import ru.vsu.cs.uvarovdp.dto.BookData;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.entity.BookStatus;
import ru.vsu.cs.uvarovdp.ex.BusinessRuleException;
import ru.vsu.cs.uvarovdp.ex.NotFoundException;
import ru.vsu.cs.uvarovdp.ex.ValidationException;
import ru.vsu.cs.uvarovdp.repository.BookRepository;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.util.IsbnValidator;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Тестирование сервиса LibraryServiceImpl")
class LibraryServiceImplTest {

    private static final String RAW_ISBN = "978-5-4461-0960-9";
    private static final String ISBN = "9785446109609";
    private static final String OTHER_ISBN = "9785750200641";

    private BookRepository repository;
    private IsbnValidator validator;
    private LibraryService service;

    @BeforeEach
    void setUp() {
        repository = mock(BookRepository.class);
        validator = mock(IsbnValidator.class);
        service = new LibraryServiceImpl(repository, validator);
    }

    private void stubValidIsbn(String raw, String normalized) {
        when(validator.normalize(raw)).thenReturn(normalized);
        when(validator.isValid(normalized)).thenReturn(true);
    }

    private void stubInvalidIsbn(String raw) {
        when(validator.normalize(raw)).thenReturn(raw);
        when(validator.isValid(raw)).thenReturn(false);
    }

    private Book storedBook(String isbn, List<String> genres) {
        Book book = new Book("Чистый код", "Роберт Мартин", isbn, genres);
        when(repository.findById(book.getId())).thenReturn(Optional.of(book));
        return book;
    }

    private Book storedBook() {
        return storedBook(ISBN, List.of("IT"));
    }

    private Book storedBorrowedBook() {
        Book book = storedBook();
        book.borrow("Алексей");
        return book;
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
    @DisplayName("addBook сохраняет в репозиторий книгу с нормализованным ISBN и статусом AVAILABLE")
    void shouldAddBookSuccessfully() {
        stubValidIsbn(RAW_ISBN, ISBN);

        Book book = service.addBook(new BookData("Чистый код", "Роберт Мартин", RAW_ISBN, List.of("IT")));

        ArgumentCaptor<Book> saved = ArgumentCaptor.forClass(Book.class);
        verify(repository).save(saved.capture());
        assertSame(book, saved.getValue());
        assertEquals(ISBN, book.getIsbn());
        assertEquals(BookStatus.AVAILABLE, book.getStatus());
        assertNull(book.getBorrowerName());
    }

    @Test
    @DisplayName("addBook выбрасывает ValidationException при некорректном формате ISBN, книга не сохраняется")
    void shouldThrowValidationExceptionWhenAddingBookWithInvalidIsbn() {
        stubInvalidIsbn("invalid-isbn");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.addBook(new BookData("Название", "Автор", "invalid-isbn", List.of("IT")))
        );
        assertEquals("Некорректный формат ISBN", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("addBook выбрасывает BusinessRuleException при дубликате ISBN, книга не сохраняется")
    void shouldThrowBusinessRuleExceptionWhenAddingDuplicateIsbn() {
        Book existing = storedBook();
        stubValidIsbn(ISBN, ISBN);
        when(repository.findByIsbn(ISBN)).thenReturn(Optional.of(existing));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.addBook(new BookData("Книга 2", "Автор 2", ISBN, List.of("Проза")))
        );
        assertEquals("Книга с ISBN 9785446109609 уже есть в каталоге", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("addBook выбрасывает ValidationException при невалидных данных сущности, книга не сохраняется")
    void shouldThrowValidationExceptionWhenAddingBookWithInvalidEntityFields() {
        stubValidIsbn(ISBN, ISBN);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.addBook(new BookData("   ", "Автор", ISBN, List.of("IT")))
        );
        assertEquals("Название книги не может быть пустым", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("editBook успешно изменяет данные книги и сохраняет ее")
    void shouldEditBookSuccessfully() {
        Book original = storedBook();
        stubValidIsbn(OTHER_ISBN, OTHER_ISBN);

        Book updated = service.editBook(original.getId(), new BookData("Новое название", "Новый автор", OTHER_ISBN, List.of("Наука")));

        assertEquals("Новое название", updated.getTitle());
        assertEquals("Новый автор", updated.getAuthor());
        assertEquals(OTHER_ISBN, updated.getIsbn());
        assertEquals(List.of("Наука"), updated.getGenres());
        verify(repository).save(original);
    }

    @Test
    @DisplayName("editBook допускает собственный ISBN книги в другом формате записи")
    void shouldAllowOwnIsbnInDifferentFormatOnEdit() {
        Book original = storedBook();
        stubValidIsbn(RAW_ISBN, ISBN);
        when(repository.findByIsbn(ISBN)).thenReturn(Optional.of(original));

        Book updated = service.editBook(original.getId(), new BookData("Новое название", "Автор", RAW_ISBN, List.of("IT")));

        assertEquals(ISBN, updated.getIsbn());
        assertEquals("Новое название", updated.getTitle());
    }

    @Test
    @DisplayName("editBook выбрасывает BusinessRuleException при попытке занять ISBN другой книги, поля не меняются")
    void shouldThrowBusinessRuleExceptionWhenEditingWithAnotherBooksIsbn() {
        Book book = storedBook(OTHER_ISBN, List.of("IT"));
        Book another = storedBook();
        stubValidIsbn(RAW_ISBN, ISBN);
        when(repository.findByIsbn(ISBN)).thenReturn(Optional.of(another));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> service.editBook(book.getId(), new BookData("Измененная", "Новый автор", RAW_ISBN, List.of("Наука")))
        );
        assertEquals("Книга с ISBN 9785446109609 уже есть в каталоге", ex.getMessage());

        assertEquals("Чистый код", book.getTitle());
        assertEquals(OTHER_ISBN, book.getIsbn());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("editBook выбрасывает ValidationException при неверном формате ISBN, книга не меняется")
    void shouldThrowValidationExceptionWhenEditingWithInvalidIsbnFormat() {
        Book book = storedBook();
        stubInvalidIsbn("bad-isbn");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.editBook(book.getId(), new BookData("Новое", "Новый автор", "bad-isbn", List.of("Наука")))
        );
        assertEquals("Некорректный формат ISBN", ex.getMessage());

        assertEquals("Чистый код", book.getTitle());
        assertEquals(ISBN, book.getIsbn());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("editBook атомарен: при невалидных жанрах ни одно поле книги не изменяется")
    void shouldMaintainAtomicityWhenEditFailsValidation() {
        Book book = storedBook();
        stubValidIsbn(OTHER_ISBN, OTHER_ISBN);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> service.editBook(book.getId(), new BookData("Новое название", "Новый автор", OTHER_ISBN,
                        List.of("1", "2", "3", "4")))
        );
        assertEquals("Количество жанров не может превышать 3", ex.getMessage());

        assertEquals("Чистый код", book.getTitle());
        assertEquals("Роберт Мартин", book.getAuthor());
        assertEquals(ISBN, book.getIsbn());
        assertEquals(List.of("IT"), book.getGenres());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("editBook выбрасывает NotFoundException для неизвестного ID и null")
    void shouldThrowNotFoundExceptionWhenEditingNonExistingOrNullId() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException exUnknown = assertThrows(
                NotFoundException.class,
                () -> service.editBook(unknownId, new BookData("Title", "Author", ISBN, List.of("IT")))
        );
        assertEquals("Книга с ID " + unknownId + " не найдена", exUnknown.getMessage());

        NotFoundException exNull = assertThrows(
                NotFoundException.class,
                () -> service.editBook(null, new BookData("Title", "Author", ISBN, List.of("IT")))
        );
        assertEquals("Идентификатор книги не может быть null", exNull.getMessage());
    }

    @Test
    @DisplayName("editBook сохраняет статус и имя читателя выданной книги")
    void shouldPreserveStatusAndBorrowerWhenEditingBorrowedBook() {
        Book book = storedBorrowedBook();
        stubValidIsbn(ISBN, ISBN);
        when(repository.findByIsbn(ISBN)).thenReturn(Optional.of(book));

        Book edited = service.editBook(book.getId(), new BookData("Чистый код 2", "Роберт Мартин", ISBN,
                List.of("IT", "Архитектура")));

        assertEquals(BookStatus.BORROWED, edited.getStatus());
        assertEquals("Алексей", edited.getBorrowerName());
    }

    @Test
    @DisplayName("editBook сохраняет жанры, если ему передан текущий список жанров самой книги")
    void shouldKeepGenresWhenEditingWithBooksOwnGenresList() {
        Book book = storedBook(ISBN, List.of("IT", "Архитектура"));
        stubValidIsbn(ISBN, ISBN);
        when(repository.findByIsbn(ISBN)).thenReturn(Optional.of(book));

        Book edited = service.editBook(book.getId(), new BookData("Новое название", "Роберт Мартин", ISBN, book.getGenres()));

        assertEquals("Новое название", edited.getTitle());
        assertEquals(List.of("IT", "Архитектура"), edited.getGenres());
    }

    @Test
    @DisplayName("deleteBook удаляет доступную книгу из репозитория")
    void shouldDeleteAvailableBook() {
        Book book = storedBook();

        service.deleteBook(book.getId());

        verify(repository).deleteById(book.getId());
    }

    @Test
    @DisplayName("deleteBook выбрасывает BusinessRuleException для выданной книги и не удаляет ее")
    void shouldThrowBusinessRuleExceptionWhenDeletingBorrowedBook() {
        Book book = storedBorrowedBook();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.deleteBook(book.getId()));
        assertEquals("Нельзя удалить выданную книгу", ex.getMessage());
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteBook успешно удаляет книгу после ее возврата")
    void shouldDeleteBookAfterItIsReturned() {
        Book book = storedBorrowedBook();
        service.returnBook(book.getId());

        service.deleteBook(book.getId());

        verify(repository).deleteById(book.getId());
    }

    @Test
    @DisplayName("deleteBook выбрасывает NotFoundException для неизвестного ID и null")
    void shouldThrowNotFoundExceptionWhenDeletingNonExistingOrNullId() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException exUnknown = assertThrows(NotFoundException.class, () -> service.deleteBook(unknownId));
        assertEquals("Книга с ID " + unknownId + " не найдена", exUnknown.getMessage());

        NotFoundException exNull = assertThrows(NotFoundException.class, () -> service.deleteBook(null));
        assertEquals("Идентификатор книги не может быть null", exNull.getMessage());
        verify(repository, never()).deleteById(any());
    }

    @Test
    @DisplayName("addGenre успешно добавляет жанр к существующей книге и сохраняет ее")
    void shouldAddGenreSuccessfully() {
        Book book = storedBook();

        Book updated = service.addGenre(book.getId(), "  Архитектура  ");

        assertEquals(List.of("IT", "Архитектура"), updated.getGenres());
        verify(repository).save(book);
    }

    @Test
    @DisplayName("addGenre выбрасывает ValidationException при добавлении дубликата без учета регистра")
    void shouldThrowValidationExceptionWhenAddingDuplicateGenre() {
        Book book = storedBook();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.addGenre(book.getId(), "it"));
        assertEquals("Жанр 'it' уже добавлен к книге", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("addGenre выбрасывает BusinessRuleException при добавлении четвертого жанра")
    void shouldThrowBusinessRuleExceptionWhenAddingFourthGenre() {
        Book book = storedBook(ISBN, List.of("1", "2", "3"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.addGenre(book.getId(), "4"));
        assertEquals("Нельзя добавить больше 3 жанров", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("addGenre выбрасывает ValidationException при пустом названии жанра")
    void shouldThrowValidationExceptionWhenAddingBlankGenre(String blankGenre) {
        Book book = storedBook();

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.addGenre(book.getId(), blankGenre));
        assertEquals("Название жанра не может быть пустым", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("addGenre выбрасывает NotFoundException для неизвестного ID книги")
    void shouldThrowNotFoundExceptionWhenAddingGenreToNonExistingBook() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.addGenre(unknownId, "IT"));
        assertEquals("Книга с ID " + unknownId + " не найдена", ex.getMessage());
    }

    @Test
    @DisplayName("borrowBook переводит книгу в BORROWED, сохраняет имя читателя и сохраняет книгу")
    void shouldBorrowBookSuccessfully() {
        Book book = storedBook();

        Book borrowed = service.borrowBook(book.getId(), "  Алексей  ");

        assertEquals(BookStatus.BORROWED, borrowed.getStatus());
        assertEquals("Алексей", borrowed.getBorrowerName());
        verify(repository).save(book);
    }

    @Test
    @DisplayName("borrowBook выбрасывает BusinessRuleException при повторной выдаче")
    void shouldThrowBusinessRuleExceptionWhenBorrowingAlreadyBorrowedBook() {
        Book book = storedBorrowedBook();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.borrowBook(book.getId(), "Иван"));
        assertEquals("Книгу нельзя выдать: текущий статус — Выдана", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("borrowBook выбрасывает BusinessRuleException при пустом читателе, статус прежний")
    void shouldThrowBusinessRuleExceptionWhenBorrowingWithBlankName(String blankName) {
        Book book = storedBook();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> service.borrowBook(book.getId(), blankName));
        assertEquals("Имя читателя не может быть пустым", ex.getMessage());

        assertEquals(BookStatus.AVAILABLE, book.getStatus());
        assertNull(book.getBorrowerName());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("borrowBook выбрасывает NotFoundException для неизвестного ID")
    void shouldThrowNotFoundExceptionWhenBorrowingNonExistingBook() {
        UUID unknownId = UUID.randomUUID();

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.borrowBook(unknownId, "Иван"));
        assertEquals("Книга с ID " + unknownId + " не найдена", ex.getMessage());
    }

    @Test
    @DisplayName("returnBook переводит книгу в AVAILABLE, сбрасывает читателя и сохраняет книгу")
    void shouldReturnBookSuccessfully() {
        Book book = storedBorrowedBook();

        Book returned = service.returnBook(book.getId());

        assertEquals(BookStatus.AVAILABLE, returned.getStatus());
        assertNull(returned.getBorrowerName());
        verify(repository).save(book);
    }

    @Test
    @DisplayName("returnBook выбрасывает BusinessRuleException, если книга не была выдана")
    void shouldThrowBusinessRuleExceptionWhenReturningAvailableBook() {
        Book book = storedBook();

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> service.returnBook(book.getId()));
        assertEquals("Книгу нельзя вернуть: текущий статус — В наличии", ex.getMessage());
        verify(repository, never()).save(any());
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
        Book book = storedBook();

        assertSame(book, service.getBookById(book.getId()));
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
    @DisplayName("getAllBooks возвращает список книг из репозитория")
    void shouldReturnAllBooksFromRepository() {
        Book martin = new Book("Чистый код", "Роберт Мартин", ISBN, List.of("IT"));
        Book bloch = new Book("Java", "Джошуа Блох", "9785699661084", List.of("IT"));
        when(repository.findAll()).thenReturn(List.of(bloch, martin));

        assertEquals(List.of(bloch, martin), service.getAllBooks());
    }

    @Test
    @DisplayName("searchByAuthor делегирует поиск репозиторию, а при пустом запросе возвращает все книги")
    void shouldSearchByAuthorCorrectly() {
        Book martin = new Book("Чистый код", "Роберт Мартин", ISBN, List.of("IT"));
        Book bloch = new Book("Java", "Джошуа Блох", "9785699661084", List.of("IT"));
        when(repository.findAll()).thenReturn(List.of(bloch, martin));
        when(repository.findByAuthorContaining("март")).thenReturn(List.of(martin));

        assertEquals(List.of(martin), service.searchByAuthor("март"));
        assertEquals(List.of(bloch, martin), service.searchByAuthor(""));
        assertEquals(List.of(bloch, martin), service.searchByAuthor(null));
        assertTrue(service.searchByAuthor("Толстой").isEmpty());
        verify(repository).findByAuthorContaining("март");
        verify(repository).findByAuthorContaining("Толстой");
    }

    @Test
    @DisplayName("searchByIsbn ищет по нормализованному ISBN, возвращает empty или выбрасывает ValidationException")
    void shouldSearchByIsbnWithVariousFormats() {
        Book book = new Book("Чистый код", "Роберт Мартин", ISBN, List.of("IT"));
        stubValidIsbn(RAW_ISBN, ISBN);
        stubValidIsbn("978-5-7502-0064-1", OTHER_ISBN);
        stubInvalidIsbn("123");
        stubInvalidIsbn("");
        when(repository.findByIsbn(ISBN)).thenReturn(Optional.of(book));

        Optional<Book> found = service.searchByIsbn(RAW_ISBN);
        assertTrue(found.isPresent());
        assertSame(book, found.get());

        assertTrue(service.searchByIsbn("978-5-7502-0064-1").isEmpty());

        ValidationException exBad = assertThrows(ValidationException.class, () -> service.searchByIsbn("123"));
        assertEquals("Некорректный формат ISBN", exBad.getMessage());

        ValidationException exNull = assertThrows(ValidationException.class, () -> service.searchByIsbn(null));
        assertEquals("Некорректный формат ISBN", exNull.getMessage());
    }

    @Test
    @DisplayName("checkIsbnAvailable валидирует доступность ISBN и не изменяет каталог")
    void shouldCheckIsbnAvailabilityWithoutModifyingCatalog() {
        Book existing = storedBook();
        stubValidIsbn("978-5-7502-0064-1", OTHER_ISBN);
        stubValidIsbn(RAW_ISBN, ISBN);
        stubInvalidIsbn("invalid");
        when(repository.findByIsbn(ISBN)).thenReturn(Optional.of(existing));

        service.checkIsbnAvailable("978-5-7502-0064-1", null);

        ValidationException exFormat = assertThrows(
                ValidationException.class,
                () -> service.checkIsbnAvailable("invalid", null)
        );
        assertEquals("Некорректный формат ISBN", exFormat.getMessage());

        BusinessRuleException exTaken = assertThrows(
                BusinessRuleException.class,
                () -> service.checkIsbnAvailable(RAW_ISBN, null)
        );
        assertEquals("Книга с ISBN 9785446109609 уже есть в каталоге", exTaken.getMessage());

        service.checkIsbnAvailable(RAW_ISBN, existing.getId());

        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
        verify(repository, never()).delete(any());
    }
}
