package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.support.PluginTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Functional (single-threaded) verification of the JDBC {@code compute} CAS path
 * over a real SQLite database: read-modify-write persists, absent keys are created,
 * and an aborting remapper writes nothing.
 */
class SqliteComputeBackendTest extends PluginTest {

    private StorageBackend backend;

    @BeforeEach
    void open() {
        backend = StorageSettings.sqlite("compute.db").useFile(false).createBackend(plugin, () -> "data", "data");
    }

    @AfterEach
    void close() {
        if (backend != null) {
            backend.close();
        }
    }

    @Test
    void computeCreatesWhenAbsent() {
        String result = backend.compute("counter", current -> current == null ? "1" : current);
        assertEquals("1", result);
        assertEquals("1", backend.load("counter"));
    }

    @Test
    void computeReadModifyWritePersists() {
        backend.compute("counter", current -> "1");
        String result = backend.compute("counter", current -> String.valueOf(Integer.parseInt(current) + 1));
        assertEquals("2", result);
        assertEquals("2", backend.load("counter"));
    }

    @Test
    void abortingRemapperWritesNothing() {
        backend.compute("counter", current -> "5");
        String result = backend.compute("counter", current -> null); // abort
        assertEquals("5", result, "abort returns the unchanged current value");
        assertEquals("5", backend.load("counter"));
    }

    @Test
    void computeOnMissingKeyWithAbortLeavesItAbsent() {
        String result = backend.compute("ghost", current -> null);
        assertNull(result);
        assertNull(backend.load("ghost"));
    }
}
