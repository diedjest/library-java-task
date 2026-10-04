package ru.vsu.cs.uvarov_d_p.ui.command;

import ru.vsu.cs.uvarov_d_p.domain.BookConstraints;
import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.ex.InputClosedException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleHelper {

    public static final int WIDTH = 100;
    public static final String SEPARATOR = "-".repeat(WIDTH);
    public static final String HEADER = "=".repeat(WIDTH);

    private static final int COL_INDEX_WIDTH = 3;
    private static final int COL_AUTHOR_WIDTH = 24;
    private static final int COL_TITLE_WIDTH = 40;
    private static final int COL_ISBN_WIDTH = 13;
    private static final int COL_STATUS_WIDTH = 12;

    private static final String TABLE_FORMAT = "%-" + COL_INDEX_WIDTH + "s %-"
            + COL_AUTHOR_WIDTH + "s %-"
            + COL_TITLE_WIDTH + "s %-"
            + COL_ISBN_WIDTH + "s %-"
            + COL_STATUS_WIDTH + "s";

    private final Scanner scanner;

    public ConsoleHelper(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner, "Scanner не может быть null");
    }

    public void printBooks(List<Book> books, String emptyMessage) {
        if (books == null || books.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }

        System.out.println(SEPARATOR);
        System.out.println(String.format(TABLE_FORMAT, "№", "Автор", "Название", "ISBN", "Статус").stripTrailing());
        System.out.println(SEPARATOR);

        String indent = " ".repeat(COL_INDEX_WIDTH + 1);

        for (int i = 0; i < books.size(); i++) {
            Book book = books.get(i);
            String line1 = String.format(TABLE_FORMAT,
                    i + 1,
                    truncate(book.getAuthor(), COL_AUTHOR_WIDTH),
                    truncate(book.getTitle(), COL_TITLE_WIDTH),
                    truncate(book.getIsbn(), COL_ISBN_WIDTH),
                    truncate(book.getStatus().getTitle(), COL_STATUS_WIDTH)
            ).stripTrailing();
            System.out.println(line1);

            String details = "Жанры: " + String.join(", ", book.getGenres());
            if (!book.isAvailable() && book.getBorrowerName() != null && !book.getBorrowerName().isBlank()) {
                details += " | Читатель: " + book.getBorrowerName();
            }
            printWrappedDetails(details, indent, WIDTH);
        }
        System.out.println(SEPARATOR);
    }

    public UUID selectBookId(List<Book> books, String prompt) {
        String input = readLine(prompt).trim();
        try {
            int index = Integer.parseInt(input);
            if (index < 1 || index > books.size()) {
                System.out.println("⚠ Номер книги вне допустимого диапазона (1-" + books.size() + ")");
                return null;
            }
            return books.get(index - 1).getId();
        } catch (NumberFormatException e) {
            System.out.println("⚠ Введено некорректное число");
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
        while (true) {
            String line = readLine(prompt);
            if (!line.isBlank()) {
                return line.trim();
            }
            System.out.println("⚠ Значение не может быть пустым. Попробуйте снова.");
        }
    }

    public String readWithDefault(String prompt, String defaultValue) {
        String line = readLine(prompt);
        if (line.isBlank()) {
            return defaultValue;
        }
        return line.trim();
    }

    private void printWrappedDetails(String text, String indent, int maxLineWidth) {
        int maxTextWidth = maxLineWidth - indent.length();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (currentLine.isEmpty()) {
                currentLine.append(word);
            } else if (currentLine.length() + 1 + word.length() <= maxTextWidth) {
                currentLine.append(" ").append(word);
            } else {
                System.out.println((indent + currentLine).stripTrailing());
                currentLine.setLength(0);
                currentLine.append(word);
            }
        }
        if (!currentLine.isEmpty()) {
            System.out.println((indent + currentLine).stripTrailing());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() > maxLength) {
            return text.substring(0, maxLength - 1) + "…";
        }
        return text;
    }
}