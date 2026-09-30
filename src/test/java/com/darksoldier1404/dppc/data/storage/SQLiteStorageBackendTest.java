package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.support.PluginTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips a real SQLite database file (created under the mock plugin's data
 * folder) to verify the JDBC backend's save / load / loadAll / delete paths.
 */
class SQLiteStorageBackendTest extends PluginTest {

    private StorageBackend backend;

    @BeforeEach
    void openBackend() {
        StorageSettings settings = StorageSettings.sqlite("test.db").useFile(false);
        backend = settings.createBackend(plugin, () -> "data", "data");
        assertTrue(backend instanceof SQLiteStorageBackend, "DB-only settings should yield a raw SQLite backend");
    }

    @AfterEach
    void closeBackend() {
        if (backend != null) {
            backend.close();
        }
    }

    @Test
    void saveThenLoadReturnsSameValue() {
        backend.save("alpha", "key: value");
        assertEquals("key: value", backend.load("alpha"));
    }

    @Test
    void saveOverwritesExistingValue() {
        backend.save("alpha", "first");
        backend.save("alpha", "second");
        assertEquals("second", backend.load("alpha"));
    }

    @Test
    void loadMissingKeyReturnsNull() {
        assertNull(backend.load("nope"));
    }

    @Test
    void loadAllReturnsEverythingStored() {
        backend.save("a", "1");
        backend.save("b", "2");
        Map<String, String> all = backend.loadAll();
        assertEquals(2, all.size());
        assertEquals("1", all.get("a"));
        assertEquals("2", all.get("b"));
    }

    @Test
    void deleteRemovesValue() {
        backend.save("a", "1");
        assertTrue(backend.delete("a"));
        assertNull(backend.load("a"));
    }

    @Test
    void deleteMissingKeyReturnsFalse() {
        assertFalse(backend.delete("ghost"));
    }

    /**
     * Tables created by an early 5.4.4 build have no {@code version} column.
     * Opening a backend over such a table must add the column (so the optimistic
     * upsert works) without throwing — and without re-issuing a failing ALTER on
     * subsequent opens, which is what produced the "Duplicate column name" warning.
     */
    @Test
    void migratesLegacyTableMissingVersionColumn() throws Exception {
        String table = AbstractSqlStorageBackend.sanitize(plugin.getName() + "_data");
        File dbFile = new File(plugin.getDataFolder(), "legacy.db");
        File parent = dbFile.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        // Build a pre-version-column table and seed a row, the legacy on-disk shape.
        try (Connection con = DriverManager.getConnection(url);
             Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE " + table
                    + " (data_key TEXT PRIMARY KEY, data_value TEXT NOT NULL, updated_at INTEGER NOT NULL)");
            st.executeUpdate("INSERT INTO " + table
                    + " (data_key, data_value, updated_at) VALUES ('old', 'legacy', 0)");
        }

        StorageSettings settings = StorageSettings.sqlite("legacy.db").useFile(false);
        StorageBackend migrated = settings.createBackend(plugin, () -> "data", "data");
        try {
            assertEquals("legacy", migrated.load("old"), "pre-existing row should survive migration");
            migrated.save("new", "value"); // upsert references the freshly added version column
            assertEquals("value", migrated.load("new"));
        } finally {
            migrated.close();
        }

        // Reopening over the now-migrated table must be a no-op (no second ALTER).
        StorageBackend reopened = settings.createBackend(plugin, () -> "data", "data");
        try {
            assertEquals("legacy", reopened.load("old"));
            assertEquals("value", reopened.load("new"));
        } finally {
            reopened.close();
        }
    }
}
