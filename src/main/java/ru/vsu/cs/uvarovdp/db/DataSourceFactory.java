package ru.vsu.cs.uvarovdp.db;

import org.postgresql.ds.PGSimpleDataSource;
import ru.vsu.cs.uvarovdp.config.DatabaseConfig;

import javax.sql.DataSource;

public final class DataSourceFactory {

    private DataSourceFactory() {
    }

    public static DataSource create(DatabaseConfig config) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(config.url());
        dataSource.setUser(config.user());
        dataSource.setPassword(config.password());
        return dataSource;
    }
}
