package ru.vsu.cs.uvarovdp.ui.command;

import ru.vsu.cs.uvarovdp.entity.Book;

import java.util.List;

public class BookTablePrinter {

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

    public void printBooks(List<Book> books, String emptyMessage) {
        if (books == null || books.isEmpty()) {
            System.out.println(emptyMessage);
            return;
        }

        System.out.println(ConsoleOutput.SEPARATOR);
        System.out.println(String.format(TABLE_FORMAT, "№", "Автор", "Название", "ISBN", "Статус").stripTrailing());
        System.out.println(ConsoleOutput.SEPARATOR);

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
            printWrappedDetails(details, indent, ConsoleOutput.WIDTH);
        }
        System.out.println(ConsoleOutput.SEPARATOR);
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
