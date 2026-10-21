package ru.vsu.cs.uvarovdp.factory;

import ru.vsu.cs.uvarovdp.repository.BookRepository;
import ru.vsu.cs.uvarovdp.repository.impl.InMemoryBookRepository;

public final class BookRepositoryFactory {

    private BookRepositoryFactory() {
    }

    public static BookRepository createInMemory() {
        return new InMemoryBookRepository();
    }
}
