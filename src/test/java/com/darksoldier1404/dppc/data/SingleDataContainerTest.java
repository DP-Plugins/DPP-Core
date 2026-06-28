package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleDataContainerTest extends PluginTest {

    @Test
    void defaultPathDependsOnDataType() {
        assertEquals("udata", new SingleDataContainer<>(plugin, DataType.USER).getPath());
        assertEquals("data", new SingleDataContainer<>(plugin, DataType.YAML).getPath());
        assertEquals("data", new SingleDataContainer<>(plugin, DataType.CUSTOM).getPath());
    }

    @Test
    void customPathIsHonoured() {
        SingleDataContainer<UUID, YamlConfiguration> c = new SingleDataContainer<>(plugin, DataType.YAML, "custompath");
        assertEquals("custompath", c.getPath());
    }

    @Test
    void setPathNullRestoresDefault() {
        SingleDataContainer<UUID, YamlConfiguration> c = new SingleDataContainer<>(plugin, DataType.USER, "x");
        c.setPath(null);
        assertEquals("udata", c.getPath());
    }

    @Test
    void validUserDataIsStored() {
        SingleDataContainer<UUID, YamlConfiguration> c = new SingleDataContainer<>(plugin, DataType.USER);
        UUID key = UUID.randomUUID();
        YamlConfiguration value = new YamlConfiguration();
        c.set(key, value);
        assertTrue(c.hasData());
        assertEquals(key, c.getKey());
        assertEquals(value, c.getValue());
    }

    @Test
    void invalidUserKeyIsRejected() {
        // USER requires UUID keys; a String key must be rejected and the container left empty.
        SingleDataContainer<Object, YamlConfiguration> c = new SingleDataContainer<>(plugin, DataType.USER);
        c.set("not-a-uuid", new YamlConfiguration());
        assertFalse(c.hasData());
        assertNull(c.getKey());
        assertNull(c.getValue());
    }

    @Test
    void nullValueIsRejected() {
        SingleDataContainer<UUID, YamlConfiguration> c = new SingleDataContainer<>(plugin, DataType.USER);
        c.set(UUID.randomUUID(), null);
        assertFalse(c.hasData());
    }

    @Test
    void wrongValueTypeForYamlIsRejected() {
        // YAML expects YamlConfiguration, not DataCargo.
        SingleDataContainer<String, Object> c = new SingleDataContainer<>(plugin, DataType.YAML);
        c.set("key", new TestCargo());
        assertFalse(c.hasData());
    }

    @Test
    void customRequiresDataCargoValue() {
        SingleDataContainer<String, Object> c = new SingleDataContainer<>(plugin, DataType.CUSTOM);
        c.set("key", new TestCargo());
        assertTrue(c.hasData());
        // A plain YamlConfiguration is not a DataCargo and must be rejected.
        SingleDataContainer<String, Object> c2 = new SingleDataContainer<>(plugin, DataType.CUSTOM);
        c2.set("key", new YamlConfiguration());
        assertFalse(c2.hasData());
    }

    @Test
    void clearEmptiesContainer() {
        SingleDataContainer<UUID, YamlConfiguration> c = new SingleDataContainer<>(plugin, DataType.USER);
        c.set(UUID.randomUUID(), new YamlConfiguration());
        c.clear();
        assertFalse(c.hasData());
    }
}
