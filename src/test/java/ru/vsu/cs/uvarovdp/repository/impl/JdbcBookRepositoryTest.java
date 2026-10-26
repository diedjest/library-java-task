package ru.vsu.cs.uvarovdp.repository.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.entity.BookStatus;
import ru.vsu.cs.uvarovdp.ex.RepositoryException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Тестирование JdbcBookRepository на моках JDBC")
class JdbcBookRepositoryTest {

    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private JdbcBookRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        repository = new JdbcBookRepository(dataSource);
    }

    private Book newBook() {
        return new Book("Война и мир", "Лев Толстой", "9785041166410", List.of("Роман", "Классика"));
    }

    @Test
    @DisplayName("Конструктор отклоняет null вместо DataSource")
    void shouldRejectNullDataSource() {
        assertThrows(NullPointerException.class, () -> new JdbcBookRepository(null));
    }

    @Test
    @DisplayName("save и delete отклоняют null")
    void shouldRejectNullBooks() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.delete(null));
        verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("save выполняет upsert книги и замену жанров в одной транзакции")
    void shouldSaveBookAndGenresInSingleTransaction() throws SQLException {
        Book book = newBook();

        repository.save(book);

        var order = inOrder(connection, statement);
        order.verify(connection).setAutoCommit(false);
        order.verify(statement).setObject(1, book.getId());
        order.verify(statement).setString(2, "Война и мир");
        order.verify(connection).commit();
        verify(connection, never()).rollback();
        verify(connection).close();
    }

    @Test
    @DisplayName("save передаёт в запрос статус и имя читателя выданной книги")
    void shouldPassStatusAndBorrowerToUpsert() throws SQLException {
        Book book = newBook();
        book.borrow("Алексей Смирнов");

        repository.save(book);

        verify(statement).setString(5, BookStatus.BORROWED.name());
        verify(statement).setString(6, "Алексей Смирнов");
    }

    @Test
    @DisplayName("save записывает жанры с порядковыми номерами")
    void shouldInsertGenresWithPositions() throws SQLException {
        Book book = newBook();

        repository.save(book);

        verify(statement).setInt(2, 0);
        verify(statement).setString(3, "Роман");
        verify(statement).setInt(2, 1);
        verify(statement).setString(3, "Классика");
        verify(statement).executeBatch();
    }

    @Test
    @DisplayName("save откатывает транзакцию и бросает RepositoryException при ошибке записи жанров")
    void shouldRollbackWhenGenreInsertFails() throws SQLException {
        SQLException failure = new SQLException("batch failed");
        when(statement.executeBatch()).thenThrow(failure);

        RepositoryException exception = assertThrows(RepositoryException.class, () -> repository.save(newBook()));

        verify(connection).rollback();
        verify(connection, never()).commit();
        verify(connection).close();
        assertSame(failure, exception.getCause());
        assertTrue(exception.getMessage().contains("batch failed"));
    }

    @Test
    @DisplayName("save оборачивает ошибку получения соединения в RepositoryException")
    void shouldWrapConnectionFailureOnSave() throws SQLException {
        SQLException failure = new SQLException("no connection");
        when(dataSource.getConnection()).thenThrow(failure);

        RepositoryException exception = assertThrows(RepositoryException.class, () -> repository.save(newBook()));

        assertSame(failure, exception.getCause());
    }

    @Test
    @DisplayName("deleteById удаляет по идентификатору и закрывает соединение")
    void shouldDeleteById() throws SQLException {
        UUID id = UUID.randomUUID();

        repository.deleteById(id);

        verify(statement).setObject(1, id);
        verify(statement).executeUpdate();
        verify(connection).close();
    }

    @Test
    @DisplayName("deleteById(null) ничего не делает и не открывает соединение")
    void shouldIgnoreNullIdOnDelete() {
        repository.deleteById(null);

        verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("delete удаляет книгу по её идентификатору")
    void shouldDeleteEntityById() throws SQLException {
        Book book = newBook();

        repository.delete(book);

        verify(statement).setObject(1, book.getId());
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("deleteById оборачивает SQLException в RepositoryException")
    void shouldWrapFailureOnDelete() throws SQLException {
        when(statement.executeUpdate()).thenThrow(new SQLException("locked"));

        assertThrows(RepositoryException.class, () -> repository.deleteById(UUID.randomUUID()));
    }

    @Test
    @DisplayName("findById(null) возвращает пустой результат без обращения к базе")
    void shouldReturnEmptyForNullId() {
        assertTrue(repository.findById(null).isEmpty());
        verifyNoInteractions(dataSource);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    @DisplayName("findByIsbn и findByAuthorContaining не обращаются к базе при пустом запросе")
    void shouldNotQueryDatabaseForBlankSearch(String blank) {
        assertTrue(repository.findByIsbn(blank).isEmpty());
        assertTrue(repository.findByAuthorContaining(blank).isEmpty());
        verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("findByAuthorContaining передаёт запрос в нижнем регистре и без пробелов по краям")
    void shouldPassLowerCasedTrimmedAuthorQuery() throws SQLException {
        when(resultSet.next()).thenReturn(false);

        repository.findByAuthorContaining("  ТОЛС  ");

        verify(statement).setObject(1, "толс");
    }

    @Test
    @DisplayName("findByIsbn передаёт ISBN параметром запроса")
    void shouldPassIsbnAsParameter() throws SQLException {
        when(resultSet.next()).thenReturn(false);

        assertTrue(repository.findByIsbn("9785041166410").isEmpty());

        verify(statement).setObject(1, "9785041166410");
    }

    @Test
    @DisplayName("findAll оборачивает SQLException в RepositoryException")
    void shouldWrapFailureOnFindAll() throws SQLException {
        when(statement.executeQuery()).thenThrow(new SQLException("syntax"));

        RepositoryException exception = assertThrows(RepositoryException.class, () -> repository.findAll());

        assertTrue(exception.getMessage().contains("syntax"));
        verify(connection).close();
    }

    @Test
    @DisplayName("findAll собирает книгу из нескольких строк результата, склеивая жанры")
    void shouldGroupJoinedRowsIntoBooksWithGenres() throws SQLException {
        UUID id = UUID.randomUUID();
        LocalDateTime created = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        LocalDateTime updated = LocalDateTime.of(2026, 2, 3, 4, 5, 6);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getObject("id", UUID.class)).thenReturn(id);
        when(resultSet.getString("title")).thenReturn("Война и мир");
        when(resultSet.getString("author")).thenReturn("Лев Толстой");
        when(resultSet.getString("isbn")).thenReturn("9785041166410");
        when(resultSet.getString("status")).thenReturn("BORROWED");
        when(resultSet.getString("borrower_name")).thenReturn("Алексей Смирнов");
        when(resultSet.getObject("created_at", LocalDateTime.class)).thenReturn(created);
        when(resultSet.getObject("updated_at", LocalDateTime.class)).thenReturn(updated);
        when(resultSet.getString("genre")).thenReturn("Роман", "Классика");

        List<Book> books = repository.findAll();

        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals(id, book.getId());
        assertEquals("Война и мир", book.getTitle());
        assertEquals(BookStatus.BORROWED, book.getStatus());
        assertEquals("Алексей Смирнов", book.getBorrowerName());
        assertEquals(List.of("Роман", "Классика"), book.getGenres());
        assertEquals(created, book.getCreatedAt());
        assertEquals(updated, book.getUpdatedAt());
    }

    @Test
    @DisplayName("findById возвращает пустой результат, если строк нет")
    void shouldReturnEmptyWhenNoRows() throws SQLException {
        when(resultSet.next()).thenReturn(false);

        assertTrue(repository.findById(UUID.randomUUID()).isEmpty());
        verify(resultSet).close();
    }
}
