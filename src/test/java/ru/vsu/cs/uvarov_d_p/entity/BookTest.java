package ru.vsu.cs.uvarov_d_p.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.vsu.cs.uvarov_d_p.domain.BookConstraints;
import ru.vsu.cs.uvarov_d_p.ex.BusinessRuleException;
import ru.vsu.cs.uvarov_d_p.ex.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование сущности Book")
class BookTest {

    private static final LocalDateTime PAST_CREATED_AT = LocalDateTime.now().minusDays(2);
    private static final LocalDateTime PAST_UPDATED_AT = LocalDateTime.now().minusDays(1);

    private Book bookWithPastTimestamps(BookStatus status, String borrowerName) {
        return new Book(UUID.randomUUID(), "Чистый код", "Роберт Мартин", "9785446109609",
                List.of("IT"), status, borrowerName, PAST_CREATED_AT, PAST_UPDATED_AT);
    }

    @Test
    @DisplayName("Конструктор успешно создает книгу с валидными данными и триммит строки")
    void shouldCreateBookWithValidDataAndTrimStrings() {
        String title = "  Чистый код  ";
        String author = "  Роберт Мартин  ";
        String isbn = "  9785446109609  ";
        List<String> genres = List.of("  Программирование  ");

        Book book = new Book(title, author, isbn, genres);

        assertNotNull(book.getId());
        assertNotNull(book.getCreatedAt());
        assertNotNull(book.getUpdatedAt());
        assertEquals(BookStatus.AVAILABLE, book.getStatus());
        assertNull(book.getBorrowerName());
        assertEquals("Чистый код", book.getTitle());
        assertEquals("Роберт Мартин", book.getAuthor());
        assertEquals("9785446109609", book.getIsbn());
        assertEquals(List.of("Программирование"), book.getGenres());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Конструктор выбрасывает ValidationException при пустом названии")
    void shouldThrowValidationExceptionWhenTitleIsBlank(String invalidTitle) {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book(invalidTitle, "Роберт Мартин", "9785446109609", List.of("IT"))
        );
        assertEquals("Название книги не может быть пустым", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Конструктор выбрасывает ValidationException при пустом авторе")
    void shouldThrowValidationExceptionWhenAuthorIsBlank(String invalidAuthor) {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book("Чистый код", invalidAuthor, "9785446109609", List.of("IT"))
        );
        assertEquals("Автор книги не может быть пустым", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    @DisplayName("Конструктор выбрасывает ValidationException при пустом ISBN")
    void shouldThrowValidationExceptionWhenIsbnIsBlank(String invalidIsbn) {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book("Чистый код", "Роберт Мартин", invalidIsbn, List.of("IT"))
        );
        assertEquals("ISBN книги не может быть пустым", ex.getMessage());
    }

    @Test
    @DisplayName("Конструктор выбрасывает ValidationException при genres == null")
    void shouldThrowValidationExceptionWhenGenresIsNull() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book("Чистый код", "Роберт Мартин", "9785446109609", null)
        );
        assertEquals("Список жанров не может быть null", ex.getMessage());
    }

    @Test
    @DisplayName("Конструктор выбрасывает ValidationException при пустом списке жанров")
    void shouldThrowValidationExceptionWhenGenresIsEmpty() {
        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of())
        );
        assertEquals("Книга должна содержать хотя бы один жанр", ex.getMessage());
    }

    @Test
    @DisplayName("Конструктор выбрасывает ValidationException при превышении лимита жанров")
    void shouldThrowValidationExceptionWhenGenresExceedLimit() {
        List<String> tooManyGenres = List.of("Жанр 1", "Жанр 2", "Жанр 3", "Жанр 4");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book("Чистый код", "Роберт Мартин", "9785446109609", tooManyGenres)
        );
        assertEquals("Количество жанров не может превышать " + BookConstraints.MAX_GENRES, ex.getMessage());
    }

    @Test
    @DisplayName("Конструктор выбрасывает ValidationException при наличии null в списке жанров")
    void shouldThrowValidationExceptionWhenGenreContainsNull() {
        List<String> genresWithNull = Arrays.asList("Роман", null);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book("Чистый код", "Роберт Мартин", "9785446109609", genresWithNull)
        );
        assertEquals("Название жанра не может быть пустым", ex.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    @DisplayName("Конструктор выбрасывает ValidationException при пустом элементе жанра")
    void shouldThrowValidationExceptionWhenGenreContainsBlankString(String blankGenre) {
        List<String> genresWithBlank = List.of("Роман", blankGenre);

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book("Чистый код", "Роберт Мартин", "9785446109609", genresWithBlank)
        );
        assertEquals("Название жанра не может быть пустым", ex.getMessage());
    }

    @Test
    @DisplayName("Конструктор выбрасывает ValidationException при дубликатах жанров без учета регистра")
    void shouldThrowValidationExceptionWhenGenresHaveDuplicatesCaseInsensitive() {
        List<String> duplicateGenres = List.of("Роман", "роман");

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book("Чистый код", "Роберт Мартин", "9785446109609", duplicateGenres)
        );
        assertEquals("Жанры не должны повторяться: роман", ex.getMessage());
    }

    @Test
    @DisplayName("Коллекция жанров защищена от внешних мутаций и getGenres возвращает unmodifiableList")
    void shouldProtectGenresFromExternalModifications() {
        List<String> externalList = new ArrayList<>(List.of("Программирование"));
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", externalList);

        externalList.add("Детектив");

        assertEquals(List.of("Программирование"), book.getGenres());
        assertThrows(UnsupportedOperationException.class, () -> book.getGenres().add("Фантастика"));
    }

    @Test
    @DisplayName("Восстанавливающий конструктор выбрасывает ValidationException при AVAILABLE со статусом и именем читателя")
    void shouldThrowValidationExceptionWhenRestoringAvailableBookWithBorrower() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book(id, "Чистый код", "Роберт Мартин", "9785446109609",
                        List.of("IT"), BookStatus.AVAILABLE, "Иван", now, now)
        );
        assertEquals("У книги в наличии не должно быть читателя", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("Восстанавливающий конструктор выбрасывает ValidationException при BORROWED без имени читателя")
    void shouldThrowValidationExceptionWhenRestoringBorrowedBookWithoutBorrower(String invalidBorrower) {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ValidationException ex = assertThrows(
                ValidationException.class,
                () -> new Book(id, "Чистый код", "Роберт Мартин", "9785446109609",
                        List.of("IT"), BookStatus.BORROWED, invalidBorrower, now, now)
        );
        assertEquals("Для выданной книги должно быть указано имя читателя", ex.getMessage());
    }

    @Test
    @DisplayName("Восстанавливающий конструктор выбрасывает NullPointerException при status == null")
    void shouldThrowNullPointerExceptionWhenRestoringWithNullStatus() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        NullPointerException ex = assertThrows(
                NullPointerException.class,
                () -> new Book(id, "Чистый код", "Роберт Мартин", "9785446109609",
                        List.of("IT"), null, null, now, now)
        );
        assertEquals("Статус книги не может быть null", ex.getMessage());
    }

    @Test
    @DisplayName("Восстанавливающий конструктор корректно сохраняет все переданные атрибуты")
    void shouldRestoreBookCorrectlyWithAllAttributes() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(2);
        LocalDateTime updatedAt = LocalDateTime.now().minusDays(1);

        Book book = new Book(id, "Чистый код", "Роберт Мартин", "9785446109609",
                List.of("IT"), BookStatus.BORROWED, "  Иван Иванов  ", createdAt, updatedAt);

        assertEquals(id, book.getId());
        assertEquals(BookStatus.BORROWED, book.getStatus());
        assertEquals("Иван Иванов", book.getBorrowerName());
        assertEquals(createdAt, book.getCreatedAt());
        assertEquals(updatedAt, book.getUpdatedAt());
    }

    @Test
    @DisplayName("update изменяет данные книги, не трогая статус, читателя и createdAt, и обновляет updatedAt")
    void shouldUpdateBookDataPreservingStatusAndBorrower() {
        Book book = bookWithPastTimestamps(BookStatus.BORROWED, "Иван Иванов");

        book.update("Новый код", "Новый Мартин", "9785446100000", List.of("Архитектура"));

        assertEquals("Новый код", book.getTitle());
        assertEquals("Новый Мартин", book.getAuthor());
        assertEquals("9785446100000", book.getIsbn());
        assertEquals(List.of("Архитектура"), book.getGenres());
        assertEquals(BookStatus.BORROWED, book.getStatus());
        assertEquals("Иван Иванов", book.getBorrowerName());
        assertEquals(PAST_CREATED_AT, book.getCreatedAt());
        assertTrue(book.getUpdatedAt().isAfter(PAST_UPDATED_AT), "update должен обновить updatedAt");
    }

    @Test
    @DisplayName("update атомарен: при ошибке валидации ни одно поле и updatedAt не изменяются")
    void shouldRemainUnchangedWhenUpdateFailsValidation() {
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        LocalDateTime initialUpdatedAt = book.getUpdatedAt();

        assertThrows(ValidationException.class, () ->
                book.update("Новый код", "Новый Мартин", "9785446100000", List.of("1", "2", "3", "4"))
        );

        assertEquals("Чистый код", book.getTitle());
        assertEquals("Роберт Мартин", book.getAuthor());
        assertEquals("9785446109609", book.getIsbn());
        assertEquals(List.of("IT"), book.getGenres());
        assertEquals(initialUpdatedAt, book.getUpdatedAt());
    }

    @Test
    @DisplayName("addGenre добавляет жанр с обрезкой пробелов и обновляет updatedAt")
    void shouldAddGenreWithTrimAndMarkAsUpdated() {
        Book book = bookWithPastTimestamps(BookStatus.AVAILABLE, null);

        book.addGenre("  Архитектура  ");

        assertEquals(List.of("IT", "Архитектура"), book.getGenres());
        assertTrue(book.getUpdatedAt().isAfter(PAST_UPDATED_AT), "addGenre должен обновить updatedAt");
        assertEquals(PAST_CREATED_AT, book.getCreatedAt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("addGenre выбрасывает ValidationException при пустом названии жанра")
    void shouldThrowValidationExceptionWhenAddingBlankGenre(String blankGenre) {
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        ValidationException ex = assertThrows(ValidationException.class, () -> book.addGenre(blankGenre));
        assertEquals("Название жанра не может быть пустым", ex.getMessage());
        assertEquals(List.of("IT"), book.getGenres());
    }

    @Test
    @DisplayName("addGenre выбрасывает ValidationException при добавлении дубликата без учета регистра")
    void shouldThrowValidationExceptionWhenAddingDuplicateGenre() {
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        ValidationException ex = assertThrows(ValidationException.class, () -> book.addGenre("  it  "));
        assertEquals("Жанр 'it' уже добавлен к книге", ex.getMessage());
        assertEquals(List.of("IT"), book.getGenres());
    }

    @Test
    @DisplayName("addGenre выбрасывает BusinessRuleException при превышении лимита MAX_GENRES")
    void shouldThrowBusinessRuleExceptionWhenAddingGenreBeyondLimit() {
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("1", "2", "3"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () -> book.addGenre("4"));
        assertEquals("Нельзя добавить больше " + BookConstraints.MAX_GENRES + " жанров", ex.getMessage());
        assertEquals(List.of("1", "2", "3"), book.getGenres());
    }

    @Test
    @DisplayName("borrow переводит книгу из AVAILABLE в BORROWED, триммит имя читателя и обновляет updatedAt")
    void shouldBorrowAvailableBookSuccessfully() {
        Book book = bookWithPastTimestamps(BookStatus.AVAILABLE, null);

        book.borrow("  Алексей Смирнов  ");

        assertEquals(BookStatus.BORROWED, book.getStatus());
        assertEquals("Алексей Смирнов", book.getBorrowerName());
        assertFalse(book.isAvailable());
        assertTrue(book.getUpdatedAt().isAfter(PAST_UPDATED_AT), "borrow должен обновить updatedAt");
    }

    @Test
    @DisplayName("borrow выбрасывает BusinessRuleException при попытке повторной выдачи")
    void shouldThrowBusinessRuleExceptionWhenBorrowingAlreadyBorrowedBook() {
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));
        book.borrow("Алексей Смирнов");

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> book.borrow("Другой Читатель")
        );
        assertEquals("Книгу нельзя выдать: текущий статус — Выдана", ex.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("borrow выбрасывает BusinessRuleException при пустом имени читателя")
    void shouldThrowBusinessRuleExceptionWhenBorrowerNameIsBlank(String blankBorrower) {
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        BusinessRuleException ex = assertThrows(
                BusinessRuleException.class,
                () -> book.borrow(blankBorrower)
        );
        assertEquals("Имя читателя не может быть пустым", ex.getMessage());
        assertEquals(BookStatus.AVAILABLE, book.getStatus());
        assertNull(book.getBorrowerName());
    }

    @Test
    @DisplayName("giveBack переводит BORROWED в AVAILABLE, сбрасывает читателя в null и обновляет updatedAt")
    void shouldReturnBorrowedBookSuccessfully() {
        Book book = bookWithPastTimestamps(BookStatus.BORROWED, "Алексей Смирнов");

        book.giveBack();

        assertEquals(BookStatus.AVAILABLE, book.getStatus());
        assertNull(book.getBorrowerName());
        assertTrue(book.isAvailable());
        assertTrue(book.getUpdatedAt().isAfter(PAST_UPDATED_AT), "giveBack должен обновить updatedAt");
    }

    @Test
    @DisplayName("giveBack выбрасывает BusinessRuleException при попытке вернуть книгу со статусом AVAILABLE")
    void shouldThrowBusinessRuleExceptionWhenReturningAvailableBook() {
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, book::giveBack);
        assertEquals("Книгу нельзя вернуть: текущий статус — В наличии", ex.getMessage());
    }

    @Test
    @DisplayName("isAvailable возвращает true только для статуса AVAILABLE")
    void shouldVerifyIsAvailableMatchesStatus() {
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT"));

        assertTrue(book.isAvailable());
        book.borrow("Иван");
        assertFalse(book.isAvailable());
    }

    @Test
    @DisplayName("equals и hashCode зависят только от id книги")
    void shouldVerifyEqualsAndHashCodeBasedOnlyOnId() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Book book1 = new Book(id, "Название 1", "Автор 1", "111", List.of("Жанр 1"),
                BookStatus.AVAILABLE, null, now, now);
        Book book2 = new Book(id, "Название 2", "Автор 2", "222", List.of("Жанр 2"),
                BookStatus.BORROWED, "Иван", now, now);
        Book bookWithDifferentId = new Book(UUID.randomUUID(), "Название 1", "Автор 1", "111",
                List.of("Жанр 1"), BookStatus.AVAILABLE, null, now, now);

        assertEquals(book1, book2);
        assertEquals(book1.hashCode(), book2.hashCode());
        assertNotEquals(book1, bookWithDifferentId);
        assertNotEquals(null, book1);
        assertNotEquals("Строка", book1);
    }

    @Test
    @DisplayName("Неудачные операции не меняют updatedAt")
    void shouldNotTouchUpdatedAtWhenOperationsFail() {
        Book book = bookWithPastTimestamps(BookStatus.AVAILABLE, null);

        assertThrows(ValidationException.class, () -> book.addGenre("   "));
        assertThrows(BusinessRuleException.class, () -> book.borrow("   "));
        assertThrows(BusinessRuleException.class, book::giveBack);
        assertThrows(ValidationException.class, () -> book.update("", "Автор", "9785446109609", List.of("IT")));

        assertEquals(PAST_UPDATED_AT, book.getUpdatedAt());
    }

    @Test
    @DisplayName("update не стирает жанры, если ему передан getGenres() этой же книги (живое представление)")
    void shouldKeepGenresWhenUpdatedWithOwnGenresView() {
        Book book = new Book("Чистый код", "Роберт Мартин", "9785446109609", List.of("IT", "Архитектура"));

        book.update("Новый код", "Роберт Мартин", "9785446109609", book.getGenres());

        assertEquals(List.of("IT", "Архитектура"), book.getGenres());
        assertEquals("Новый код", book.getTitle());
    }
}