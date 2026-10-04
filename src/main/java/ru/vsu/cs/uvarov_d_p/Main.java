package ru.vsu.cs.uvarov_d_p;

import ru.vsu.cs.uvarov_d_p.entity.Book;
import ru.vsu.cs.uvarov_d_p.repository.BookRepository;
import ru.vsu.cs.uvarov_d_p.repository.impl.InMemoryBookRepository;
import ru.vsu.cs.uvarov_d_p.service.LibraryService;
import ru.vsu.cs.uvarov_d_p.service.impl.LibraryServiceImpl;
import ru.vsu.cs.uvarov_d_p.ui.ConsoleUI;
import ru.vsu.cs.uvarov_d_p.util.IsbnValidator;
import ru.vsu.cs.uvarov_d_p.util.impl.IsbnValidatorImpl;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        IsbnValidator isbnValidator = new IsbnValidatorImpl();
        BookRepository bookRepository = new InMemoryBookRepository();
        LibraryService libraryService = new LibraryServiceImpl(bookRepository, isbnValidator);

        seedDemoData(libraryService);

        ConsoleUI ui = new ConsoleUI(libraryService);
        ui.start();
    }

    private static void seedDemoData(LibraryService service) {
        service.addBook(
                "Чистый код",
                "Роберт Мартин",
                "978-5-4461-0960-9",
                List.of("Программирование", "Архитектура", "Рефакторинг")
        );

        Book borrowedBook = service.addBook(
                "Совершенный код",
                "Стив Макконнелл",
                "978-5-7502-0064-1",
                List.of("Программирование", "Техническая литература")
        );
        service.borrowBook(borrowedBook.getId(), "Алексей Смирнов");

        service.addBook(
                "Паттерны проектирования",
                "Эрих Гамма",
                "978-5-4461-0106-1",
                List.of("Архитектура ПО")
        );

        service.addBook(
                "Java. Эффективное программирование",
                "Джошуа Блох",
                "978-5-699-66108-4",
                List.of("Разработка ПО", "Java")
        );
    }
}