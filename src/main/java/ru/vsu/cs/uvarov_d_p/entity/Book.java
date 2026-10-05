package ru.vsu.cs.uvarov_d_p.entity;

import ru.vsu.cs.uvarov_d_p.domain.BookConstraints;
import ru.vsu.cs.uvarov_d_p.ex.BusinessRuleException;
import ru.vsu.cs.uvarov_d_p.ex.ValidationException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class Book extends AbstractEntity<UUID> {

    private String title;
    private String author;
    private String isbn;
    private final List<String> genres = new ArrayList<>();
    private BookStatus status;
    private String borrowerName;

    private record ValidatedData(String title, String author, String isbn, List<String> genres) {
    }

    public Book(String title, String author, String isbn, List<String> genres) {
        super(UUID.randomUUID());
        ValidatedData validated = validateAndNormalize(title, author, isbn, genres);
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
        ValidatedData validated = validateAndNormalize(title, author, isbn, genres);
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

    private static ValidatedData validateAndNormalize(String title, String author, String isbn, List<String> genres) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Название книги не может быть пустым");
        }
        if (author == null || author.isBlank()) {
            throw new ValidationException("Автор книги не может быть пустым");
        }
        if (isbn == null || isbn.isBlank()) {
            throw new ValidationException("ISBN книги не может быть пустым");
        }
        if (genres == null) {
            throw new ValidationException("Список жанров не может быть null");
        }

        List<String> trimmedGenres = new ArrayList<>();
        Set<String> uniqueCheck = new HashSet<>();

        for (String genre : genres) {
            if (genre == null || genre.isBlank()) {
                throw new ValidationException("Название жанра не может быть пустым");
            }
            String trimmed = genre.trim();
            if (!uniqueCheck.add(genreKey(trimmed))) {
                throw new ValidationException("Жанры не должны повторяться: " + trimmed);
            }
            trimmedGenres.add(trimmed);
        }

        if (trimmedGenres.isEmpty()) {
            throw new ValidationException("Книга должна содержать хотя бы один жанр");
        }
        if (trimmedGenres.size() > BookConstraints.MAX_GENRES) {
            throw new ValidationException("Количество жанров не может превышать " + BookConstraints.MAX_GENRES);
        }

        return new ValidatedData(title.trim(), author.trim(), isbn.trim(), trimmedGenres);
    }

    private static String genreKey(String genre) {
        return genre.trim().toLowerCase(Locale.ROOT);
    }

    public void update(String title, String author, String isbn, List<String> genres) {
        ValidatedData validated = validateAndNormalize(title, author, isbn, genres);
        this.title = validated.title();
        this.author = validated.author();
        this.isbn = validated.isbn();
        this.genres.clear();
        this.genres.addAll(validated.genres());
        markAsUpdated();
    }

    public void addGenre(String genre) {
        if (genre == null || genre.isBlank()) {
            throw new ValidationException("Название жанра не может быть пустым");
        }
        String trimmed = genre.trim();
        String key = genreKey(trimmed);
        if (this.genres.stream().anyMatch(g -> genreKey(g).equals(key))) {
            throw new ValidationException("Жанр '" + trimmed + "' уже добавлен к книге");
        }
        if (this.genres.size() >= BookConstraints.MAX_GENRES) {
            throw new BusinessRuleException("Нельзя добавить больше " + BookConstraints.MAX_GENRES + " жанров");
        }
        this.genres.add(trimmed);
        markAsUpdated();
    }

    public void borrow(String borrowerName) {
        if (this.status != BookStatus.AVAILABLE) {
            throw new BusinessRuleException("Книгу нельзя выдать: текущий статус — " + this.status.getTitle());
        }
        if (borrowerName == null || borrowerName.isBlank()) {
            throw new BusinessRuleException("Имя читателя не может быть пустым");
        }
        this.status = BookStatus.BORROWED;
        this.borrowerName = borrowerName.trim();
        markAsUpdated();
    }

    public void giveBack() {
        if (this.status != BookStatus.BORROWED) {
            throw new BusinessRuleException("Книгу нельзя вернуть: текущий статус — " + this.status.getTitle());
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