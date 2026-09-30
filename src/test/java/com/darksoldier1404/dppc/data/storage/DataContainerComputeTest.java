package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.data.DataContainer;
import com.darksoldier1404.dppc.data.DataType;
import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Drives {@link DataContainer#compute} over a real SQLite backend: an exchange-style
 * stock decrement persists and reflects in memory, and the transaction aborts (writing
 * nothing) once stock is exhausted.
 */
class DataContainerComputeTest extends PluginTest {

    private StorageSettings sqlite() {
        return StorageSettings.sqlite("dc-compute.db").useFile(false);
    }

    private void decrementStock(YamlConfiguration v) {
        int stock = v.getInt("stock");
        if (stock <= 0) {
            throw new DataContainer.AbortTransactionException("out of stock");
        }
        v.set("stock", stock - 1);
    }

    @Test
    void computeDecrementsAndPersists() {
        DataContainer<String, YamlConfiguration> c = new DataContainer<>(plugin, DataType.YAML, "shop", sqlite());
        try {
            YamlConfiguration item = new YamlConfiguration();
            item.set("stock", 2);
            c.put("item", item);
            c.save("item");

            assertNotNull(c.compute("item", YamlConfiguration.class, this::decrementStock));
            assertEquals(1, c.get("item").getInt("stock"));

            // persisted: a fresh container loads the decremented value
            DataContainer<String, YamlConfiguration> reload = new DataContainer<>(plugin, DataType.YAML, "shop", sqlite());
            try {
                reload.loadAll(null);
                assertEquals(1, reload.get("item").getInt("stock"));
            } finally {
                reload.close();
            }
        } finally {
            c.close();
        }
    }

    @Test
    void computeAbortsWhenExhausted() {
        DataContainer<String, YamlConfiguration> c = new DataContainer<>(plugin, DataType.YAML, "shop2", sqlite());
        try {
            YamlConfiguration item = new YamlConfiguration();
            item.set("stock", 1);
            c.put("item", item);
            c.save("item");

            assertNotNull(c.compute("item", YamlConfiguration.class, this::decrementStock)); // 1 -> 0
            assertEquals(0, c.get("item").getInt("stock"));

            assertNull(c.compute("item", YamlConfiguration.class, this::decrementStock)); // aborts
            assertEquals(0, c.get("item").getInt("stock"), "aborted compute must not change stock");
        } finally {
            c.close();
        }
    }
}
