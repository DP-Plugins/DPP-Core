package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.data.DPlugin;
import com.zaxxer.hikari.HikariConfig;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * SQLite backend. Stores the database in a single file under the plugin's data
 * folder. The Hikari pool is capped at one connection because SQLite serialises
 * writers; a {@code busy_timeout} avoids spurious {@code SQLITE_BUSY} errors.
 */
@DPPCoreVersion(since = "5.4.4")
public class SQLiteStorageBackend extends AbstractSqlStorageBackend {

    public SQLiteStorageBackend(@NotNull DPlugin plugin, @NotNull StorageSettings settings, @NotNull String path) {
        super(plugin, settings, path);
    }

    private File databaseFile() {
        return new File(plugin.getDataFolder(), settings.getSqliteFile());
    }

    @Override
    protected String buildSignature() {
        return "sqlite:" + databaseFile().getAbsolutePath();
    }

    @Override
    protected HikariConfig buildHikariConfig() {
        File file = databaseFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        HikariConfig cfg = baseConfig("SQLite-" + table);
        // Set the driver explicitly: under Bukkit's per-plugin classloaders, HikariCP's
        // DriverManager-based auto-detection cannot see the shaded driver.
        cfg.setDriverClassName("org.sqlite.JDBC");
        cfg.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        cfg.setMaximumPoolSize(1);
        cfg.addDataSourceProperty("busy_timeout", "5000");
        return cfg;
    }

    @Override
    protected String createTableSql() {
        return "CREATE TABLE IF NOT EXISTS " + table + " ("
                + "data_key TEXT PRIMARY KEY, "
                + "data_value TEXT NOT NULL, "
                + "version INTEGER NOT NULL DEFAULT 0, "
                + "updated_at INTEGER NOT NULL)";
    }

    @Override
    protected String upsertSql() {
        return "INSERT INTO " + table + " (data_key, data_value, version, updated_at) VALUES (?, ?, 1, ?) "
                + "ON CONFLICT(data_key) DO UPDATE SET data_value = excluded.data_value, version = " + table + ".version + 1, updated_at = excluded.updated_at";
    }

    @Override
    protected String versionColumnType() {
        return "INTEGER";
    }
}
