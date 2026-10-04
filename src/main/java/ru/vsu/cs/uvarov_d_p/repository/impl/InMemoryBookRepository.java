package ru.vsu.cs.uvarov_d_p.repository.impl;

import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.repository.BookRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class InMemoryBookRepository implements BookRepository {

    private static final Comparator<Book> BY_AUTHOR_THEN_TITLE =
            Comparator.comparing((Book book) -> extractSurname(book.getAuthor()), String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Book::getAuthor, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(Book::getIsbn);

    private final Map<UUID, Book> storage = new HashMap<>();

    @Override
    public void save(Book entity) {
        Objects.requireNonNull(entity, "Книга не может быть null");
        storage.put(entity.getId(), entity);
    }

    @Override
    public void delete(Book entity) {
        Objects.requireNonNull(entity, "Книга не может быть null");
        deleteById(entity.getId());
    }

    @Override
    public void deleteById(UUID id) {
        if (id != null) {
            storage.remove(id);
        }
    }

    @Override
    public Optional<Book> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Book> findAll() {
        return storage.values().stream()
                .sorted(BY_AUTHOR_THEN_TITLE)
                .toList();
    }

    @Override
    public List<Book> findByAuthorContaining(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);
        return storage.values().stream()
                .filter(book -> book.getAuthor().toLowerCase(Locale.ROOT).contains(lowerQuery))
                .sorted(BY_AUTHOR_THEN_TITLE)
                .toList();
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(book -> book.getIsbn().equals(isbn))
                .findFirst();
    }

    // Суффиксы вроде «мл.» не обрабатываются
    private static String extractSurname(String author) {
        if (author == null || author.isBlank()) {
            return "";
        }
        String[] parts = author.trim().split("\\s+");
        return parts[parts.length - 1];
    }
}