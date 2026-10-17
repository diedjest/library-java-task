package ru.vsu.cs.uvarovdp.entity;

import ru.vsu.cs.uvarovdp.domain.BookConstraints;
import ru.vsu.cs.uvarovdp.ex.BusinessRuleException;
import ru.vsu.cs.uvarovdp.ex.BusinessRuleType;
import ru.vsu.cs.uvarovdp.ex.ValidationException;
import ru.vsu.cs.uvarovdp.validator.BookValidator;
import ru.vsu.cs.uvarovdp.validator.BookValidator.ValidatedData;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Book extends AbstractEntity<UUID> {

    private String title;
    private String author;
    private String isbn;
    private final List<String> genres = new ArrayList<>();
    private BookStatus status;
    private String borrowerName;

    public Book(String title, String author, String isbn, List<String> genres) {
        super(UUID.randomUUID());
        ValidatedData validated = BookValidator.validateAndNormalize(title, author, isbn, genres);
        this.title = validated.title();
        this.author = validated.author();
        this.isbn = validated.isbn();
        this.genres.addAll(validated.genres());
        this.status = BookStatus.AVAILABLE;
        this.borrowerName = null;
    }

    public Book(UUID id, String title, String author, String isbn, List<String> genres,
                BookStatus status, String borrowerName, LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, createdAt, updatedAt);
        ValidatedData validated = BookValidator.validateAndNormalize(title, author, isbn, genres);
        this.title = validated.title();
        this.author = validated.author();
        this.isbn = validated.isbn();
        this.genres.addAll(validated.genres());
        this.status = Objects.requireNonNull(status, "Статус книги не может быть null");
        if (this.status == BookStatus.AVAILABLE) {
            if (borrowerName != null && !borrowerName.isBlank()) {
                throw new ValidationException("У книги в наличии не должно быть читателя");
            }
            this.borrowerName = null;
        } else {
            if (borrowerName == null || borrowerName.isBlank()) {
                throw new ValidationException("Для выданной книги должно быть указано имя читателя");
            }
            this.borrowerName = borrowerName.trim();
        }
    }

    public void setTitle(String title) {
        this.title = title;
        markAsUpdated();
    }

    public void setAuthor(String author) {
        this.author = author;
        markAsUpdated();
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
        markAsUpdated();
    }

    public void setGenres(List<String> genres) {
        List<String> newGenres = new ArrayList<>(genres);
        this.genres.clear();
        this.genres.addAll(newGenres);
        markAsUpdated();
    }

    public void addGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            throw new ValidationException("Название жанра не может быть пустым");
        }
        String trimmed = genre.trim();
        String key = BookValidator.genreKey(trimmed);
        if (this.genres.stream().anyMatch(g -> BookValidator.genreKey(g).equals(key))) {
            throw new ValidationException("Жанр '" + trimmed + "' уже добавлен к книге");
        }
        if (this.genres.size() >= BookConstraints.MAX_GENRES) {
            throw new BusinessRuleException(BusinessRuleType.GENRE_LIMIT_EXCEEDED, "Нельзя добавить больше " + BookConstraints.MAX_GENRES + " жанров");
        }
        this.genres.add(trimmed);
        markAsUpdated();
    }

    public void borrow(String borrowerName) {
        if (this.status != BookStatus.AVAILABLE) {
            throw new BusinessRuleException(BusinessRuleType.BOOK_NOT_AVAILABLE, "Книгу нельзя выдать: текущий статус — " + this.status.getTitle());
        }
        if (borrowerName == null || borrowerName.isBlank()) {
            throw new BusinessRuleException(BusinessRuleType.EMPTY_BORROWER_NAME, "Имя читателя не может быть пустым");
        }
        this.status = BookStatus.BORROWED;
        this.borrowerName = borrowerName.trim();
        markAsUpdated();
    }

    public void giveBack() {
        if (this.status != BookStatus.BORROWED) {
            throw new BusinessRuleException(BusinessRuleType.BOOK_NOT_BORROWED, "Книгу нельзя вернуть: текущий статус — " + this.status.getTitle());
        }
        this.status = BookStatus.AVAILABLE;
        this.borrowerName = null;
        markAsUpdated();
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public List<String> getGenres() {
        return Collections.unmodifiableList(genres);
    }

    public BookStatus getStatus() {
        return status;
    }

    public String getBorrowerName() {
        return borrowerName;
    }

    public boolean isAvailable() {
        return this.status == BookStatus.AVAILABLE;
    }

    @Override
    public String toString() {
        return "Book{" +
                "id=" + getId() +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", isbn='" + isbn + '\'' +
                ", genres=" + genres +
                ", status=" + status +
                ", borrowerName='" + borrowerName + '\'' +
                '}';
    }
}