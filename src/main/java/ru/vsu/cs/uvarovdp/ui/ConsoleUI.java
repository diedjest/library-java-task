package ru.vsu.cs.uvarovdp.ui;

import ru.vsu.cs.uvarovdp.ex.AppException;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.command.Command;
import ru.vsu.cs.uvarovdp.ui.command.BookTablePrinter;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleInput;
import ru.vsu.cs.uvarovdp.ui.command.ConsoleOutput;
import ru.vsu.cs.uvarovdp.ex.InputClosedException;
import ru.vsu.cs.uvarovdp.ui.command.impl.AddBookCommand;
import ru.vsu.cs.uvarovdp.ui.command.impl.AddGenreCommand;
import ru.vsu.cs.uvarovdp.ui.command.impl.BorrowBookCommand;
import ru.vsu.cs.uvarovdp.ui.command.impl.DeleteBookCommand;
import ru.vsu.cs.uvarovdp.ui.command.impl.EditBookCommand;
import ru.vsu.cs.uvarovdp.ui.command.impl.ReturnBookCommand;
import ru.vsu.cs.uvarovdp.ui.command.impl.SearchByAuthorCommand;
import ru.vsu.cs.uvarovdp.ui.command.impl.SearchByIsbnCommand;
import ru.vsu.cs.uvarovdp.ui.command.impl.ShowAllCommand;

import java.util.Map;
import java.util.Scanner;

public class ConsoleUI {

    private static final String SHOW_ALL = "show";
    private static final String ADD_BOOK = "add";
    private static final String EDIT_BOOK = "edit";
    private static final String DELETE_BOOK = "delete";
    private static final String SEARCH_AUTHOR = "author";
    private static final String SEARCH_ISBN = "isbn";
    private static final String ADD_GENRE = "genre";
    private static final String BORROW_BOOK = "borrow";
    private static final String RETURN_BOOK = "return";
    private static final String EXIT = "exit";
    private static final String EMPTY_DESCRIPTION = "";

    private final ConsoleInput input;
    private final Map<String, Command> commands;

    public ConsoleUI(LibraryService service) {
        this.input = new ConsoleInput(new Scanner(System.in));
        BookTablePrinter printer = new BookTablePrinter();
        this.commands = Map.of(
                SHOW_ALL, new ShowAllCommand(service, printer),
                ADD_BOOK, new AddBookCommand(service, input),
                EDIT_BOOK, new EditBookCommand(service, input, printer),
                DELETE_BOOK, new DeleteBookCommand(service, input, printer),
                SEARCH_AUTHOR, new SearchByAuthorCommand(service, input, printer),
                SEARCH_ISBN, new SearchByIsbnCommand(service, input, printer),
                ADD_GENRE, new AddGenreCommand(service, input, printer),
                BORROW_BOOK, new BorrowBookCommand(service, input, printer),
                RETURN_BOOK, new ReturnBookCommand(service, input, printer)
        );
    }

    public void start() {
        System.out.println(ConsoleOutput.HEADER);
        System.out.println("         КАТАЛОГ ДОМАШНЕЙ БИБЛИОТЕКИ");
        System.out.println(ConsoleOutput.HEADER);

        try {
            boolean running = true;
            while (running) {
                printMenu();
                String choice = input.readLine("Выберите пункт меню: ").trim();
                if (EXIT.equals(choice)) {
                    System.out.println("Выход...");
                    running = false;
                } else {
                    handleChoice(choice);
                }
            }
        } catch (InputClosedException e) {
            System.out.println("\nВвод завершён. Выход...");
        }
    }

    private void handleChoice(String choice) {
        Command command = commands.get(choice);
        if (command == null) {
            System.out.println("Неверный пункт меню");
            return;
        }

        try {
            command.execute();
        } catch (AppException e) {
            System.out.println("Ошибка: " + describe(e));
        } catch (InputClosedException e) {
            throw e;
        } catch (RuntimeException e) {
            System.out.println("Непредвиденная ошибка: " + describe(e));
        }
    }

    private static String describe(Throwable e) {
        if (e == null) {
            return EMPTY_DESCRIPTION;
        }
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return e.getClass().getSimpleName();
    }

    private void printMenu() {
        System.out.println("\n" + ConsoleOutput.SEPARATOR);
        printMenuItem(SHOW_ALL, "Показать все книги");
        printMenuItem(ADD_BOOK, "Добавить книгу");
        printMenuItem(EDIT_BOOK, "Редактировать книгу");
        printMenuItem(DELETE_BOOK, "Удалить книгу");
        printMenuItem(SEARCH_AUTHOR, "Поиск по автору");
        printMenuItem(SEARCH_ISBN, "Поиск по ISBN");
        printMenuItem(ADD_GENRE, "Добавить жанр к книге");
        printMenuItem(BORROW_BOOK, "Выдать книгу читателю");
        printMenuItem(RETURN_BOOK, "Принять книгу обратно");
        printMenuItem(EXIT, "Выход");
        System.out.println(ConsoleOutput.SEPARATOR);
    }

    private static void printMenuItem(String prefix, String title) {
        System.out.println(prefix + ". " + title);
    }
}
