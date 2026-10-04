package ru.vsu.cs.uvarov_d_p.ui.command.impl;

import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.ex.AppException;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.ui.command.BookInputReader;
import ru.vsu.cs.uvarov_d_p.ui.command.Command;
import ru.vsu.cs.uvarov_d_p.ui.command.ConsoleHelper;

import java.util.List;
import java.util.Objects;

public class AddBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleHelper helper;
    private final BookInputReader reader;

    public AddBookCommand(LibraryService service, ConsoleHelper helper) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.helper = Objects.requireNonNull(helper, "ConsoleHelper не может быть null");
        this.reader = new BookInputReader(service, helper);
    }

    @Override
    public void execute() {
        String author = null;
        String title = null;
        String isbn = null;
        List<String> genres = null;

        while (true) {
            if (author == null) {
                author = helper.readNonBlank("Введите автора: ");
            } else {
                author = helper.readWithDefault("Введите автора [" + author + "]: ", author);
            }

            if (title == null) {
                title = helper.readNonBlank("Введите название: ");
            } else {
                title = helper.readWithDefault("Введите название [" + title + "]: ", title);
            }

            isbn = reader.readIsbnForAdd(isbn);
            if (isbn == null) {
                System.out.println("Добавление отменено");
                System.out.println();
                return;
            }

            genres = reader.readGenres(genres);

            try {
                Book book = service.addBook(title, author, isbn, genres);
                System.out.println("✓ Книга добавлена: " + book.getAuthor() + " — «" + book.getTitle() + "»");
                System.out.println();
                return;
            } catch (AppException e) {
                System.out.println("✗ Ошибка: " + e.getMessage());
                if (!helper.confirm("Исправить данные и повторить? (да/нет): ")) {
                    System.out.println("Добавление отменено");
                    System.out.println();
                    return;
                }
            }
        }
    }
}