package ru.vsu.cs.uvarovdp.db;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import ru.vsu.cs.uvarovdp.ex.RepositoryException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Objects;

public class DatabaseMigrator {

    static final String CHANGELOG = "db/changelog/db.changelog-master.yaml";

    private final DataSource dataSource;

    public DatabaseMigrator(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "DataSource не может быть null");
    }

    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.update();
            }
        } catch (Exception e) {
            throw new RepositoryException("Не удалось применить миграции базы данных: " + e.getMessage(), e);
        }
    }
}
