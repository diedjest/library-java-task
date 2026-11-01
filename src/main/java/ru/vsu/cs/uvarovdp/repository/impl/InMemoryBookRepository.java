package ru.vsu.cs.uvarovdp.repository.impl;

import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.repository.BookRepository;
import ru.vsu.cs.uvarovdp.util.BookComparators;
import ru.vsu.cs.uvarovdp.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class InMemoryBookRepository implements BookRepository {

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
        if (id == null) {
            return;
        }
        storage.remove(id);
    }

    @Override
    public Optional<Book> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Book> findAll() {
        return storage.values().stream()
                .sorted(BookComparators.BY_AUTHOR_THEN_TITLE)
                .toList();
    }

    @Override
    public List<Book> findByAuthorContaining(String query) {
        if (StringUtils.isNullOrBlank(query)) {
            return List.of();
        }
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);
        return storage.values().stream()
                .filter(book -> book.getAuthor().toLowerCase(Locale.ROOT).contains(lowerQuery))
                .sorted(BookComparators.BY_AUTHOR_THEN_TITLE)
                .toList();
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        if (StringUtils.isNullOrBlank(isbn)) {
            return Optional.empty();
        }
        return storage.values().stream()
                .filter(book -> book.getIsbn().equals(isbn))
                .findFirst();
    }
}