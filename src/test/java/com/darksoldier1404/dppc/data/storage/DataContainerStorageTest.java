package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.data.DataContainer;
import com.darksoldier1404.dppc.data.DataType;
import com.darksoldier1404.dppc.data.TestCargo;
import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end tests that drive {@link DataContainer} through a real SQLite backend,
 * confirming the YAML serialization contract survives a database round-trip for
 * both YAML and CUSTOM data types.
 */
class DataContainerStorageTest extends PluginTest {

    private StorageSettings sqlite() {
        return StorageSettings.sqlite("container.db").useFile(false);
    }

    @Test
    void yamlValueSurvivesSaveAndLoad() {
        DataContainer<String, YamlConfiguration> c =
                new DataContainer<>(plugin, DataType.YAML, "data", sqlite());
        try {
            YamlConfiguration value = new YamlConfiguration();
            value.set("greeting", "hello");
            c.put("entry", value);
            c.save("entry");
            c.clear();
            assertFalse(c.containsKey("entry"));

            c.load("entry", YamlConfiguration.class);
            assertNotNull(c.get("entry"));
            assertEquals("hello", c.get("entry").getString("greeting"));
        } finally {
            c.close();
        }
    }

    @Test
    void loadAllRestoresEveryEntry() {
        DataContainer<String, YamlConfiguration> c =
                new DataContainer<>(plugin, DataType.YAML, "bulk", sqlite());
        try {
            YamlConfiguration a = new YamlConfiguration();
            a.set("n", 1);
            YamlConfiguration b = new YamlConfiguration();
            b.set("n", 2);
            c.put("a", a);
            c.put("b", b);
            c.saveAll();
            c.clear();

            c.loadAll(null);
            assertEquals(2, c.size());
            assertEquals(1, c.get("a").getInt("n"));
            assertEquals(2, c.get("b").getInt("n"));
        } finally {
            c.close();
        }
    }

    @Test
    void customCargoSurvivesSaveAndLoad() {
        DataContainer<String, TestCargo> c =
                new DataContainer<>(plugin, DataType.CUSTOM, "custom", sqlite());
        try {
            TestCargo cargo = c.create("hero", TestCargo.class);
            assertNotNull(cargo);
            cargo.value = "stored";
            c.save("hero");
            c.clear();

            c.loadAll(TestCargo.class);
            assertTrue(c.containsKey("hero"));
            assertEquals("stored", c.get("hero").value);
        } finally {
            c.close();
        }
    }

    @Test
    void deleteRemovesFromDatabaseAndMemory() {
        DataContainer<String, YamlConfiguration> c =
                new DataContainer<>(plugin, DataType.YAML, "del", sqlite());
        try {
            YamlConfiguration value = new YamlConfiguration();
            value.set("x", "y");
            c.put("gone", value);
            c.save("gone");

            assertTrue(c.delete("gone"));
            assertFalse(c.containsKey("gone"));

            DataContainer<String, YamlConfiguration> reload =
                    new DataContainer<>(plugin, DataType.YAML, "del", sqlite());
            try {
                reload.loadAll(null);
                assertFalse(reload.containsKey("gone"));
            } finally {
                reload.close();
            }
        } finally {
            c.close();
        }
    }
}
