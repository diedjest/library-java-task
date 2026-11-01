package ru.vsu.cs.uvarovdp.factory;

import ru.vsu.cs.uvarovdp.repository.BookRepository;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.service.impl.LibraryServiceImpl;
import ru.vsu.cs.uvarovdp.util.IsbnValidator;

import javax.sql.DataSource;

public final class LibraryServiceFactory {

    private LibraryServiceFactory() {
    }

    public static LibraryService createInMemoryService() {
        return create(BookRepositoryFactory.createInMemory());
    }

    public static LibraryService createJdbcService(DataSource dataSource) {
        return create(BookRepositoryFactory.createJdbc(dataSource));
    }

    private static LibraryService create(BookRepository bookRepository) {
        IsbnValidator isbnValidator = IsbnValidatorFactory.create();
        return new LibraryServiceImpl(bookRepository, isbnValidator);
    }
}
