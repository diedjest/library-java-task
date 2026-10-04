package ru.vsu.cs.uvarov_d_p.ui;

import ru.vsu.cs.uvarov_d_p.ex.AppException;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.ui.command.Command;
import ru.vsu.cs.uvarov_d_p.ui.command.ConsoleHelper;
import ru.vsu.cs.uvarov_d_p.ex.InputClosedException;
import ru.vsu.cs.uvarov_d_p.ui.command.impl.AddBookCommand;
import ru.vsu.cs.uvarov_d_p.ui.command.impl.AddGenreCommand;
import ru.vsu.cs.uvarov_d_p.ui.command.impl.BorrowBookCommand;
import ru.vsu.cs.uvarov_d_p.ui.command.impl.DeleteBookCommand;
import ru.vsu.cs.uvarov_d_p.ui.command.impl.EditBookCommand;
import ru.vsu.cs.uvarov_d_p.ui.command.impl.ReturnBookCommand;
import ru.vsu.cs.uvarov_d_p.ui.command.impl.SearchByAuthorCommand;
import ru.vsu.cs.uvarov_d_p.ui.command.impl.SearchByIsbnCommand;
import ru.vsu.cs.uvarov_d_p.ui.command.impl.ShowAllCommand;

import java.util.Map;
import java.util.Scanner;

public class ConsoleUI {

    private final ConsoleHelper helper;
    private final Map<String, Command> commands;

    public ConsoleUI(LibraryService service) {
        this.helper = new ConsoleHelper(new Scanner(System.in));
        this.commands = Map.of(
                "1", new ShowAllCommand(service, helper),
                "2", new AddBookCommand(service, helper),
                "3", new EditBookCommand(service, helper),
                "4", new DeleteBookCommand(service, helper),
                "5", new SearchByAuthorCommand(service, helper),
                "6", new SearchByIsbnCommand(service, helper),
                "7", new AddGenreCommand(service, helper),
                "8", new BorrowBookCommand(service, helper),
                "9", new ReturnBookCommand(service, helper)
        );
    }

    public void start() {
        System.out.println(ConsoleHelper.HEADER);
        System.out.println("         КАТАЛОГ ДОМАШНЕЙ БИБЛИОТЕКИ");
        System.out.println(ConsoleHelper.HEADER);

        try {
            while (true) {
                printMenu();
                String choice = helper.readLine("Выберите пункт меню: ").trim();
                if ("0".equals(choice)) {
                    System.out.println("До свидания!");
                    break;
                }

                Command command = commands.get(choice);
                if (command == null) {
                    System.out.println("⚠ Неверный пункт меню");
                    continue;
                }

                try {
                    command.execute();
                } catch (AppException e) {
                    System.out.println("✗ Ошибка: " + describe(e));
                } catch (InputClosedException e) {
                    throw e;
                } catch (RuntimeException e) {
                    System.out.println("✗ Непредвиденная ошибка: " + describe(e));
                }
            }
        } catch (InputClosedException e) {
            System.out.println("\nВвод завершён. До свидания!");
        }
    }

    private static String describe(Throwable e) {
        if (e == null) {
            return "";
        }
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        return e.getClass().getSimpleName();
    }

    private void printMenu() {
        System.out.println("\n" + ConsoleHelper.SEPARATOR);
        System.out.println("1. Показать все книги");
        System.out.println("2. Добавить книгу");
        System.out.println("3. Редактировать книгу");
        System.out.println("4. Удалить книгу");
        System.out.println("5. Поиск по автору");
        System.out.println("6. Поиск по ISBN");
        System.out.println("7. Добавить жанр к книге");
        System.out.println("8. Выдать книгу читателю");
        System.out.println("9. Принять книгу обратно");
        System.out.println("0. Выход");
        System.out.println(ConsoleHelper.SEPARATOR);
    }
}