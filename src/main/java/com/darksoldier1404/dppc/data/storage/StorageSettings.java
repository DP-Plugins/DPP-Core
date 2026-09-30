package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.data.sync.RedisSyncTransport;
import com.darksoldier1404.dppc.data.sync.SyncTransport;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Describes how a {@code DataContainer} / {@code SingleDataContainer} should
 * persist its data: whether to write YAML files, which (optional) database
 * backend to use, and the connection details for that database.
 * <p>
 * Build one with the static factory methods and tweak it fluently:
 * <pre>{@code
 * // File only (the historical default)
 * StorageSettings.fileOnly();
 *
 * // Database only
 * StorageSettings.mysql("localhost", 3306, "mc", "root", "pw").useFile(false);
 *
 * // File AND database (file acts as local backup / offline fallback)
 * StorageSettings.redis("localhost", 6379, 0, null);
 *
 * // Read the standard "Database" section from a plugin config.yml
 * StorageSettings.fromConfig(plugin.getConfig());
 * }</pre>
 */
@DPPCoreVersion(since = "5.4.4")
public class StorageSettings {

    private boolean useFile = true;
    private StorageType type = StorageType.NONE;

    // Shared JDBC / Redis connection details
    private String host;
    private int port;
    private String database; // schema (sql) or numeric index (redis, parsed)
    private String username;
    private String password;

    // SQLite specific: the database file name (relative to the plugin data folder)
    private String sqliteFile = "database.db";

    // Real-time sync (Redis Pub/Sub). Independent of the storage backend so that
    // e.g. MySQL storage can still propagate via Redis.
    private boolean syncEnabled = false;
    private String syncHost;
    private int syncPort;
    private int syncDatabase;
    private String syncPassword;

    private StorageSettings() {
    }

    /* ------------------------------------------------------------------ */
    /* Factory methods                                                     */
    /* ------------------------------------------------------------------ */

    /**
     * File-only storage (no database). This reproduces the historical behaviour
     * and is what the legacy constructors delegate to.
     */
    @NotNull
    public static StorageSettings fileOnly() {
        StorageSettings s = new StorageSettings();
        s.useFile = true;
        s.type = StorageType.NONE;
        return s;
    }

    @NotNull
    public static StorageSettings mysql(String host, int port, String database, String username, String password) {
        StorageSettings s = new StorageSettings();
        s.type = StorageType.MYSQL;
        s.host = host;
        s.port = port;
        s.database = database;
        s.username = username;
        s.password = password;
        return s;
    }

    /**
     * @param file the SQLite database file name, relative to the plugin's data folder
     */
    @NotNull
    public static StorageSettings sqlite(String file) {
        StorageSettings s = new StorageSettings();
        s.type = StorageType.SQLITE;
        s.sqliteFile = (file == null || file.isEmpty()) ? "database.db" : file;
        return s;
    }

    /**
     * @param db       the Redis logical database index
     * @param password the Redis password, or {@code null} if none
     */
    @NotNull
    public static StorageSettings redis(String host, int port, int db, @Nullable String password) {
        StorageSettings s = new StorageSettings();
        s.type = StorageType.REDIS;
        s.host = host;
        s.port = port;
        s.database = String.valueOf(db);
        s.password = password;
        return s;
    }

    /**
     * Parses a standard {@code Database} section from a plugin config. When the
     * section is missing, disabled, or malformed, this falls back to
     * {@link #fileOnly()} so a misconfiguration can never break a plugin.
     * <pre>{@code
     * Database:
     *   enabled: true
     *   type: MYSQL        # MYSQL | SQLITE | REDIS
     *   use-file: true
     *   host: localhost
     *   port: 3306
     *   database: minecraft
     *   username: root
     *   password: ""
     *   file: database.db  # sqlite only
     * }</pre>
     */
    @NotNull
    public static StorageSettings fromConfig(@Nullable YamlConfiguration config) {
        if (config == null) {
            return fileOnly();
        }
        ConfigurationSection sec = config.getConfigurationSection("Database");
        if (sec == null || !sec.getBoolean("enabled", false)) {
            return fileOnly();
        }
        StorageType type;
        try {
            type = StorageType.valueOf(sec.getString("type", "NONE").trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fileOnly();
        }
        boolean useFile = sec.getBoolean("use-file", true);
        StorageSettings s;
        switch (type) {
            case MYSQL:
                s = mysql(sec.getString("host", "localhost"), sec.getInt("port", 3306),
                        sec.getString("database", "minecraft"), sec.getString("username", "root"),
                        sec.getString("password", ""));
                break;
            case SQLITE:
                s = sqlite(sec.getString("file", "database.db"));
                break;
            case REDIS:
                String pw = sec.getString("password", "");
                s = redis(sec.getString("host", "localhost"), sec.getInt("port", 6379),
                        sec.getInt("database", 0), (pw == null || pw.isEmpty()) ? null : pw);
                break;
            default:
                return fileOnly();
        }
        s.useFile(useFile);
        ConfigurationSection sync = sec.getConfigurationSection("sync");
        if (sync != null && sync.getBoolean("enabled", false)) {
            if (type == StorageType.REDIS && !sync.contains("redis-host")) {
                s.enableSync();
            } else {
                String syncPw = sync.getString("redis-password", "");
                s.withSync(sync.getString("redis-host", "localhost"), sync.getInt("redis-port", 6379),
                        sync.getInt("redis-db", 0), (syncPw == null || syncPw.isEmpty()) ? null : syncPw);
            }
        }
        return s;
    }

    /* ------------------------------------------------------------------ */
    /* Fluent options                                                      */
    /* ------------------------------------------------------------------ */

    /**
     * Controls whether YAML files are written in addition to the database.
     * When both are enabled, files act as a local backup and offline fallback.
     */
    @NotNull
    public StorageSettings useFile(boolean useFile) {
        this.useFile = useFile;
        return this;
    }

    public boolean isUseFile() {
        return useFile;
    }

    /**
     * Enables real-time cross-server propagation over a dedicated Redis instance.
     * Use this when the storage backend is not Redis (e.g. MySQL) but you still
     * want change notifications.
     */
    @NotNull
    public StorageSettings withSync(String redisHost, int redisPort, int redisDb, @Nullable String redisPassword) {
        this.syncEnabled = true;
        this.syncHost = redisHost;
        this.syncPort = redisPort;
        this.syncDatabase = redisDb;
        this.syncPassword = redisPassword;
        return this;
    }

    /**
     * Enables real-time propagation reusing this settings' Redis connection.
     * Only meaningful when {@link #getType()} is {@link StorageType#REDIS}; for any
     * other backend use {@link #withSync} to supply Redis coordinates.
     */
    @NotNull
    public StorageSettings enableSync() {
        if (type == StorageType.REDIS) {
            int db = 0;
            try {
                db = Integer.parseInt(database);
            } catch (NumberFormatException ignored) {
            }
            return withSync(host, port, db, password);
        }
        // No Redis coordinates available: leave sync off rather than misconfigure.
        return this;
    }

    public boolean isSyncEnabled() {
        return syncEnabled && syncHost != null;
    }

    /** Stable signature identifying the sync transport (shared subscriber key). */
    @NotNull
    public String syncSignature() {
        return "sync-redis:" + syncHost + ":" + syncPort + ":" + syncDatabase;
    }

    /** Builds the Redis-backed sync transport described by these settings. */
    @NotNull
    public SyncTransport createSyncTransport(@NotNull DPlugin plugin) {
        return new RedisSyncTransport(plugin, syncHost, syncPort, syncDatabase, syncPassword);
    }

    public StorageType getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSqliteFile() {
        return sqliteFile;
    }

    /* ------------------------------------------------------------------ */
    /* Backend construction                                                */
    /* ------------------------------------------------------------------ */

    /**
     * Builds the {@link StorageBackend} described by these settings for the given
     * container path. The {@code pathSupplier} lets the file backend always honour
     * the container's (mutable) path, preserving the legacy {@code setPath} behaviour.
     *
     * @param plugin       the owning plugin
     * @param pathSupplier supplies the container's current path (for files)
     * @param path         the path captured at construction (for DB table / Redis key)
     */
    @NotNull
    public StorageBackend createBackend(@NotNull DPlugin plugin, @NotNull Supplier<String> pathSupplier, @NotNull String path) {
        StorageBackend file = useFile ? new YamlStorageBackend(plugin, pathSupplier) : null;
        StorageBackend db = null;
        try {
            switch (type) {
                case MYSQL:
                    db = new MySQLStorageBackend(plugin, this, path);
                    break;
                case SQLITE:
                    db = new SQLiteStorageBackend(plugin, this, path);
                    break;
                case REDIS:
                    db = new RedisStorageBackend(plugin, this, path);
                    break;
                case NONE:
                default:
                    break;
            }
        } catch (Exception e) {
            plugin.getLog().warning("Failed to initialise " + type + " backend, falling back to file storage: " + e.getMessage(),
                    com.darksoldier1404.dppc.api.logger.DLogManager.printStorageLogs);
            db = null;
        }
        if (db == null && file == null) {
            // Degenerate config (no file, no usable DB) -> never lose data, keep files.
            return new YamlStorageBackend(plugin, pathSupplier);
        }
        if (db == null) {
            return file;
        }
        if (file == null) {
            return db;
        }
        return new CompositeStorage(db, file);
    }
}
