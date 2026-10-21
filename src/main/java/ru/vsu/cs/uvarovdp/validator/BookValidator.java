package ru.vsu.cs.uvarovdp.validator;

import ru.vsu.cs.uvarovdp.domain.BookConstraints;
import ru.vsu.cs.uvarovdp.entity.BookStatus;
import ru.vsu.cs.uvarovdp.ex.ValidationException;
import ru.vsu.cs.uvarovdp.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class BookValidator {

    public record ValidatedData(String title, String author, String isbn, List<String> genres) {
    }

    private BookValidator() {
    }

    public static ValidatedData validateAndNormalize(String title, String author, String isbn, List<String> genres) {
        validateRequiredFields(title, author, isbn, genres);
        List<String> trimmedGenres = normalizeGenres(genres);
        return new ValidatedData(title.trim(), author.trim(), isbn.trim(), trimmedGenres);
    }

    public static String validateBorrower(BookStatus status, String borrowerName) {
        if (status == BookStatus.AVAILABLE) {
            if (!StringUtils.isNullOrBlank(borrowerName)) {
                throw new ValidationException("У книги в наличии не должно быть читателя");
            }
            return null;
        }
        if (StringUtils.isNullOrBlank(borrowerName)) {
            throw new ValidationException("Для выданной книги должно быть указано имя читателя");
        }
        return borrowerName.trim();
    }

    public static String genreKey(String genre) {
        return genre.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateRequiredFields(String title, String author, String isbn, List<String> genres) {
        if (StringUtils.isNullOrBlank(title)) {
            throw new ValidationException("Название книги не может быть пустым");
        }
        if (StringUtils.isNullOrBlank(author)) {
            throw new ValidationException("Автор книги не может быть пустым");
        }
        if (StringUtils.isNullOrBlank(isbn)) {
            throw new ValidationException("ISBN книги не может быть пустым");
        }
        if (genres == null) {
            throw new ValidationException("Список жанров не может быть null");
        }
    }

    private static List<String> normalizeGenres(List<String> genres) {
        List<String> trimmedGenres = new ArrayList<>();
        Set<String> uniqueCheck = new HashSet<>();

        for (String genre : genres) {
            if (StringUtils.isNullOrBlank(genre)) {
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
        return trimmedGenres;
    }
}
