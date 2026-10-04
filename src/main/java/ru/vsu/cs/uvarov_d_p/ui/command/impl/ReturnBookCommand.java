package ru.vsu.cs.uvarov_d_p.ui.command.impl;

import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.ui.command.Command;
import ru.vsu.cs.uvarov_d_p.ui.command.ConsoleHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ReturnBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleHelper helper;

    public ReturnBookCommand(LibraryService service, ConsoleHelper helper) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.helper = Objects.requireNonNull(helper, "ConsoleHelper не может быть null");
    }

    @Override
    public void execute() {
        List<Book> borrowedBooks = service.getAllBooks().stream()
                .filter(book -> !book.isAvailable())
                .toList();

        if (borrowedBooks.isEmpty()) {
            System.out.println("Нет выданных книг.");
            System.out.println();
            return;
        }

        helper.printBooks(borrowedBooks, "Нет выданных книг.");
        UUID bookId = helper.selectBookId(borrowedBooks, "Выберите номер книги для возврата: ");
        if (bookId == null) {
            System.out.println();
            return;
        }

        Book book = service.getBookById(bookId);
        if (helper.confirm("Принять «" + book.getTitle() + "» от читателя " + book.getBorrowerName() + "? (да/нет): ")) {
            service.returnBook(bookId);
            System.out.println("✓ Книга возвращена в каталог");
        } else {
            System.out.println("Возврат отменён");
        }
        System.out.println();
    }
}