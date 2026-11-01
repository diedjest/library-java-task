package ru.vsu.cs.uvarovdp.db;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import ru.vsu.cs.uvarovdp.config.DatabaseConfig;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тестирование DataSourceFactory")
class DataSourceFactoryTest {

    @Test
    @DisplayName("create настраивает PostgreSQL DataSource по параметрам из DatabaseConfig")
    void shouldConfigureDataSourceFromConfig() {
        DatabaseConfig config = new DatabaseConfig("jdbc:postgresql://db:5432/library", "reader", "secret");

        DataSource dataSource = DataSourceFactory.create(config);

        PGSimpleDataSource postgres = assertInstanceOf(PGSimpleDataSource.class, dataSource);
        assertTrue(postgres.getUrl().startsWith("jdbc:postgresql://db:5432/library"));
        assertEquals("reader", postgres.getUser());
        assertEquals("secret", postgres.getPassword());
    }
}
