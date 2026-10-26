package ru.vsu.cs.uvarovdp.ui.command.impl;

import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.command.BookTablePrinter;
import ru.vsu.cs.uvarovdp.ui.command.Command;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleInput;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleOutput;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BorrowBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleInput input;
    private final BookTablePrinter printer;

    public BorrowBookCommand(LibraryService service, ConsoleInput input, BookTablePrinter printer) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.input = Objects.requireNonNull(input, "ConsoleInput не может быть null");
        this.printer = Objects.requireNonNull(printer, "BookTablePrinter не может быть null");
    }

    @Override
    public void execute() {
        List<Book> availableBooks = service.getAllBooks().stream()
                .filter(Book::isAvailable)
                .toList();

        if (availableBooks.isEmpty()) {
            ConsoleOutput.printWithGap("Нет книг в наличии.");
            return;
        }

        printer.printBooks(availableBooks, "Нет книг в наличии.");
        UUID bookId = input.selectBookId(availableBooks, "Выберите номер книги для выдачи: ");
        if (bookId == null) {
            ConsoleOutput.printBlankLine();
            return;
        }

        String borrowerName = input.readNonBlank("Кому выдать (имя читателя): ");
        Book book = service.borrowBook(bookId, borrowerName);
        ConsoleOutput.printWithGap("Книга выдана: " + book.getAuthor() + " — «" + book.getTitle() + "» читателю " + book.getBorrowerName());
    }
}