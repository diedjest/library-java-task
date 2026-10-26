package ru.vsu.cs.uvarovdp;

import ru.vsu.cs.uvarovdp.dto.BookData;
import ru.vsu.cs.uvarovdp.entity.Book;
import ru.vsu.cs.uvarovdp.factory.ConsoleUIFactory;
import ru.vsu.cs.uvarovdp.factory.LibraryServiceFactory;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.ConsoleUI;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        LibraryService libraryService = LibraryServiceFactory.createInMemoryService();

        seedDemoData(libraryService);

        ConsoleUI ui = ConsoleUIFactory.create(libraryService);
        ui.start();
    }

    private static void seedDemoData(LibraryService service) {
        service.addBook(new BookData(
                "Преступление и наказание",
                "Фёдор Достоевский",
                "978-5-04-116640-3",
                List.of("Русская классика", "Роман", "Психологический роман")
        ));

        Book borrowedBook = service.addBook(new BookData(
                "Война и мир",
                "Лев Толстой",
                "978-5-04-116641-0",
                List.of("Русская классика", "Роман-эпопея")
        ));
        service.borrowBook(borrowedBook.getId(), "Алексей Смирнов");

        service.addBook(new BookData(
                "Мастер и Маргарита",
                "Михаил Булгаков",
                "978-5-04-116642-7",
                List.of("Русская классика", "Мистика", "Сатира")
        ));

        service.addBook(new BookData(
                "Мёртвые души",
                "Николай Гоголь",
                "978-5-04-116643-4",
                List.of("Русская классика", "Поэма", "Сатира")
        ));
    }
}