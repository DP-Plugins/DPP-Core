package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataContainerTest extends PluginTest {

    @Test
    void defaultPathDependsOnDataType() {
        assertEquals("udata", new DataContainer<>(plugin, DataType.USER).getPath());
        assertEquals("data", new DataContainer<>(plugin, DataType.YAML).getPath());
    }

    @Test
    void setPathNullRestoresDefault() {
        DataContainer<UUID, YamlConfiguration> c = new DataContainer<>(plugin, DataType.USER, "x");
        assertEquals("x", c.getPath());
        c.setPath(null);
        assertEquals("udata", c.getPath());
    }

    @Test
    void exposesPluginAndType() {
        DataContainer<UUID, YamlConfiguration> c = new DataContainer<>(plugin, DataType.YAML);
        assertSame(plugin, c.getPlugin());
        assertEquals(DataType.YAML, c.getDataType());
    }

    @Test
    void behavesAsMap() {
        DataContainer<String, YamlConfiguration> c = new DataContainer<>(plugin, DataType.YAML);
        YamlConfiguration value = new YamlConfiguration();
        c.put("k", value);
        assertTrue(c.containsKey("k"));
        assertSame(value, c.get("k"));
    }

    @Test
    void createCustomInstantiatesAndStores() {
        DataContainer<String, TestCargo> c = new DataContainer<>(plugin, DataType.CUSTOM);
        TestCargo created = c.create("a", TestCargo.class);
        assertNotNull(created);
        assertTrue(c.containsKey("a"));
        assertSame(created, c.get("a"));
    }

    @Test
    void createExistingKeyReturnsExisting() {
        DataContainer<String, TestCargo> c = new DataContainer<>(plugin, DataType.CUSTOM);
        TestCargo first = c.create("a", TestCargo.class);
        TestCargo second = c.create("a", TestCargo.class);
        assertSame(first, second);
    }

    @Test
    void deleteInvalidKeyReturnsFalse() {
        DataContainer<Object, YamlConfiguration> c = new DataContainer<>(plugin, DataType.USER);
        assertFalse(c.delete(123));
    }

    @Test
    void deleteMissingFileReturnsFalse() {
        DataContainer<UUID, YamlConfiguration> c = new DataContainer<>(plugin, DataType.USER);
        assertFalse(c.delete(UUID.randomUUID()));
    }
}
