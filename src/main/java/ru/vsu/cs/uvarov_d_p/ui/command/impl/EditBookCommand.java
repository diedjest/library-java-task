package ru.vsu.cs.uvarov_d_p.ui.command.impl;

import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.ex.AppException;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.ui.command.BookInputReader;
import ru.vsu.cs.uvarov_d_p.ui.command.Command;
import ru.vsu.cs.uvarov_d_p.ui.command.ConsoleHelper;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class EditBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleHelper helper;
    private final BookInputReader reader;

    public EditBookCommand(LibraryService service, ConsoleHelper helper) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.helper = Objects.requireNonNull(helper, "ConsoleHelper не может быть null");
        this.reader = new BookInputReader(service, helper);
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
        UUID bookId = helper.selectBookId(books, "Выберите номер книги для редактирования: ");
        if (bookId == null) {
            System.out.println();
            return;
        }

        Book current = service.getBookById(bookId);
        System.out.println("Текущие данные книги:");
        System.out.println("Автор: " + current.getAuthor());
        System.out.println("Название: " + current.getTitle());
        System.out.println("ISBN: " + current.getIsbn());
        System.out.println("Жанры: " + String.join(", ", current.getGenres()));

        String author = current.getAuthor();
        String title = current.getTitle();
        String isbn = current.getIsbn();
        List<String> genres = current.getGenres();

        while (true) {
            author = helper.readWithDefault("Новый автор [" + author + "]: ", author);
            title = helper.readWithDefault("Новое название [" + title + "]: ", title);
            isbn = reader.readIsbnForEdit(isbn, bookId);
            genres = reader.readGenres(genres);

            try {
                service.editBook(bookId, title, author, isbn, genres);
                System.out.println("Книга обновлена");
                System.out.println();
                return;
            } catch (AppException e) {
                System.out.println("✗ Ошибка: " + e.getMessage());
                if (!helper.confirm("Исправить данные и повторить? (да/нет): ")) {
                    System.out.println("Редактирование отменено");
                    System.out.println();
                    return;
                }
            }
        }
    }
}