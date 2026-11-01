package ru.vsu.cs.uvarovdp.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.vsu.cs.uvarovdp.ex.RepositoryException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Тестирование DatabaseMigrator на моках")
class DatabaseMigratorTest {

    @Test
    @DisplayName("Конструктор отклоняет null вместо DataSource")
    void shouldRejectNullDataSource() {
        assertThrows(NullPointerException.class, () -> new DatabaseMigrator(null));
    }

    @Test
    @DisplayName("migrate оборачивает ошибку подключения в RepositoryException с исходной причиной")
    void shouldWrapConnectionFailure() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        SQLException failure = new SQLException("connection refused");
        when(dataSource.getConnection()).thenThrow(failure);

        RepositoryException exception = assertThrows(RepositoryException.class,
                () -> new DatabaseMigrator(dataSource).migrate());

        assertSame(failure, exception.getCause());
        assertTrue(exception.getMessage().startsWith("Не удалось применить миграции базы данных"));
        assertTrue(exception.getMessage().contains("connection refused"));
    }

    @Test
    @DisplayName("migrate превращает любой сбой Liquibase в RepositoryException и закрывает соединение")
    void shouldWrapLiquibaseFailureAndCloseConnection() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenThrow(new SQLException("broken metadata"));

        assertThrows(RepositoryException.class, () -> new DatabaseMigrator(dataSource).migrate());

        verify(connection).close();
    }
}
