package ru.vsu.cs.uvarovdp.db;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.entity.BookStatus;
import ru.vsu.cs.uvarovdp.repository.impl.JdbcBookRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Тестирование миграций Liquibase на PostgreSQL")
class DatabaseMigratorIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    private static DataSource dataSource;

    @BeforeAll
    static void migrateDatabase() {
        PGSimpleDataSource source = new PGSimpleDataSource();
        source.setUrl(POSTGRES.getJdbcUrl());
        source.setUser(POSTGRES.getUsername());
        source.setPassword(POSTGRES.getPassword());
        dataSource = source;
        new DatabaseMigrator(dataSource).migrate();
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             var resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    @Test
    @DisplayName("Применяются все три changeset'а")
    void shouldApplyAllChangeSets() throws SQLException {
        assertEquals(3, count("SELECT count(*) FROM databasechangelog"));
    }

    @Test
    @DisplayName("Повторный запуск миграций ничего не меняет")
    void shouldBeIdempotent() throws SQLException {
        new DatabaseMigrator(dataSource).migrate();
        new DatabaseMigrator(dataSource).migrate();

        assertEquals(3, count("SELECT count(*) FROM databasechangelog"));
        assertEquals(4, count("SELECT count(*) FROM books"));
    }

    @Test
    @DisplayName("Демонстрационные данные: 4 книги и 11 жанров, одна книга выдана")
    void shouldSeedDemoBooks() throws SQLException {
        assertEquals(4, count("SELECT count(*) FROM books"));
        assertEquals(11, count("SELECT count(*) FROM book_genres"));
        assertEquals(1, count("SELECT count(*) FROM books WHERE status = 'BORROWED' AND borrower_name = 'Алексей Смирнов'"));
    }

    @Test
    @DisplayName("Демонстрационные книги читаются репозиторием как валидные сущности")
    void shouldLoadSeedBooksThroughRepository() {
        List<Book> books = new JdbcBookRepository(dataSource).findAll();

        Set<String> authors = books.stream().map(Book::getAuthor).collect(Collectors.toSet());
        assertEquals(Set.of("Фёдор Достоевский", "Лев Толстой", "Михаил Булгаков", "Николай Гоголь"), authors);
        assertEquals(BookStatus.BORROWED, books.stream()
                .filter(book -> book.getAuthor().equals("Лев Толстой")).findFirst().orElseThrow().getStatus());
    }

    @Test
    @DisplayName("Уникальный индекс не позволяет вставить повторяющийся ISBN")
    void shouldEnforceUniqueIsbn() {
        assertThrows(SQLException.class, () -> execute("""
                INSERT INTO books (id, title, author, isbn, status, created_at, updated_at)
                VALUES (gen_random_uuid(), 'T', 'A', '9785041166410', 'AVAILABLE', now(), now())
                """));
    }

    @Test
    @DisplayName("CHECK не позволяет выданную книгу без читателя и свободную книгу с читателем")
    void shouldEnforceBorrowerConsistency() {
        assertThrows(SQLException.class, () -> execute("""
                INSERT INTO books (id, title, author, isbn, status, created_at, updated_at)
                VALUES (gen_random_uuid(), 'T', 'A', '1111111111', 'BORROWED', now(), now())
                """));
        assertThrows(SQLException.class, () -> execute("""
                INSERT INTO books (id, title, author, isbn, status, borrower_name, created_at, updated_at)
                VALUES (gen_random_uuid(), 'T', 'A', '2222222222', 'AVAILABLE', 'Иван', now(), now())
                """));
    }

    @Test
    @DisplayName("CHECK не позволяет неизвестный статус")
    void shouldRejectUnknownStatus() {
        assertThrows(SQLException.class, () -> execute("""
                INSERT INTO books (id, title, author, isbn, status, borrower_name, created_at, updated_at)
                VALUES (gen_random_uuid(), 'T', 'A', '3333333333', 'LOST', 'Иван', now(), now())
                """));
    }

    @Test
    @DisplayName("Жанр нельзя привязать к несуществующей книге")
    void shouldEnforceGenreForeignKey() {
        assertThrows(SQLException.class, () -> execute(
                "INSERT INTO book_genres (book_id, position, genre) VALUES (gen_random_uuid(), 0, 'Драма')"));
    }

    @Test
    @DisplayName("Позиция жанра уникальна в пределах книги")
    void shouldEnforceUniqueGenrePositionPerBook() {
        assertThrows(SQLException.class, () -> execute("""
                INSERT INTO book_genres (book_id, position, genre)
                VALUES ('0b1f6a54-6f3c-4e5b-9d57-1f0f3c9a0001', 0, 'Дубликат позиции')
                """));
    }

    @Test
    @DisplayName("Удаление книги каскадно удаляет её жанры")
    void shouldCascadeDeleteGenres() throws SQLException {
        execute("""
                INSERT INTO books (id, title, author, isbn, status, created_at, updated_at)
                VALUES ('0b1f6a54-6f3c-4e5b-9d57-1f0f3c9a0099', 'T', 'A', '4444444444', 'AVAILABLE', now(), now())
                """);
        execute("INSERT INTO book_genres (book_id, position, genre) VALUES ('0b1f6a54-6f3c-4e5b-9d57-1f0f3c9a0099', 0, 'X')");

        execute("DELETE FROM books WHERE id = '0b1f6a54-6f3c-4e5b-9d57-1f0f3c9a0099'");

        assertEquals(0, count("SELECT count(*) FROM book_genres WHERE book_id = '0b1f6a54-6f3c-4e5b-9d57-1f0f3c9a0099'"));
    }
}
