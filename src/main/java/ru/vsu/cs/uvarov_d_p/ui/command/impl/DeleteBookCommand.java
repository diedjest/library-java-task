package ru.vsu.cs.uvarov_d_p.ui.command.impl;

import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.ui.command.Command;
import ru.vsu.cs.uvarov_d_p.ui.command.ConsoleHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class DeleteBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleHelper helper;

    public DeleteBookCommand(LibraryService service, ConsoleHelper helper) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.helper = Objects.requireNonNull(helper, "ConsoleHelper не может быть null");
    }

    @Override
    public void execute() {
        List<Book> books = service.getAllBooks();
        if (books.isEmpty()) {
            System.out.println("Каталог пуст.");
            System.out.println();
            return;
        }

        helper.printBooks(books, "Каталог пуст.");
        UUID bookId = helper.selectBookId(books, "Выберите номер книги для удаления: ");
        if (bookId == null) {
            System.out.println();
            return;
        }

        Book book = service.getBookById(bookId);
        if (helper.confirm("Удалить «" + book.getTitle() + "»? (да/нет): ")) {
            service.deleteBook(bookId);
            System.out.println("Книга удалена");
        } else {
            System.out.println("Удаление отменено");
        }
        System.out.println();
    }
}