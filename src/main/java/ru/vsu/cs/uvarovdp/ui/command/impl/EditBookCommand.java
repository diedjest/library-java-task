package ru.vsu.cs.uvarovdp.ui.command.impl;

import ru.vsu.cs.uvarovdp.dto.BookData;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.ex.AppException;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.command.BookInputReader;
import ru.vsu.cs.uvarovdp.ui.command.BookTablePrinter;
import ru.vsu.cs.uvarovdp.ui.command.Command;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleInput;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleOutput;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class EditBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleInput input;
    private final BookTablePrinter printer;
    private final BookInputReader reader;

    public EditBookCommand(LibraryService service, ConsoleInput input, BookTablePrinter printer) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.input = Objects.requireNonNull(input, "ConsoleInput не может быть null");
        this.printer = Objects.requireNonNull(printer, "BookTablePrinter не может быть null");
        this.reader = new BookInputReader(service, input);
    }

    @Override
    public void execute() {
        List<Book> books = service.getAllBooks();
        if (books.isEmpty()) {
            ConsoleOutput.printWithGap("Каталог пуст.");
            return;
        }

        printer.printBooks(books, "Каталог пуст.");
        UUID bookId = input.selectBookId(books, "Выберите номер книги для редактирования: ");
        if (bookId == null) {
            ConsoleOutput.printBlankLine();
            return;
        }

        Book current = service.getBookById(bookId);
        printCurrentData(current);
        editUntilFinished(bookId, current);
    }

    private void printCurrentData(Book book) {
        System.out.println("Текущие данные книги:");
        System.out.println("Автор: " + book.getAuthor());
        System.out.println("Название: " + book.getTitle());
        System.out.println("ISBN: " + book.getIsbn());
        System.out.println("Жанры: " + String.join(", ", book.getGenres()));
    }

    private void editUntilFinished(UUID bookId, Book current) {
        BookData data = new BookData(current.getTitle(), current.getAuthor(), current.getIsbn(), current.getGenres());

        boolean finished = false;
        while (!finished) {
            data = readNewData(bookId, data);
            finished = trySave(bookId, data);
        }
    }

    private BookData readNewData(UUID bookId, BookData defaults) {
        String author = input.readWithDefault("Новый автор [" + defaults.author() + "]: ", defaults.author());
        String title = input.readWithDefault("Новое название [" + defaults.title() + "]: ", defaults.title());
        String isbn = reader.readIsbnForEdit(defaults.isbn(), bookId);
        List<String> genres = reader.readGenres(defaults.genres());
        return new BookData(title, author, isbn, genres);
    }

    private boolean trySave(UUID bookId, BookData data) {
        try {
            service.editBook(bookId, data);
            ConsoleOutput.printWithGap("Книга обновлена");
            return true;
        } catch (AppException e) {
            System.out.println("✗ Ошибка: " + e.getMessage());
            if (input.confirm("Исправить данные и повторить? (да/нет): ")) {
                return false;
            }
            ConsoleOutput.printWithGap("Редактирование отменено");
            return true;
        }
    }
}
