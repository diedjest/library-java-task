package ru.vsu.cs.uvarovdp.ui.command.impl;

import ru.vsu.cs.uvarovdp.domain.BookConstraints;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.command.BookTablePrinter;
import ru.vsu.cs.uvarovdp.ui.command.Command;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleInput;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleOutput;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AddGenreCommand implements Command {

    private final LibraryService service;
    private final ConsoleInput input;
    private final BookTablePrinter printer;

    public AddGenreCommand(LibraryService service, ConsoleInput input, BookTablePrinter printer) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.input = Objects.requireNonNull(input, "ConsoleInput не может быть null");
        this.printer = Objects.requireNonNull(printer, "BookTablePrinter не может быть null");
    }

    @Override
    public void execute() {
        List<Book> books = service.getAllBooks();
        if (books.isEmpty()) {
            ConsoleOutput.printWithGap("Каталог пуст.");
            return;
        }

        printer.printBooks(books, "Каталог пуст.");
        UUID bookId = input.selectBookId(books, "Выберите номер книги для добавления жанра: ");
        if (bookId == null) {
            ConsoleOutput.printBlankLine();
            return;
        }

        Book book = service.getBookById(bookId);
        System.out.println("Текущие жанры (" + book.getGenres().size() + "/" + BookConstraints.MAX_GENRES + "): "
                + String.join(", ", book.getGenres()));

        if (book.getGenres().size() >= BookConstraints.MAX_GENRES) {
            ConsoleOutput.printWithGap("Достигнут лимит жанров для этой книги.");
            return;
        }

        String newGenre = input.readNonBlank("Введите новый жанр: ");
        Book updated = service.addGenre(bookId, newGenre);
        ConsoleOutput.printWithGap("Жанр добавлен. Текущие жанры: " + String.join(", ", updated.getGenres()));
    }
}