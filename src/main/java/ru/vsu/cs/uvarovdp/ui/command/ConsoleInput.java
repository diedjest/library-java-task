package ru.vsu.cs.uvarovdp.ui.command;

import ru.vsu.cs.uvarovdp.domain.BookConstraints;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.ex.InputClosedException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "Scanner не может быть null");
    }

    public UUID selectBookId(List<Book> books, String prompt) {
        String input = readLine(prompt).trim();
        try {
            int index = Integer.parseInt(input);
            if (index < 1 || index > books.size()) {
                System.out.println("Номер книги вне допустимого диапазона (1-" + books.size() + ")");
                return null;
            }
            return books.get(index - 1).getId();
        } catch (NumberFormatException e) {
            System.out.println("Введено некорректное число");
            return null;
        }
    }

    public List<String> readGenres() {
        List<String> genres = new ArrayList<>();
        System.out.println("Введите жанры (от 1 до " + BookConstraints.MAX_GENRES + "):");
        String first = readNonBlank("Жанр 1: ");
        genres.add(first);

        for (int i = 2; i <= BookConstraints.MAX_GENRES; i++) {
            String next = readLine("Жанр " + i + " (Enter, чтобы пропустить): ").trim();
            if (next.isEmpty()) {
                break;
            }
            genres.add(next);
        }
        return genres;
    }

    public boolean confirm(String prompt) {
        String answer = readLine(prompt).trim().toLowerCase();
        return answer.equals("да") || answer.equals("д") || answer.equals("yes") || answer.equals("y");
    }

    public String readLine(String prompt) {
        if (prompt != null && !prompt.isEmpty()) {
            System.out.print(prompt);
        }
        try {
            if (!scanner.hasNextLine()) {
                throw new InputClosedException();
            }
            return scanner.nextLine();
        } catch (NoSuchElementException | IllegalStateException e) {
            throw new InputClosedException();
        }
    }

    public String readNonBlank(String prompt) {
        String line = readLine(prompt);
        while (line.isBlank()) {
            System.out.println("Значение не может быть пустым. Попробуйте снова.");
            line = readLine(prompt);
        }
        return line.trim();
    }

    public String readWithDefault(String prompt, String defaultValue) {
        String line = readLine(prompt);
        if (line.isBlank()) {
            return defaultValue;
        }
        return line.trim();
    }
}
