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

public class ReturnBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleInput input;
    private final BookTablePrinter printer;

    public ReturnBookCommand(LibraryService service, ConsoleInput input, BookTablePrinter printer) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.input = Objects.requireNonNull(input, "ConsoleInput не может быть null");
        this.printer = Objects.requireNonNull(printer, "BookTablePrinter не может быть null");
    }

    @Override
    public void execute() {
        List<Book> borrowedBooks = service.getAllBooks().stream()
                .filter(book -> !book.isAvailable())
                .toList();

        if (borrowedBooks.isEmpty()) {
            ConsoleOutput.printWithGap("Нет выданных книг.");
            return;
        }

        printer.printBooks(borrowedBooks, "Нет выданных книг.");
        UUID bookId = input.selectBookId(borrowedBooks, "Выберите номер книги для возврата: ");
        if (bookId == null) {
            ConsoleOutput.printBlankLine();
            return;
        }

        Book book = service.getBookById(bookId);
        if (input.confirm("Принять «" + book.getTitle() + "» от читателя " + book.getBorrowerName() + "? (да/нет): ")) {
            service.returnBook(bookId);
            System.out.println("Книга возвращена в каталог");
        } else {
            System.out.println("Возврат отменён");
        }
        ConsoleOutput.printBlankLine();
    }
}