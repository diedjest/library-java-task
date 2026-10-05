package ru.vsu.cs.uvarov_d_p.ui.command;

import ru.vsu.cs.uvarov_d_p.domain.BookConstraints;
import ru.vsu.cs.uvarov_d_p.ex.AppException;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BookInputReader {

    private final LibraryService service;
    private final ConsoleHelper helper;

    public BookInputReader(LibraryService service, ConsoleHelper helper) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.helper = Objects.requireNonNull(helper, "ConsoleHelper не может быть null");
    }

    public String readIsbnForAdd(String defaultIsbn) {
        while (true) {
            String input;
            if (defaultIsbn == null) {
                input = helper.readLine("Введите ISBN (Enter — отмена): ").trim();
                if (input.isEmpty()) {
                    return null;
                }
            } else {
                input = helper.readWithDefault("Введите ISBN [" + defaultIsbn + "]: ", defaultIsbn);
            }

            try {
                service.checkIsbnAvailable(input, null);
                return input;
            } catch (AppException e) {
                System.out.println("✗ Ошибка: " + e.getMessage());
            }
        }
    }

    public String readIsbnForEdit(String defaultIsbn, UUID bookId) {
        while (true) {
            String input = helper.readWithDefault("Новый ISBN [" + defaultIsbn + "]: ", defaultIsbn);
            try {
                service.checkIsbnAvailable(input, bookId);
                return input;
            } catch (AppException e) {
                System.out.println("✗ Ошибка: " + e.getMessage());
            }
        }
    }

    public List<String> readGenres(List<String> defaultGenres) {
        if (defaultGenres != null && !defaultGenres.isEmpty()) {
            System.out.println("Введите новые жанры (Enter для сохранения текущих: " + String.join(", ", defaultGenres) + "):");
            String first = helper.readLine("Жанр 1: ").trim();
            if (first.isEmpty()) {
                return defaultGenres;
            }
            List<String> genres = new ArrayList<>();
            genres.add(first);
            for (int i = 2; i <= BookConstraints.MAX_GENRES; i++) {
                String next = helper.readLine("Жанр " + i + " (Enter, чтобы завершить): ").trim();
                if (next.isEmpty()) {
                    break;
                }
                genres.add(next);
            }
            return genres;
        }
        return helper.readGenres();
    }
}