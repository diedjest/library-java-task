package ru.vsu.cs.uvarovdp.factory;

import ru.vsu.cs.uvarovdp.repository.BookRepository;
import ru.vsu.cs.uvarovdp.repository.impl.InMemoryBookRepository;
import ru.vsu.cs.uvarovdp.repository.impl.JdbcBookRepository;

import javax.sql.DataSource;

public final class BookRepositoryFactory {

    private BookRepositoryFactory() {
    }

    public static BookRepository createInMemory() {
        return new InMemoryBookRepository();
    }

    public static BookRepository createJdbc(DataSource dataSource) {
        return new JdbcBookRepository(dataSource);
    }
}
