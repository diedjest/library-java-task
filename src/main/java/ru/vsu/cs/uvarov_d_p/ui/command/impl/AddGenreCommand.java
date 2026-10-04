package ru.vsu.cs.uvarov_d_p.ui.command.impl;

import ru.vsu.cs.uvarov_d_p.domain.BookConstraints;
import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.ui.command.Command;
import ru.vsu.cs.uvarov_d_p.ui.command.ConsoleHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AddGenreCommand implements Command {

    private final LibraryService service;
    private final ConsoleHelper helper;

    public AddGenreCommand(LibraryService service, ConsoleHelper helper) {
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
        UUID bookId = helper.selectBookId(books, "Выберите номер книги для добавления жанра: ");
        if (bookId == null) {
            System.out.println();
            return;
        }

        Book book = service.getBookById(bookId);
        System.out.println("Текущие жанры (" + book.getGenres().size() + "/" + BookConstraints.MAX_GENRES + "): "
                + String.join(", ", book.getGenres()));

        if (book.getGenres().size() >= BookConstraints.MAX_GENRES) {
            System.out.println("⚠ Достигнут лимит жанров для этой книги.");
            System.out.println();
            return;
        }

        String newGenre = helper.readNonBlank("Введите новый жанр: ");
        Book updated = service.addGenre(bookId, newGenre);
        System.out.println("✓ Жанр добавлен. Текущие жанры: " + String.join(", ", updated.getGenres()));
        System.out.println();
    }
}