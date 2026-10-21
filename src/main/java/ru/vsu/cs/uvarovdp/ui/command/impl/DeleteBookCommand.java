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

public class DeleteBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleInput input;
    private final BookTablePrinter printer;

    public DeleteBookCommand(LibraryService service, ConsoleInput input, BookTablePrinter printer) {
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
        UUID bookId = input.selectBookId(books, "Выберите номер книги для удаления: ");
        if (bookId == null) {
            ConsoleOutput.printBlankLine();
            return;
        }

        Book book = service.getBookById(bookId);
        if (input.confirm("Удалить «" + book.getTitle() + "»? (да/нет): ")) {
            service.deleteBook(bookId);
            System.out.println("Книга удалена");
        } else {
            System.out.println("Удаление отменено");
        }
        ConsoleOutput.printBlankLine();
    }
}