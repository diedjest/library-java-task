package ru.vsu.cs.uvarovdp.repository;

import ru.vsu.cs.uvarovdp.entity.Book;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends AbstractRepository<Book, UUID> {

    List<Book> findByAuthorContaining(String query);

    Optional<Book> findByIsbn(String isbn);
}