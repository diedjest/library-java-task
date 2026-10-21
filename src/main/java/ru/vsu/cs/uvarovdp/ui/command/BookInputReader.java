package ru.vsu.cs.uvarovdp.ui.command;

import ru.vsu.cs.uvarovdp.domain.BookConstraints;
import ru.vsu.cs.uvarovdp.ex.AppException;
import ru.vsu.cs.uvarovdp.service.LibraryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BookInputReader {

    private final LibraryService service;
    private final ConsoleInput consoleInput;

    public BookInputReader(LibraryService service, ConsoleInput input) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.consoleInput = Objects.requireNonNull(input, "ConsoleInput не может быть null");
    }

    public String readIsbnForAdd(String defaultIsbn) {
        String isbn = readIsbnOrCancel(defaultIsbn);
        while (isbn != null && !isIsbnAvailable(isbn, null)) {
            isbn = readIsbnOrCancel(defaultIsbn);
        }
        return isbn;
    }

    public String readIsbnForEdit(String defaultIsbn, UUID bookId) {
        String isbn = readEditedIsbn(defaultIsbn);
        while (!isIsbnAvailable(isbn, bookId)) {
            isbn = readEditedIsbn(defaultIsbn);
        }
        return isbn;
    }

    private String readIsbnOrCancel(String defaultIsbn) {
        if (defaultIsbn != null) {
            return consoleInput.readWithDefault("Введите ISBN [" + defaultIsbn + "]: ", defaultIsbn);
        }
        String isbn = consoleInput.readLine("Введите ISBN (Enter — отмена): ").trim();
        return isbn.isEmpty() ? null : isbn;
    }

    private String readEditedIsbn(String defaultIsbn) {
        return consoleInput.readWithDefault("Новый ISBN [" + defaultIsbn + "]: ", defaultIsbn);
    }

    private boolean isIsbnAvailable(String isbn, UUID excludeBookId) {
        try {
            service.checkIsbnAvailable(isbn, excludeBookId);
            return true;
        } catch (AppException e) {
            System.out.println("✗ Ошибка: " + e.getMessage());
            return false;
        }
    }

    public List<String> readGenres(List<String> defaultGenres) {
        if (defaultGenres != null && !defaultGenres.isEmpty()) {
            System.out.println("Введите новые жанры (Enter для сохранения текущих: " + String.join(", ", defaultGenres) + "):");
            String first = consoleInput.readLine("Жанр 1: ").trim();
            if (first.isEmpty()) {
                return defaultGenres;
            }
            List<String> genres = new ArrayList<>();
            genres.add(first);
            for (int i = 2; i <= BookConstraints.MAX_GENRES; i++) {
                String next = consoleInput.readLine("Жанр " + i + " (Enter, чтобы завершить): ").trim();
                if (next.isEmpty()) {
                    break;
                }
                genres.add(next);
            }
            return genres;
        }
        return consoleInput.readGenres();
    }
}