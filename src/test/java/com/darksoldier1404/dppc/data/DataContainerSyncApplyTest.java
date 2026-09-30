package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.data.storage.StorageSettings;
import com.darksoldier1404.dppc.data.sync.SyncOp;
import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the receiver side of real-time sync: when notified of a remote change,
 * a container reloads the affected key from the shared (SQLite) store into memory.
 * Uses two containers over one database to stand in for two servers; the sync
 * notification is invoked directly so no Redis is required.
 */
class DataContainerSyncApplyTest extends PluginTest {

    private StorageSettings sqlite() {
        return StorageSettings.sqlite("sync-apply.db").useFile(false);
    }

    @Test
    void remoteUpsertReloadsValueIntoMemory() {
        DataContainer<String, YamlConfiguration> serverA = new DataContainer<>(plugin, DataType.YAML, "live", sqlite());
        DataContainer<String, YamlConfiguration> serverB = new DataContainer<>(plugin, DataType.YAML, "live", sqlite());
        try {
            YamlConfiguration v1 = new YamlConfiguration();
            v1.set("price", 100);
            serverA.put("item", v1);
            serverA.save("item");

            serverB.loadAll(null);
            assertEquals(100, serverB.get("item").getInt("price"));

            // A changes the price; B's in-memory copy is now stale
            YamlConfiguration v2 = new YamlConfiguration();
            v2.set("price", 50);
            serverA.put("item", v2);
            serverA.save("item");
            assertEquals(100, serverB.get("item").getInt("price"), "B is stale until notified");

            // Sync notification arrives at B -> reload from the shared store
            serverB.applyRemoteChange("item", SyncOp.UPSERT);
            assertEquals(50, serverB.get("item").getInt("price"));
        } finally {
            serverA.close();
            serverB.close();
        }
    }

    @Test
    void remoteDeleteEvictsFromMemory() {
        DataContainer<String, YamlConfiguration> serverA = new DataContainer<>(plugin, DataType.YAML, "live2", sqlite());
        DataContainer<String, YamlConfiguration> serverB = new DataContainer<>(plugin, DataType.YAML, "live2", sqlite());
        try {
            YamlConfiguration v = new YamlConfiguration();
            v.set("price", 7);
            serverA.put("item", v);
            serverA.save("item");
            serverB.loadAll(null);
            assertTrue(serverB.containsKey("item"));

            serverA.delete("item");
            serverB.applyRemoteChange("item", SyncOp.DELETE);
            assertFalse(serverB.containsKey("item"));
        } finally {
            serverA.close();
            serverB.close();
        }
    }

    @Test
    void remoteUpsertWorksForCustomType() {
        DataContainer<String, TestCargo> serverA = new DataContainer<>(plugin, DataType.CUSTOM, "livec", sqlite());
        DataContainer<String, TestCargo> serverB = new DataContainer<>(plugin, DataType.CUSTOM, "livec", sqlite());
        try {
            TestCargo cargo = serverA.create("hero", TestCargo.class);
            cargo.value = "first";
            serverA.save("hero");

            serverB.loadAll(TestCargo.class); // sets the value class for remote reloads
            assertEquals("first", serverB.get("hero").value);

            TestCargo updated = serverA.get("hero");
            updated.value = "second";
            serverA.save("hero");

            serverB.applyRemoteChange("hero", SyncOp.UPSERT);
            assertEquals("second", serverB.get("hero").value);
        } finally {
            serverA.close();
            serverB.close();
        }
    }
}
