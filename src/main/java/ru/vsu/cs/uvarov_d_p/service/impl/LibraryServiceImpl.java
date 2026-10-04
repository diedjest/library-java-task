package ru.vsu.cs.uvarov_d_p.service.impl;

import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.ex.BusinessRuleException;
import ru.vsu.cs.uvarov_d_p.ex.NotFoundException;
import ru.vsu.cs.uvarov_d_p.ex.ValidationException;
import ru.vsu.cs.uvarov_d_p.repository.BookRepository;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.util.IsbnValidator;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class LibraryServiceImpl implements LibraryService {

    private final BookRepository bookRepository;
    private final IsbnValidator isbnValidator;

    public LibraryServiceImpl(BookRepository bookRepository, IsbnValidator isbnValidator) {
        this.bookRepository = Objects.requireNonNull(bookRepository, "Репозиторий книг не может быть null");
        this.isbnValidator = Objects.requireNonNull(isbnValidator, "Валидатор ISBN не может быть null");
    }

    @Override
    public Book addBook(String title, String author, String isbn, List<String> genres) {
        checkIsbnAvailable(isbn, null);
        String normalizedIsbn = isbnValidator.normalize(isbn);
        Book book = new Book(title, author, normalizedIsbn, genres);
        bookRepository.save(book);
        return book;
    }

    @Override
    public Book editBook(UUID bookId, String title, String author, String isbn, List<String> genres) {
        Book book = getBookOrThrow(bookId);
        checkIsbnAvailable(isbn, bookId);
        String normalizedIsbn = isbnValidator.normalize(isbn);

        book.update(title, author, normalizedIsbn, genres);
        bookRepository.save(book);
        return book;
    }

    @Override
    public void deleteBook(UUID bookId) {
        Book book = getBookOrThrow(bookId);
        if (!book.isAvailable()) {
            throw new BusinessRuleException("Нельзя удалить выданную книгу");
        }
        bookRepository.deleteById(bookId);
    }

    @Override
    public Book addGenre(UUID bookId, String genre) {
        Book book = getBookOrThrow(bookId);
        book.addGenre(genre);
        bookRepository.save(book);
        return book;
    }

    @Override
    public Book borrowBook(UUID bookId, String borrowerName) {
        Book book = getBookOrThrow(bookId);
        book.borrow(borrowerName);
        bookRepository.save(book);
        return book;
    }

    @Override
    public Book returnBook(UUID bookId) {
        Book book = getBookOrThrow(bookId);
        book.giveBack();
        bookRepository.save(book);
        return book;
    }

    @Override
    public Book getBookById(UUID bookId) {
        return getBookOrThrow(bookId);
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public List<Book> searchByAuthor(String query) {
        if (query == null || query.isBlank()) {
            return getAllBooks();
        }
        return bookRepository.findByAuthorContaining(query);
    }

    @Override
    public Optional<Book> searchByIsbn(String isbn) {
        String normalizedIsbn = normalizeAndValidateIsbn(isbn);
        return bookRepository.findByIsbn(normalizedIsbn);
    }

    @Override
    public void checkIsbnAvailable(String isbn, UUID excludeBookId) {
        String normalizedIsbn = normalizeAndValidateIsbn(isbn);
        ensureIsbnIsUnique(normalizedIsbn, excludeBookId);
    }

    private Book getBookOrThrow(UUID bookId) {
        if (bookId == null) {
            throw new NotFoundException("Идентификатор книги не может быть null");
        }
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Книга с ID " + bookId + " не найдена"));
    }

    private String normalizeAndValidateIsbn(String isbn) {
        String normalized = isbnValidator.normalize(isbn);
        if (!isbnValidator.isValid(normalized)) {
            throw new ValidationException("Некорректный формат ISBN");
        }
        return normalized;
    }

    private void ensureIsbnIsUnique(String isbn, UUID excludeId) {
        bookRepository.findByIsbn(isbn).ifPresent(existingBook -> {
            if (excludeId == null || !existingBook.getId().equals(excludeId)) {
                throw new BusinessRuleException("Книга с ISBN " + isbn + " уже есть в каталоге");
            }
        });
    }
}