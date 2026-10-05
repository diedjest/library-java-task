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
                "Преступление и наказание",
                "Фёдор Достоевский",
                "978-5-04-116640-3",
                List.of("Русская классика", "Роман", "Психологический роман")
        );

        Book borrowedBook = service.addBook(
                "Война и мир",
                "Лев Толстой",
                "978-5-04-116641-0",
                List.of("Русская классика", "Роман-эпопея")
        );
        service.borrowBook(borrowedBook.getId(), "Алексей Смирнов");

        service.addBook(
                "Мастер и Маргарита",
                "Михаил Булгаков",
                "978-5-04-116642-7",
                List.of("Русская классика", "Мистика", "Сатира")
        );

        service.addBook(
                "Мёртвые души",
                "Николай Гоголь",
                "978-5-04-116643-4",
                List.of("Русская классика", "Поэма", "Сатира")
        );
    }
}