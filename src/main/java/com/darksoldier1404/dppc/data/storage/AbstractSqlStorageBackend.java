package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.data.DPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Shared JDBC implementation for the relational backends. Stores each key as a
 * single row of {@code (data_key, data_value, updated_at)} in a table named after
 * the owning plugin and container path. Subclasses only provide the connection
 * pool, a stable signature, and the dialect-specific upsert / create-table SQL.
 */
@DPPCoreVersion(since = "5.4.4")
public abstract class AbstractSqlStorageBackend implements StorageBackend {

    /** Maximum optimistic-CAS attempts in {@link #compute} before giving up. */
    protected static final int MAX_CAS_RETRIES = 16;

    protected final DPlugin plugin;
    protected final StorageSettings settings;
    protected final String table;
    protected final String signature;
    protected final HikariDataSource dataSource;

    protected AbstractSqlStorageBackend(@NotNull DPlugin plugin, @NotNull StorageSettings settings, @NotNull String path) {
        this.plugin = plugin;
        this.settings = settings;
        this.table = sanitize(plugin.getName() + "_" + path);
        // buildSignature()/buildHikariConfig() read only `settings`, which is already
        // assigned above, so invoking these overridable methods here is safe.
        this.signature = buildSignature();
        this.dataSource = ConnectionManager.acquireJdbc(signature, this::buildHikariConfig);
        ensureTable();
    }

    /**
     * Sanitizes an identifier so it is safe to embed directly in SQL: only
     * letters, digits and underscores survive.
     */
    protected static String sanitize(String raw) {
        String cleaned = raw.replaceAll("[^a-zA-Z0-9_]", "_");
        if (!cleaned.isEmpty() && Character.isDigit(cleaned.charAt(0))) {
            cleaned = "t_" + cleaned;
        }
        return cleaned;
    }

    /** A stable key identifying the shared connection pool. */
    protected abstract String buildSignature();

    /** Builds the Hikari pool configuration for this dialect. */
    protected abstract HikariConfig buildHikariConfig();

    /** {@code CREATE TABLE IF NOT EXISTS} statement for this dialect. */
    protected abstract String createTableSql();

    /** Dialect-specific upsert statement with two positional parameters (value, key bound separately). */
    protected abstract String upsertSql();

    /** SQL type for the optimistic-locking {@code version} column ({@code BIGINT} / {@code INTEGER}). */
    protected abstract String versionColumnType();

    private void ensureTable() {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(createTableSql())) {
            ps.executeUpdate();
        } catch (SQLException e) {
            warn("create table", null, e);
        }
        // Migration for tables created before the version column existed (5.4.4 early).
        // We probe for the column first rather than issuing a blind ALTER that fails
        // harmlessly: some drivers (e.g. MariaDB) log the server-side DDL error at WARN
        // — "Duplicate column name 'version'" — on every startup before the exception
        // is even thrown, so swallowing it is not enough to keep the log clean.
        try (Connection con = dataSource.getConnection()) {
            if (!hasVersionColumn(con)) {
                try (PreparedStatement ps = con.prepareStatement(
                        "ALTER TABLE " + table + " ADD COLUMN version " + versionColumnType() + " NOT NULL DEFAULT 0")) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            warn("add version column", null, e);
        }
    }

    /**
     * Whether the backing table already has the optimistic-locking {@code version}
     * column. Reads from JDBC metadata (no DDL), comparing column names
     * case-insensitively so it works across dialects that fold identifier case.
     */
    private boolean hasVersionColumn(Connection con) throws SQLException {
        try (ResultSet rs = con.getMetaData().getColumns(con.getCatalog(), null, table, null)) {
            while (rs.next()) {
                if ("version".equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void save(@NotNull String key, @NotNull String yaml) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(upsertSql())) {
            bindUpsert(ps, key, yaml);
            ps.executeUpdate();
        } catch (SQLException e) {
            warn("save", key, e);
        }
    }

    /**
     * Binds parameters for the upsert. Default layout matches
     * {@code INSERT INTO t(data_key, data_value, updated_at) VALUES(?,?,?)} with a
     * trailing update clause that reuses the values. Subclasses whose statement
     * needs a different parameter order override this.
     */
    protected void bindUpsert(PreparedStatement ps, String key, String yaml) throws SQLException {
        ps.setString(1, key);
        ps.setString(2, yaml);
        ps.setLong(3, System.currentTimeMillis());
    }

    @Override
    @Nullable
    public String load(@NotNull String key) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT data_value FROM " + table + " WHERE data_key = ?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            warn("load", key, e);
        }
        return null;
    }

    @Override
    @NotNull
    public Map<String, String> loadAll() {
        Map<String, String> result = new HashMap<>();
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT data_key, data_value FROM " + table);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            warn("loadAll", null, e);
        }
        return result;
    }

    @Override
    public boolean delete(@NotNull String key) {
        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM " + table + " WHERE data_key = ?")) {
            ps.setString(1, key);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            warn("delete", key, e);
            return false;
        }
    }

    @Override
    @Nullable
    public String compute(@NotNull String key, @NotNull UnaryOperator<String> remapper) {
        for (int attempt = 0; attempt < MAX_CAS_RETRIES; attempt++) {
            try (Connection con = dataSource.getConnection()) {
                String current = null;
                long version = 0;
                boolean exists = false;
                try (PreparedStatement ps = con.prepareStatement("SELECT data_value, version FROM " + table + " WHERE data_key = ?")) {
                    ps.setString(1, key);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            current = rs.getString(1);
                            version = rs.getLong(2);
                            exists = true;
                        }
                    }
                }
                String next = remapper.apply(current);
                if (next == null) {
                    return current; // remapper aborted: no write
                }
                if (exists) {
                    try (PreparedStatement up = con.prepareStatement(
                            "UPDATE " + table + " SET data_value = ?, version = ?, updated_at = ? WHERE data_key = ? AND version = ?")) {
                        up.setString(1, next);
                        up.setLong(2, version + 1);
                        up.setLong(3, System.currentTimeMillis());
                        up.setString(4, key);
                        up.setLong(5, version);
                        if (up.executeUpdate() == 1) {
                            return next; // CAS won
                        }
                        // version moved under us: retry
                    }
                } else {
                    try (PreparedStatement in = con.prepareStatement(
                            "INSERT INTO " + table + " (data_key, data_value, version, updated_at) VALUES (?, ?, 1, ?)")) {
                        in.setString(1, key);
                        in.setString(2, next);
                        in.setLong(3, System.currentTimeMillis());
                        in.executeUpdate();
                        return next;
                    } catch (SQLException insertEx) {
                        if (!isDuplicateKey(insertEx)) {
                            warn("compute-insert", key, insertEx);
                            return null;
                        }
                        // someone else inserted first: retry as an update
                    }
                }
            } catch (SQLException e) {
                warn("compute", key, e);
                return null;
            }
        }
        plugin.getLog().warning("[" + table + "] compute (key=" + key + ") gave up after "
                + MAX_CAS_RETRIES + " CAS retries", DLogManager.printStorageLogs);
        return null;
    }

    /** True if the exception is an integrity-constraint (duplicate primary key) violation. */
    private static boolean isDuplicateKey(SQLException e) {
        String state = e.getSQLState();
        return state != null && state.startsWith("23");
    }

    @Override
    public void close() {
        ConnectionManager.releaseJdbc(signature);
    }

    protected HikariConfig baseConfig(String poolNameSuffix) {
        HikariConfig cfg = new HikariConfig();
        cfg.setPoolName("DPP-" + poolNameSuffix);
        return cfg;
    }

    private void warn(String op, @Nullable String key, Exception e) {
        plugin.getLog().warning("[" + table + "] SQL " + op + (key != null ? " (key=" + key + ")" : "")
                + " failed: " + e.getMessage(), DLogManager.printStorageLogs);
    }
}
