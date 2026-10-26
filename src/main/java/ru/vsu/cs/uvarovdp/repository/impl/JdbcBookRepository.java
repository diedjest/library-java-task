package ru.vsu.cs.uvarovdp.repository.impl;

import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.entity.BookStatus;
import ru.vsu.cs.uvarovdp.ex.RepositoryException;
import ru.vsu.cs.uvarovdp.repository.BookRepository;
import ru.vsu.cs.uvarovdp.util.BookComparators;
import ru.vsu.cs.uvarovdp.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class JdbcBookRepository implements BookRepository {

    private static final String SELECT_BOOKS = """
            SELECT b.id, b.title, b.author, b.isbn, b.status, b.borrower_name, b.created_at, b.updated_at, g.genre
            FROM books b
            LEFT JOIN book_genres g ON g.book_id = b.id
            """;
    private static final String ORDER_BY_GENRE_POSITION = " ORDER BY g.position";

    private static final String UPSERT_BOOK = """
            INSERT INTO books (id, title, author, isbn, status, borrower_name, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                title = EXCLUDED.title,
                author = EXCLUDED.author,
                isbn = EXCLUDED.isbn,
                status = EXCLUDED.status,
                borrower_name = EXCLUDED.borrower_name,
                updated_at = EXCLUDED.updated_at
            """;
    private static final String DELETE_GENRES = "DELETE FROM book_genres WHERE book_id = ?";
    private static final String INSERT_GENRE = "INSERT INTO book_genres (book_id, position, genre) VALUES (?, ?, ?)";
    private static final String DELETE_BOOK = "DELETE FROM books WHERE id = ?";

    private final DataSource dataSource;

    public JdbcBookRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "DataSource не может быть null");
    }

    @Override
    public void save(Book entity) {
        Objects.requireNonNull(entity, "Книга не может быть null");
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                upsertBook(connection, entity);
                replaceGenres(connection, entity);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Не удалось сохранить книгу: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(Book entity) {
        Objects.requireNonNull(entity, "Книга не может быть null");
        deleteById(entity.getId());
    }

    @Override
    public void deleteById(UUID id) {
        if (id == null) {
            return;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_BOOK)) {
            statement.setObject(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Не удалось удалить книгу: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Book> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        List<Book> books = query(SELECT_BOOKS + " WHERE b.id = ?" + ORDER_BY_GENRE_POSITION, id);
        return books.stream().findFirst();
    }

    @Override
    public List<Book> findAll() {
        return sorted(query(SELECT_BOOKS + ORDER_BY_GENRE_POSITION));
    }

    @Override
    public List<Book> findByAuthorContaining(String query) {
        if (StringUtils.isNullOrBlank(query)) {
            return List.of();
        }
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);
        return sorted(query(SELECT_BOOKS + " WHERE position(? in lower(b.author)) > 0" + ORDER_BY_GENRE_POSITION, lowerQuery));
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        if (StringUtils.isNullOrBlank(isbn)) {
            return Optional.empty();
        }
        List<Book> books = query(SELECT_BOOKS + " WHERE b.isbn = ?" + ORDER_BY_GENRE_POSITION, isbn);
        return books.stream().findFirst();
    }

    private static List<Book> sorted(List<Book> books) {
        return books.stream().sorted(BookComparators.BY_AUTHOR_THEN_TITLE).toList();
    }

    private static void upsertBook(Connection connection, Book book) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(UPSERT_BOOK)) {
            statement.setObject(1, book.getId());
            statement.setString(2, book.getTitle());
            statement.setString(3, book.getAuthor());
            statement.setString(4, book.getIsbn());
            statement.setString(5, book.getStatus().name());
            statement.setString(6, book.getBorrowerName());
            statement.setObject(7, book.getCreatedAt());
            statement.setObject(8, book.getUpdatedAt());
            statement.executeUpdate();
        }
    }

    private static void replaceGenres(Connection connection, Book book) throws SQLException {
        try (PreparedStatement delete = connection.prepareStatement(DELETE_GENRES)) {
            delete.setObject(1, book.getId());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = connection.prepareStatement(INSERT_GENRE)) {
            List<String> genres = book.getGenres();
            for (int i = 0; i < genres.size(); i++) {
                insert.setObject(1, book.getId());
                insert.setInt(2, i);
                insert.setString(3, genres.get(i));
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private List<Book> query(String sql, Object... parameters) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapBooks(resultSet);
            }
        } catch (SQLException e) {
            throw new RepositoryException("Не удалось прочитать книги: " + e.getMessage(), e);
        }
    }

    private static List<Book> mapBooks(ResultSet resultSet) throws SQLException {
        Map<UUID, ResultSetRow> rows = new LinkedHashMap<>();
        Map<UUID, List<String>> genresByBook = new HashMap<>();
        while (resultSet.next()) {
            UUID id = resultSet.getObject("id", UUID.class);
            rows.computeIfAbsent(id, key -> readRow(resultSet));
            String genre = resultSet.getString("genre");
            if (genre != null) {
                genresByBook.computeIfAbsent(id, key -> new ArrayList<>()).add(genre);
            }
        }
        return rows.entrySet().stream()
                .map(entry -> entry.getValue().toBook(genresByBook.getOrDefault(entry.getKey(), List.of())))
                .toList();
    }

    private static ResultSetRow readRow(ResultSet resultSet) {
        try {
            return new ResultSetRow(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("title"),
                    resultSet.getString("author"),
                    resultSet.getString("isbn"),
                    BookStatus.valueOf(resultSet.getString("status")),
                    resultSet.getString("borrower_name"),
                    resultSet.getObject("created_at", LocalDateTime.class),
                    resultSet.getObject("updated_at", LocalDateTime.class)
            );
        } catch (SQLException e) {
            throw new RepositoryException("Не удалось прочитать книгу: " + e.getMessage(), e);
        }
    }

    private record ResultSetRow(
            UUID id,
            String title,
            String author,
            String isbn,
            BookStatus status,
            String borrowerName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Book toBook(List<String> genres) {
            return new Book(id, title, author, isbn, genres, status, borrowerName, createdAt, updatedAt);
        }
    }
}
