package ru.vsu.cs.uvarovdp.ui.command.impl;

import ru.vsu.cs.uvarovdp.dto.BookData;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.ex.AppException;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.command.BookInputReader;
import ru.vsu.cs.uvarovdp.ui.command.Command;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleInput;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleOutput;

import java.util.List;
import java.util.Objects;

public class AddBookCommand implements Command {

    private final LibraryService service;
    private final ConsoleInput input;
    private final BookInputReader reader;

    public AddBookCommand(LibraryService service, ConsoleInput input) {
        this.service = Objects.requireNonNull(service, "Сервис не может быть null");
        this.input = Objects.requireNonNull(input, "ConsoleInput не может быть null");
        this.reader = new BookInputReader(service, input);
    }

    @Override
    public void execute() {
        String author = null;
        String title = null;
        String isbn = null;
        List<String> genres = null;

        boolean finished = false;
        while (!finished) {
            author = readAuthor(author);
            title = readTitle(title);

            isbn = reader.readIsbnForAdd(isbn);
            if (isbn == null) {
                ConsoleOutput.printWithGap("Добавление отменено");
                finished = true;
            } else {
                genres = reader.readGenres(genres);
                finished = trySave(new BookData(title, author, isbn, genres));
            }
        }
    }

    private String readAuthor(String current) {
        if (current == null) {
            return input.readNonBlank("Введите автора: ");
        }
        return input.readWithDefault("Введите автора [" + current + "]: ", current);
    }

    private String readTitle(String current) {
        if (current == null) {
            return input.readNonBlank("Введите название: ");
        }
        return input.readWithDefault("Введите название [" + current + "]: ", current);
    }

    private boolean trySave(BookData data) {
        try {
            Book book = service.addBook(data);
            ConsoleOutput.printWithGap("Книга добавлена: " + book.getAuthor() + " — «" + book.getTitle() + "»");
            return true;
        } catch (AppException e) {
            System.out.println("✗ Ошибка: " + e.getMessage());
            if (input.confirm("Исправить данные и повторить? (да/нет): ")) {
                return false;
            }
            ConsoleOutput.printWithGap("Добавление отменено");
            return true;
        }
    }
}
