package com.darksoldier1404.dppc.utils;

import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUtilsTest extends PluginTest {

    @Test
    void missingKeyFixAddsAbsentKeysAndKeepsExisting() {
        YamlConfiguration def = new YamlConfiguration();
        def.set("a", 1);
        def.set("b.c", 2);
        YamlConfiguration config = new YamlConfiguration();
        config.set("a", 9);

        YamlConfiguration result = ConfigUtils.missingKeyFix(config, def);
        assertEquals(9, result.getInt("a"));   // existing kept
        assertEquals(2, result.getInt("b.c")); // missing added
    }

    @Test
    void saveAndLoadCustomDataRoundTrip() {
        YamlConfiguration data = new YamlConfiguration();
        data.set("greeting", "hello");
        data.set("count", 3);
        ConfigUtils.saveCustomData(plugin, data, "myfile", "sub");

        YamlConfiguration loaded = ConfigUtils.loadCustomData(plugin, "myfile", "sub");
        assertNotNull(loaded);
        assertEquals("hello", loaded.getString("greeting"));
        assertEquals(3, loaded.getInt("count"));
    }

    @Test
    void loadMissingCustomDataReturnsNull() {
        assertNull(ConfigUtils.loadCustomData(plugin, "nope", "sub"));
    }

    @Test
    void createCustomDataCreatesFileInDataFolderRoot() {
        // The single-arg overload targets the (already existing) data-folder root.
        YamlConfiguration created = ConfigUtils.createCustomData(plugin, "fresh");
        assertNotNull(created);
        // Loading it back from the root should now succeed.
        assertNotNull(ConfigUtils.loadCustomData(plugin, "fresh", ""));
    }

    @Test
    void createCustomDataCreatesFileInNonExistingSubdirectory() {
        // The path overload must create the parent directory before creating the file.
        // "made" does not yet exist under the data folder.
        YamlConfiguration created = ConfigUtils.createCustomData(plugin, "fresh", "made");
        assertNotNull(created);
        // Loading it back from the (now created) subdirectory should succeed.
        assertNotNull(ConfigUtils.loadCustomData(plugin, "fresh", "made"));
    }

    @Test
    void loadCustomDataMapReturnsAllFilesInPath() {
        ConfigUtils.saveCustomData(plugin, new YamlConfiguration(), "one", "bucket");
        ConfigUtils.saveCustomData(plugin, new YamlConfiguration(), "two", "bucket");
        HashMap<String, YamlConfiguration> map = ConfigUtils.loadCustomDataMap(plugin, "bucket");
        assertTrue(map.containsKey("one"));
        assertTrue(map.containsKey("two"));
    }
}
