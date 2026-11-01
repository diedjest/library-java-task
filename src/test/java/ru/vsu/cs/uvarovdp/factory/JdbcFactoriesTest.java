package ru.vsu.cs.uvarovdp.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.vsu.cs.uvarovdp.ex.RepositoryException;
import ru.vsu.cs.uvarovdp.repository.BookRepository;
import ru.vsu.cs.uvarovdp.repository.impl.InMemoryBookRepository;
import ru.vsu.cs.uvarovdp.repository.impl.JdbcBookRepository;
import ru.vsu.cs.uvarovdp.service.LibraryService;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Тестирование фабрик репозитория и сервиса")
class JdbcFactoriesTest {

    @Test
    @DisplayName("BookRepositoryFactory.createJdbc возвращает JdbcBookRepository")
    void shouldCreateJdbcRepository() {
        BookRepository repository = BookRepositoryFactory.createJdbc(mock(DataSource.class));

        assertInstanceOf(JdbcBookRepository.class, repository);
    }

    @Test
    @DisplayName("BookRepositoryFactory.createInMemory по-прежнему возвращает InMemoryBookRepository")
    void shouldCreateInMemoryRepository() {
        assertInstanceOf(InMemoryBookRepository.class, BookRepositoryFactory.createInMemory());
    }

    @Test
    @DisplayName("createJdbcService не открывает соединение при создании сервиса")
    void shouldNotConnectWhileCreatingService() {
        DataSource dataSource = mock(DataSource.class);

        LibraryService service = LibraryServiceFactory.createJdbcService(dataSource);

        assertNotNull(service);
        verifyNoInteractions(dataSource);
    }

    @Test
    @DisplayName("Сервис из createJdbcService работает через базу данных")
    void shouldWireServiceToJdbcRepository() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("db is down"));
        LibraryService service = LibraryServiceFactory.createJdbcService(dataSource);

        RepositoryException exception = assertThrows(RepositoryException.class, service::getAllBooks);

        assertTrue(exception.getMessage().contains("db is down"));
    }

    @Test
    @DisplayName("Сервис из createInMemoryService не требует базы данных")
    void shouldCreateInMemoryServiceWithoutDatabase() {
        LibraryService service = LibraryServiceFactory.createInMemoryService();

        assertTrue(service.getAllBooks().isEmpty());
    }
}
