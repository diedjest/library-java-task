package ru.vsu.cs.uvarov_d_p.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.entity.BookStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование репозитория InMemoryBookRepository")
class InMemoryBookRepositoryTest {

    private InMemoryBookRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryBookRepository();
    }

    private Book book(String author, String title, String isbn, String... genres) {
        List<String> genreList = (genres == null || genres.length == 0)
                ? List.of("Жанр")
                : List.of(genres);
        return new Book(title, author, isbn, genreList);
    }

    @Test
    @DisplayName("save сохраняет новую книгу, и она находится по ID")
    void shouldSaveAndFindNewBookById() {
        Book book = book("Роберт Мартин", "Чистый код", "9785446109609");

        repository.save(book);
        Optional<Book> found = repository.findById(book.getId());

        assertTrue(found.isPresent());
        assertEquals(book, found.get());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName("Повторный save книги с тем же ID заменяет её, количество не растёт")
    void shouldReplaceBookOnRepeatedSaveWithoutIncreasingSize() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Book original = new Book(id, "Старое название", "Автор", "111", List.of("IT"),
                BookStatus.AVAILABLE, null, now, now);
        Book updated = new Book(id, "Новое название", "Автор", "111", List.of("IT"),
                BookStatus.AVAILABLE, null, now, now);

        repository.save(original);
        repository.save(updated);
        Optional<Book> found = repository.findById(id);

        assertEquals(1, repository.findAll().size());
        assertTrue(found.isPresent());
        assertEquals("Новое название", found.get().getTitle());
    }

    @Test
    @DisplayName("save выбрасывает NullPointerException при передаче null")
    void shouldThrowNullPointerExceptionWhenSavingNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> repository.save(null));
        assertEquals("Книга не может быть null", ex.getMessage());
    }

    @Test
    @DisplayName("findById возвращает книгу по существующему ID")
    void shouldReturnBookByExistingId() {
        Book book = book("Джошуа Блох", "Java", "9785699661084");
        repository.save(book);

        Optional<Book> found = repository.findById(book.getId());

        assertTrue(found.isPresent());
        assertEquals(book, found.get());
    }

    @Test
    @DisplayName("findById возвращает пустой Optional для несуществующего ID и для null")
    void shouldReturnEmptyOptionalForNonExistingAndNullId() {
        Book book = book("Джошуа Блох", "Java", "9785699661084");
        repository.save(book);

        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
        assertTrue(repository.findById(null).isEmpty());
    }

    @Test
    @DisplayName("deleteById удаляет книгу по идентификатору")
    void shouldDeleteBookById() {
        Book book = book("Джошуа Блох", "Java", "9785699661084");
        repository.save(book);

        repository.deleteById(book.getId());

        assertTrue(repository.findById(book.getId()).isEmpty());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("deleteById с несуществующим ID или null не выбрасывает исключений и не меняет состав")
    void shouldDoNothingOnDeleteByIdWithNonExistingOrNullId() {
        Book book = book("Джошуа Блох", "Java", "9785699661084");
        repository.save(book);

        repository.deleteById(UUID.randomUUID());
        repository.deleteById(null);

        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(book.getId()).isPresent());
    }

    @Test
    @DisplayName("delete удаляет существующую книгу")
    void shouldDeleteBookByEntity() {
        Book book = book("Джошуа Блох", "Java", "9785699661084");
        repository.save(book);

        repository.delete(book);

        assertTrue(repository.findById(book.getId()).isEmpty());
    }

    @Test
    @DisplayName("delete выбрасывает NullPointerException при передаче null")
    void shouldThrowNullPointerExceptionWhenDeletingNull() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> repository.delete(null));
        assertEquals("Книга не может быть null", ex.getMessage());
    }

    @Test
    @DisplayName("findAll возвращает пустой список для пустого репозитория")
    void shouldReturnEmptyListWhenRepositoryIsEmpty() {
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    @DisplayName("findAll сортирует книги по фамилии авторов (последнее слово)")
    void shouldSortBooksByAuthorSurname() {
        Book martin = book("Роберт Мартин", "Чистый код", "1");
        Book bloch = book("Джошуа Блох", "Effective Java", "2");
        Book gamma = book("Эрих Гамма", "Design Patterns", "3");
        Book mcconnell = book("Стив Макконнелл", "Code Complete", "4");

        repository.save(martin);
        repository.save(bloch);
        repository.save(gamma);
        repository.save(mcconnell);

        List<Book> books = repository.findAll();

        assertEquals(List.of(bloch, gamma, mcconnell, martin), books);
    }

    @Test
    @DisplayName("findAll сортирует фамилии без учета регистра")
    void shouldSortSurnamesCaseInsensitively() {
        Book lowerBloch = book("джошуа блох", "Java B", "1");
        Book upperBloch = book("Джошуа Блох", "Java A", "2");
        Book gamma = book("Эрих Гамма", "Patterns", "3");

        repository.save(gamma);
        repository.save(lowerBloch);
        repository.save(upperBloch);

        List<Book> books = repository.findAll();

        assertEquals(List.of(upperBloch, lowerBloch, gamma), books);
    }

    @Test
    @DisplayName("findAll при одинаковой фамилии сортирует по полной строке автора")
    void shouldSortByFullAuthorWhenSurnameMatches() {
        Book boris = book("Борис Иванов", "Книга 1", "1");
        Book alex = book("Алексей Иванов", "Книга 2", "2");

        repository.save(boris);
        repository.save(alex);

        List<Book> books = repository.findAll();

        assertEquals(List.of(alex, boris), books);
    }

    @Test
    @DisplayName("findAll при одинаковом авторе сортирует по названию")
    void shouldSortByTitleWhenAuthorMatches() {
        Book b1 = book("Роберт Мартин", "Чистый код", "1");
        Book b2 = book("Роберт Мартин", "Идеальный программист", "2");

        repository.save(b1);
        repository.save(b2);

        List<Book> books = repository.findAll();

        assertEquals(List.of(b2, b1), books);
    }

    @Test
    @DisplayName("findAll при одинаковом авторе и названии сортирует по ISBN")
    void shouldSortByIsbnWhenAuthorAndTitleMatch() {
        Book b1 = book("Роберт Мартин", "Чистый код", "9785446109609");
        Book b2 = book("Роберт Мартин", "Чистый код", "9785446101061");

        repository.save(b1);
        repository.save(b2);

        List<Book> books = repository.findAll();

        assertEquals(List.of(b2, b1), books);
    }

    @Test
    @DisplayName("findAll сортирует автора из одного слова по этому слову")
    void shouldSortSingleWordAuthorByThatWord() {
        Book aristotle = book("Аристотель", "Поэтика", "1");
        Book homer = book("Гомер", "Илиада", "2");

        repository.save(homer);
        repository.save(aristotle);

        List<Book> books = repository.findAll();

        assertEquals(List.of(aristotle, homer), books);
    }

    @Test
    @DisplayName("findAll возвращает одинаковый порядок независимо от очередности добавления")
    void shouldSortConsistentlyRegardlessOfInsertionOrder() {
        Book b1 = book("Джошуа Блох", "Java", "1");
        Book b2 = book("Эрих Гамма", "Patterns", "2");
        Book b3 = book("Стив Макконнелл", "Code Complete", "3");
        Book b4 = book("Роберт Мартин", "Clean Code", "4");

        InMemoryBookRepository repo1 = new InMemoryBookRepository();
        repo1.save(b4);
        repo1.save(b1);
        repo1.save(b3);
        repo1.save(b2);

        InMemoryBookRepository repo2 = new InMemoryBookRepository();
        repo2.save(b1);
        repo2.save(b2);
        repo2.save(b3);
        repo2.save(b4);

        List<Book> expected = List.of(b1, b2, b3, b4);
        assertEquals(expected, repo1.findAll());
        assertEquals(expected, repo2.findAll());
    }

    @Test
    @DisplayName("Возвращаемые списки findAll и findByAuthorContaining неизменяемы")
    void shouldReturnUnmodifiableListsFromFindAllAndFindByAuthor() {
        Book b = book("Автор", "Название", "111");
        repository.save(b);

        List<Book> all = repository.findAll();
        List<Book> byAuthor = repository.findByAuthorContaining("Автор");

        assertNotNull(all);
        assertNotNull(byAuthor);
        assertThrows(UnsupportedOperationException.class, () -> all.add(b));
        assertThrows(UnsupportedOperationException.class, () -> byAuthor.add(b));
    }

    @Test
    @DisplayName("findByAuthorContaining находит книги по имени, фамилии, части слова, без учета регистра и с пробелами")
    void shouldFindByAuthorContainingWithVariousValidQueries() {
        Book martin = book("Роберт Мартин", "Чистый код", "1");
        Book bloch = book("Джошуа Блох", "Effective Java", "2");
        repository.save(martin);
        repository.save(bloch);

        assertEquals(List.of(martin), repository.findByAuthorContaining("Роберт"));
        assertEquals(List.of(martin), repository.findByAuthorContaining("Мартин"));
        assertEquals(List.of(martin), repository.findByAuthorContaining("март"));
        assertEquals(List.of(martin), repository.findByAuthorContaining("  роберт  "));
        assertEquals(List.of(bloch), repository.findByAuthorContaining("БЛОХ"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("findByAuthorContaining возвращает пустой список при null, пустом или пробельном запросе")
    void shouldReturnEmptyListForBlankAuthorQuery(String blankQuery) {
        repository.save(book("Роберт Мартин", "Чистый код", "1"));

        assertTrue(repository.findByAuthorContaining(blankQuery).isEmpty());
    }

    @Test
    @DisplayName("findByAuthorContaining возвращает пустой список, если совпадений нет")
    void shouldReturnEmptyListWhenNoAuthorMatches() {
        repository.save(book("Роберт Мартин", "Чистый код", "1"));

        assertTrue(repository.findByAuthorContaining("Толстой").isEmpty());
    }

    @Test
    @DisplayName("Результат findByAuthorContaining отсортирован по тем же правилам, что и findAll")
    void shouldReturnSortedResultForFindByAuthorContaining() {
        Book b1 = book("Роберт Мартин", "Чистый код", "1");
        Book b2 = book("Джордж Мартин", "Игра престолов", "2");
        repository.save(b1);
        repository.save(b2);

        List<Book> found = repository.findByAuthorContaining("Мартин");

        assertEquals(List.of(b2, b1), found);
    }

    @Test
    @DisplayName("findByIsbn находит книгу при точном совпадении нормализованного ISBN")
    void shouldFindBookByExactIsbn() {
        Book book = book("Роберт Мартин", "Чистый код", "9785446109609");
        repository.save(book);

        Optional<Book> found = repository.findByIsbn("9785446109609");

        assertTrue(found.isPresent());
        assertEquals(book, found.get());
    }

    @Test
    @DisplayName("findByIsbn возвращает empty для несуществующего ISBN")
    void shouldReturnEmptyForNonExistingIsbn() {
        repository.save(book("Роберт Мартин", "Чистый код", "9785446109609"));

        assertTrue(repository.findByIsbn("0000000000000").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("findByIsbn возвращает empty при null, пустом или пробельном ISBN")
    void shouldReturnEmptyForBlankIsbn(String blankIsbn) {
        repository.save(book("Роберт Мартин", "Чистый код", "9785446109609"));

        assertTrue(repository.findByIsbn(blankIsbn).isEmpty());
    }

    @Test
    @DisplayName("findByIsbn выполняет точное сравнение и не находит ISBN с дефисами")
    void shouldNotFindIsbnWithHyphensBecauseExactMatchIsExpected() {
        Book book = book("Роберт Мартин", "Чистый код", "9785446109609");
        repository.save(book);

        Optional<Book> found = repository.findByIsbn("978-5-4461-0960-9");

        assertTrue(found.isEmpty(), "Репозиторий ожидает нормализованный ISBN без дефисов");
    }
}