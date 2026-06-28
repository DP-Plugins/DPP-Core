package com.darksoldier1404.dppc.builder.action.obj;

import com.darksoldier1404.dppc.support.PluginTest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistence round-trips for {@link VariableStore}. Uses {@link PluginTest} so a
 * real data folder is available for the YAML files.
 */
class VariableStoreTest extends PluginTest {

    @Test
    void globalRoundTripsThroughDisk() {
        VariableStore writer = new VariableStore(plugin);
        writer.setGlobal("kills", "7");
        writer.setGlobal("dotted.name", "ok");
        writer.saveGlobal();

        VariableStore reader = new VariableStore(plugin);
        reader.loadGlobal();
        assertEquals("7", reader.getGlobal("kills"));
        assertEquals("ok", reader.getGlobal("dotted.name"));
        assertTrue(reader.hasGlobal("kills"));
    }

    @Test
    void playerRoundTripsThroughDisk() {
        UUID uuid = UUID.randomUUID();
        VariableStore writer = new VariableStore(plugin);
        writer.setPlayer(uuid, "coins", "100");
        writer.savePlayer(uuid);

        VariableStore reader = new VariableStore(plugin);
        reader.loadPlayer(uuid);
        assertEquals("100", reader.getPlayer(uuid, "coins"));
        assertTrue(reader.hasPlayer(uuid, "coins"));
    }

    @Test
    void unknownReturnsEmptyString() {
        VariableStore store = new VariableStore(plugin);
        assertEquals("", store.getGlobal("missing"));
        assertEquals("", store.getPlayer(UUID.randomUUID(), "missing"));
        assertFalse(store.hasGlobal("missing"));
    }

    @Test
    void memoryOnlyStoreDoesNotThrowOnPersist() {
        VariableStore store = new VariableStore();
        store.setGlobal("x", "1");
        store.setPlayer(UUID.randomUUID(), "y", "2");
        // No plugin -> save/load are no-ops, must not throw.
        store.saveAll();
        store.loadGlobal();
        assertEquals("1", store.getGlobal("x"));
    }
}
