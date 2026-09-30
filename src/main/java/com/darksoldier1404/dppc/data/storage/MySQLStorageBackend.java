package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.data.DPlugin;
import com.zaxxer.hikari.HikariConfig;
import org.jetbrains.annotations.NotNull;

/**
 * MySQL / MariaDB backend. Uses the MariaDB Connector/J driver (which also speaks
 * to MySQL servers) behind a shared HikariCP pool.
 */
@DPPCoreVersion(since = "5.4.4")
public class MySQLStorageBackend extends AbstractSqlStorageBackend {

    public MySQLStorageBackend(@NotNull DPlugin plugin, @NotNull StorageSettings settings, @NotNull String path) {
        super(plugin, settings, path);
    }

    @Override
    protected String buildSignature() {
        return "mysql:" + settings.getHost() + ":" + settings.getPort() + ":"
                + settings.getDatabase() + ":" + settings.getUsername();
    }

    @Override
    protected HikariConfig buildHikariConfig() {
        HikariConfig cfg = baseConfig("MySQL-" + table);
        // Set the driver explicitly: under Bukkit's per-plugin classloaders, HikariCP's
        // DriverManager-based auto-detection cannot see the shaded driver.
        cfg.setDriverClassName("org.mariadb.jdbc.Driver");
        cfg.setJdbcUrl("jdbc:mariadb://" + settings.getHost() + ":" + settings.getPort() + "/" + settings.getDatabase());
        cfg.setUsername(settings.getUsername());
        cfg.setPassword(settings.getPassword());
        cfg.setMaximumPoolSize(10);
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        return cfg;
    }

    @Override
    protected String createTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "data_key VARCHAR(191) PRIMARY KEY, "
                + "data_value MEDIUMTEXT NOT NULL, "
                + "version BIGINT NOT NULL DEFAULT 0, "
                + "updated_at BIGINT NOT NULL)";
    }

    @Override
    protected String upsertSql() {
        return "INSERT INTO " + table + " (data_key, data_value, version, updated_at) VALUES (?, ?, 1, ?) "
                + "ON DUPLICATE KEY UPDATE data_value = VALUES(data_value), version = version + 1, updated_at = VALUES(updated_at)";
    }

    @Override
    protected String versionColumnType() {
        return "BIGINT";
    }
}
