package ru.vsu.cs.uvarovdp;

import ru.vsu.cs.uvarovdp.config.DatabaseConfig;
import ru.vsu.cs.uvarovdp.db.DataSourceFactory;
import ru.vsu.cs.uvarovdp.db.DatabaseMigrator;
import ru.vsu.cs.uvarovdp.ex.RepositoryException;
import ru.vsu.cs.uvarovdp.factory.ConsoleUIFactory;
import ru.vsu.cs.uvarovdp.factory.LibraryServiceFactory;
import ru.vsu.cs.uvarovdp.service.LibraryService;
import ru.vsu.cs.uvarovdp.ui.ConsoleUI;

import javax.sql.DataSource;

public class Main {

    public static void main(String[] args) {
        DataSource dataSource = DataSourceFactory.create(DatabaseConfig.fromEnvironment());
        try {
            new DatabaseMigrator(dataSource).migrate();
        } catch (RepositoryException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }

        LibraryService libraryService = LibraryServiceFactory.createJdbcService(dataSource);
        ConsoleUI ui = ConsoleUIFactory.create(libraryService);
        ui.start();
    }
}
