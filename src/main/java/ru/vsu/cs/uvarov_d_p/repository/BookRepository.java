package ru.vsu.cs.uvarov_d_p.repository;

import ru.vsu.cs.uvarov_d_p.entity.Book;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends MyRepository<Book, UUID> {

    List<Book> findByAuthorContaining(String query);

    Optional<Book> findByIsbn(String isbn);
}