package com.darksoldier1404.dppc.data.storage;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the database-first / file-fallback routing of {@link CompositeStorage}
 * using in-memory fakes, with no real database or file system involved.
 */
class CompositeStorageTest {

    private InMemoryStorageBackend db;
    private InMemoryStorageBackend file;
    private CompositeStorage composite;

    private void setup() {
        db = new InMemoryStorageBackend();
        file = new InMemoryStorageBackend();
        composite = new CompositeStorage(db, file);
    }

    @Test
    void writeGoesToBothBackends() {
        setup();
        composite.save("a", "value");
        assertEquals("value", db.map.get("a"));
        assertEquals("value", file.map.get("a"));
    }

    @Test
    void readPrefersDatabase() {
        setup();
        db.map.put("k", "db-value");
        file.map.put("k", "file-value");
        assertEquals("db-value", composite.load("k"));
    }

    @Test
    void readFallsBackToFileWhenDatabaseMisses() {
        setup();
        file.map.put("k", "file-value");
        assertEquals("file-value", composite.load("k"));
    }

    @Test
    void readReturnsNullWhenNeitherHasKey() {
        setup();
        assertNull(composite.load("missing"));
    }

    @Test
    void loadAllPrefersDatabaseWhenNonEmpty() {
        setup();
        db.map.put("a", "db-a");
        file.map.put("b", "file-b");
        Map<String, String> all = composite.loadAll();
        assertEquals(1, all.size());
        assertEquals("db-a", all.get("a"));
    }

    @Test
    void loadAllFallsBackToFileWhenDatabaseEmpty() {
        setup();
        file.map.put("a", "file-a");
        file.map.put("b", "file-b");
        Map<String, String> all = composite.loadAll();
        assertEquals(2, all.size());
        assertEquals("file-a", all.get("a"));
    }

    @Test
    void deleteRemovesFromBothBackends() {
        setup();
        db.map.put("k", "v");
        file.map.put("k", "v");
        assertTrue(composite.delete("k"));
        assertFalse(db.map.containsKey("k"));
        assertFalse(file.map.containsKey("k"));
    }

    @Test
    void deleteReturnsTrueWhenOnlyOneBackendHadKey() {
        setup();
        file.map.put("k", "v");
        assertTrue(composite.delete("k"));
    }

    @Test
    void closeClosesBothBackends() {
        setup();
        composite.close();
        assertTrue(db.closed);
        assertTrue(file.closed);
    }
}
