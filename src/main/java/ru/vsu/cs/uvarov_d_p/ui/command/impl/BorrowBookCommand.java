package ru.vsu.cs.uvarov_d_p.ui.command.impl;

import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.ui.command.Command;
import ru.vsu.cs.uvarov_d_p.ui.command.ConsoleHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BorrowBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleHelper helper;

    public BorrowBookCommand(LibraryService service, ConsoleHelper helper) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.helper = Objects.requireNonNull(helper, "ConsoleHelper не может быть null");
    }

    @Override
    public void execute() {
        List<Book> availableBooks = service.getAllBooks().stream()
                .filter(Book::isAvailable)
                .toList();

        if (availableBooks.isEmpty()) {
            System.out.println("Нет книг в наличии.");
            System.out.println();
            return;
        }

        helper.printBooks(availableBooks, "Нет книг в наличии.");
        UUID bookId = helper.selectBookId(availableBooks, "Выберите номер книги для выдачи: ");
        if (bookId == null) {
            System.out.println();
            return;
        }

        String borrowerName = helper.readNonBlank("Кому выдать (имя читателя): ");
        Book book = service.borrowBook(bookId, borrowerName);
        System.out.println("Книга выдана: " + book.getAuthor() + " — «" + book.getTitle() + "» читателю " + book.getBorrowerName());
        System.out.println();
    }
}