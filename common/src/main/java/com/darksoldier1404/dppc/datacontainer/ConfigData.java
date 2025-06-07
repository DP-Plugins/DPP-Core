package com.darksoldier1404.dppc.datacontainer;

import com.darksoldier1404.dppc.datacontainer.obj.YamlData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Handles plugin configuration data with type-safe accessors.
 * This class extends YamlData to provide configuration-specific functionality.
 */
public class ConfigData extends YamlData {
    
    /**
     * Creates a new ConfigData instance with the default config file.
     * @param plugin The plugin instance
     */
    public ConfigData(@NotNull JavaPlugin plugin) {
        super(plugin, "config.yml", DataType.CONFIG);
    }

    /**
     * Creates a new ConfigData instance with a custom file name.
     * @param plugin The plugin instance
     * @param fileName The name of the config file
     */
    public ConfigData(@NotNull JavaPlugin plugin, @NotNull String fileName) {
        super(plugin, fileName.endsWith(".yml") ? fileName : fileName + ".yml", DataType.CONFIG);
    }

    /**
     * Creates a new ConfigData instance with existing YAML data.
     * @param plugin The plugin instance
     * @param data The YAML configuration data
     */
    public ConfigData(@NotNull JavaPlugin plugin, @NotNull YamlConfiguration data) {
        super(plugin, data, DataType.CONFIG);
    }
}
