package ru.vsu.cs.uvarovdp.factory;

import ru.vsu.cs.uvarovdp.repository.BookRepository;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.service.impl.LibraryServiceImpl;
import ru.vsu.cs.uvarovdp.util.IsbnValidator;

public final class LibraryServiceFactory {

    private LibraryServiceFactory() {
    }

    public static LibraryService createInMemoryService() {
        IsbnValidator isbnValidator = IsbnValidatorFactory.create();
        BookRepository bookRepository = BookRepositoryFactory.createInMemory();
        return new LibraryServiceImpl(bookRepository, isbnValidator);
    }
}
