package ru.vsu.cs.uvarovdp.repository.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.vsu.cs.uvarovdp.db.DatabaseMigrator;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.entity.BookStatus;
import ru.vsu.cs.uvarovdp.ex.RepositoryException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Тестирование JdbcBookRepository на PostgreSQL")
class JdbcBookRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private static DataSource dataSource;

    private JdbcBookRepository repository;

    @BeforeAll
    static void migrateDatabase() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setUrl(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        dataSource = source;
        new DatabaseMigrator(dataSource).migrate();
    }

    @BeforeEach
    void cleanTables() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM books");
        }
        repository = new JdbcBookRepository(dataSource);
    }

    private static void assertWithinMicrosecond(LocalDateTime expected, LocalDateTime actual) {
        assertTrue(Duration.between(expected, actual).abs().compareTo(Duration.ofNanos(1000)) <= 0,
                "Ожидалось " + expected + ", получено " + actual);
    }

    private Book book(String title, String author, String isbn, String... genres) {
        return new Book(title, author, isbn, List.of(genres));
    }

    @Test
    @DisplayName("save с занятым ISBN бросает RepositoryException и не сохраняет жанры")
    void shouldRejectDuplicateIsbnAndKeepOriginalBook() {
        Book original = book("Война и мир", "Лев Толстой", "9785041166410", "Роман");
        repository.save(original);

        Book duplicate = book("Другая книга", "Другой автор", "9785041166410", "Драма");
        assertThrows(RepositoryException.class, () -> repository.save(duplicate));

        assertTrue(repository.findById(duplicate.getId()).isEmpty());
        assertEquals(1, repository.findAll().size());
        assertEquals(List.of("Роман"), repository.findById(original.getId()).orElseThrow().getGenres());
    }

    @Test
    @DisplayName("save при обновлении сохраняет дату создания и записывает новую дату обновления")
    void shouldKeepCreatedAtAndStoreUpdatedAtOnUpdate() {
        Book book = book("Война и мир", "Лев Толстой", "9785041166410", "Роман");
        repository.save(book);
        Book first = repository.findById(book.getId()).orElseThrow();

        book.borrow("Алексей Смирнов");
        repository.save(book);
        Book second = repository.findById(book.getId()).orElseThrow();

        assertEquals(first.getCreatedAt(), second.getCreatedAt());
        assertFalse(second.getUpdatedAt().isBefore(first.getUpdatedAt()));
    }

    @Test
    @DisplayName("Даты создания и обновления читаются из базы с точностью до микросекунды")
    void shouldRoundTripTimestamps() {
        Book book = book("Война и мир", "Лев Толстой", "9785041166410", "Роман");
        repository.save(book);

        Book loaded = repository.findById(book.getId()).orElseThrow();

        assertWithinMicrosecond(book.getCreatedAt(), loaded.getCreatedAt());
        assertWithinMicrosecond(book.getUpdatedAt(), loaded.getUpdatedAt());
    }

    @Test
    @DisplayName("Книга без изменений сохраняется повторно без дублирования жанров")
    void shouldNotDuplicateGenresOnRepeatedSave() {
        Book book = book("Мастер и Маргарита", "Михаил Булгаков", "9785041166427", "Роман", "Мистика");

        repository.save(book);
        repository.save(book);

        assertEquals(List.of("Роман", "Мистика"), repository.findById(book.getId()).orElseThrow().getGenres());
    }

    @Test
    @DisplayName("save и findById сохраняют все поля книги, включая порядок жанров")
    void shouldSaveAndLoadBookWithAllFields() {
        Book saved = book("Мастер и Маргарита", "Михаил Булгаков", "9785041166427", "Роман", "Мистика", "Сатира");

        repository.save(saved);
        Optional<Book> loaded = repository.findById(saved.getId());

        assertTrue(loaded.isPresent());
        Book book = loaded.get();
        assertEquals(saved.getId(), book.getId());
        assertEquals("Мастер и Маргарита", book.getTitle());
        assertEquals("Михаил Булгаков", book.getAuthor());
        assertEquals("9785041166427", book.getIsbn());
        assertEquals(List.of("Роман", "Мистика", "Сатира"), book.getGenres());
        assertEquals(BookStatus.AVAILABLE, book.getStatus());
        assertNull(book.getBorrowerName());
    }

    @Test
    @DisplayName("save обновляет существующую книгу, статус и жанры")
    void shouldUpdateExistingBook() {
        Book book = book("Война и мир", "Лев Толстой", "9785041166410", "Роман");
        repository.save(book);

        book.borrow("Алексей Смирнов");
        book.addGenre("Классика");
        repository.save(book);

        Book loaded = repository.findById(book.getId()).orElseThrow();
        assertEquals(BookStatus.BORROWED, loaded.getStatus());
        assertEquals("Алексей Смирнов", loaded.getBorrowerName());
        assertEquals(List.of("Роман", "Классика"), loaded.getGenres());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    @DisplayName("findById возвращает пустой результат для неизвестного идентификатора")
    void shouldReturnEmptyForUnknownId() {
        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("deleteById удаляет книгу вместе с жанрами")
    void shouldDeleteBookWithGenres() throws SQLException {
        Book book = book("Мёртвые души", "Николай Гоголь", "9785041166434", "Поэма", "Сатира");
        repository.save(book);

        repository.deleteById(book.getId());

        assertTrue(repository.findById(book.getId()).isEmpty());
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT count(*) FROM book_genres")) {
            resultSet.next();
            assertEquals(0, resultSet.getInt(1));
        }
    }

    @Test
    @DisplayName("findAll сортирует книги по фамилии автора, затем по названию")
    void shouldReturnBooksSortedBySurnameThenTitle() {
        repository.save(book("Мёртвые души", "Николай Гоголь", "9785041166434", "Поэма"));
        repository.save(book("Анна Каренина", "Лев Толстой", "9785041166441", "Роман"));
        repository.save(book("Война и мир", "Лев Толстой", "9785041166410", "Роман"));
        repository.save(book("Идиот", "Фёдор Достоевский", "9785041166458", "Роман"));

        List<String> titles = repository.findAll().stream().map(Book::getTitle).toList();

        assertEquals(List.of("Мёртвые души", "Идиот", "Анна Каренина", "Война и мир"), titles);
    }

    @Test
    @DisplayName("findByAuthorContaining ищет подстроку без учёта регистра")
    void shouldFindByAuthorSubstringIgnoringCase() {
        repository.save(book("Война и мир", "Лев Толстой", "9785041166410", "Роман"));
        repository.save(book("Мёртвые души", "Николай Гоголь", "9785041166434", "Поэма"));

        List<Book> found = repository.findByAuthorContaining("  ТОЛС ");

        assertEquals(1, found.size());
        assertEquals("Война и мир", found.get(0).getTitle());
    }

    @Test
    @DisplayName("findByAuthorContaining не считает символы % и _ шаблонами")
    void shouldTreatWildcardsInAuthorQueryLiterally() {
        repository.save(book("Война и мир", "Лев Толстой", "9785041166410", "Роман"));

        assertTrue(repository.findByAuthorContaining("%").isEmpty());
        assertTrue(repository.findByAuthorContaining("_").isEmpty());
    }

    @Test
    @DisplayName("findByAuthorContaining возвращает пустой список для пустого запроса")
    void shouldReturnEmptyListForBlankAuthorQuery() {
        repository.save(book("Война и мир", "Лев Толстой", "9785041166410", "Роман"));

        assertTrue(repository.findByAuthorContaining("   ").isEmpty());
    }

    @Test
    @DisplayName("findByIsbn находит книгу по нормализованному ISBN")
    void shouldFindBookByIsbn() {
        Book book = book("Война и мир", "Лев Толстой", "9785041166410", "Роман");
        repository.save(book);

        assertEquals(book.getId(), repository.findByIsbn("9785041166410").orElseThrow().getId());
        assertFalse(repository.findByIsbn("9785041166427").isPresent());
    }
}
