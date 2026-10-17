package ru.vsu.cs.uvarovdp.factory;

import ru.vsu.cs.uvarovdp.repository.BookRepository;
import ru.vsu.cs.uvarovdp.repository.impl.InMemoryBookRepository;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.service.impl.LibraryServiceImpl;
import ru.vsu.cs.uvarovdp.util.IsbnValidator;
import ru.vsu.cs.uvarovdp.util.impl.IsbnValidatorImpl;

public final class LibraryServiceFactory {

    private LibraryServiceFactory() {
    }

    public static LibraryService createInMemoryService() {
        IsbnValidator isbnValidator = new IsbnValidatorImpl();
        BookRepository bookRepository = new InMemoryBookRepository();
        return new LibraryServiceImpl(bookRepository, isbnValidator);
    }
}
