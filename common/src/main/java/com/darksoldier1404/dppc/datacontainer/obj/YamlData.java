package com.darksoldier1404.dppc.datacontainer.obj;

import com.darksoldier1404.dppc.datacontainer.DataType;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for all YAML-based data containers.
 * Provides common functionality for loading, saving, and managing YAML configuration files.
 */
public abstract class YamlData implements DData {
    protected final JavaPlugin plugin;
    protected final DataType dataType;
    protected final String fileName;
    protected final Object lock = new Object();
    protected volatile YamlConfiguration data;

    /**
     * Creates a new YamlData instance with the specified file name.
     * @param plugin The plugin instance
     * @param fileName The name of the YAML file (with or without .yml extension)
     * @param dataType The type of data being stored
     */
    protected YamlData(@NotNull JavaPlugin plugin, @NotNull String fileName, @NotNull DataType dataType) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.fileName = fileName.endsWith(".yml") ? fileName : fileName + ".yml";
        this.dataType = Objects.requireNonNull(dataType, "Data type cannot be null");
        this.data = new YamlConfiguration();
        loadData();
    }

    /**
     * Creates a new YamlData instance with an existing configuration.
     * @param plugin The plugin instance
     * @param data The existing YAML configuration
     * @param dataType The type of data being stored
     */
    protected YamlData(@NotNull JavaPlugin plugin, @NotNull YamlConfiguration data, @NotNull DataType dataType) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.dataType = Objects.requireNonNull(dataType, "Data type cannot be null");
        this.fileName = dataType.name().toLowerCase() + ".yml";
        this.data = Objects.requireNonNull(data, "YAML data cannot be null");
    }

    /**
     * Gets the file where this data is stored.
     * @return The data file
     */
    @NotNull
    public File getDataFile() {
        File dataFolder = new File(plugin.getDataFolder(), dataType.name().toLowerCase());
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create directory: " + dataFolder.getAbsolutePath());
        }
        return new File(dataFolder, fileName);
    }

    /**
     * Loads the data from the file system.
     */
    public void loadData() {
        File configFile = getDataFile();
        if (!configFile.exists()) {
            // Try to save default from resources if it exists
            if (plugin.getResource(fileName) != null) {
                plugin.saveResource(fileName, false);
            } else {
                // Create empty file if no resource exists
                String path = dataType.name().toLowerCase();
                if (fileName.contains("/")) {
                    path = fileName.substring(0, fileName.lastIndexOf('/'));
                }
                YamlConfiguration newConfig = ConfigUtils.createCustomData(plugin, 
                    fileName.replace(".yml", "").replace(path + "/", ""), 
                    path);
                if (newConfig != null) {
                    this.data = newConfig;
                    return;
                }
            }
        }

        // Load the configuration using ConfigUtils
        String path = dataType.name().toLowerCase();
        if (fileName.contains("/")) {
            path = fileName.substring(0, fileName.lastIndexOf('/'));
            String name = fileName.substring(fileName.lastIndexOf('/') + 1).replace(".yml", "");
            this.data = ConfigUtils.loadCustomData(plugin, name, path);
        } else {
            this.data = ConfigUtils.loadCustomData(plugin, fileName.replace(".yml", ""));
        }

        if (this.data == null) {
            this.data = new YamlConfiguration();
            plugin.getLogger().warning("Failed to load " + dataType + " data from " + fileName);
        } else {
            plugin.getLogger().info("Loaded " + dataType + " data from " + fileName);
        }
    }

    /**
     * Gets the YAML configuration.
     * @return The YAML configuration
     */
    @NotNull
    public YamlConfiguration getData() {
        return data;
    }

    /**
     * Gets the file name of this data container.
     * @return The file name
     */
    @NotNull
    public String getFileName() {
        return fileName;
    }

    @Override
    @NotNull
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Saves the data to the file system.
     */
    @Override
    public void exportData() {
        synchronized (lock) {
            String path = dataType.name().toLowerCase();
            if (fileName.contains("/")) {
                path = fileName.substring(0, fileName.lastIndexOf('/'));
                String name = fileName.substring(fileName.lastIndexOf('/') + 1).replace(".yml", "");
                ConfigUtils.saveCustomData(plugin, data, name, path);
            } else {
                ConfigUtils.saveCustomData(plugin, data, fileName.replace(".yml", ""));
            }
        }
    }

    /**
     * Imports data from another YAML configuration.
     * @param newData The new YAML configuration to import
     */
    @Override
    public void importData(@Nullable YamlConfiguration newData) {
        if (newData != null) {
            synchronized (lock) {
                this.data = new YamlConfiguration();
                for (String key : newData.getKeys(true)) {
                    this.data.set(key, newData.get(key));
                }
            }
            exportData();
        }
    }

    /**
     * Gets a value from the configuration with a default value if not found.
     * @param path The path to the value
     * @param type The expected type of the value
     * @param def The default value to return if not found
     * @param <T> The type of the value
     * @return The value, or the default if not found
     */
    @Nullable
    protected <T> T get(@NotNull String path, @NotNull Class<T> type, @Nullable T def) {
        Object value = data.get(path, def);
        try {
            return type.isInstance(value) ? type.cast(value) : def;
        } catch (ClassCastException e) {
            plugin.getLogger().warning("Invalid type for " + path + ", expected " + type.getSimpleName());
            return def;
        }
    }

    /**
     * Gets a list of values from the configuration.
     * @param path The path to the list
     * @param type The type of elements in the list
     * @param <T> The type of elements in the list
     * @return The list of values, or an empty list if not found or invalid
     */
    @NotNull
    protected <T> List<T> getList(@NotNull String path, @NotNull Class<T> type) {
        List<?> list = data.getList(path, new ArrayList<>());
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .filter(type::isInstance)
                .map(type::cast)
                .collect(Collectors.toList());
    }

    /**
     * Gets the keys under the specified path.
     * @param path The configuration path
     * @return The set of keys, or an empty set if none found
     */
    @NotNull
    protected Set<String> getKeys(@NotNull String path) {
        if (path.isEmpty()) {
            return data.getKeys(false);
        }
        return data.getConfigurationSection(path) != null ?
                data.getConfigurationSection(path).getKeys(false) : new HashSet<>();
    }

    /**
     * Sets a value in the configuration and saves it.
     * @param path The path to set
     * @param value The value to set
     */
    protected void set(@NotNull String path, @Nullable Object value) {
        set(path, value, true);
    }

    /**
     * Sets a value in the configuration with an option to save immediately.
     * @param path The path to set
     * @param value The value to set
     * @param save Whether to save the config immediately
     */
    protected void set(@NotNull String path, @Nullable Object value, boolean save) {
        synchronized (lock) {
            data.set(path, value);
        }
        if (save) {
            exportData();
        }
    }

    /**
     * Gets a configuration value with type safety.
     * @param path The configuration path
     * @param type The expected type
     * @param <T> The type of the value
     * @return The value, or null if not found or type mismatch
     */
    @Nullable
    public <T> T get(@NotNull String path, @NotNull Class<T> type) {
        return get(path, type, null);
    }

    /**
     * Gets a string value from the configuration.
     * @param path The configuration path
     * @param def The default value
     * @return The string value, or the default if not found
     */
    @Nullable
    public String getString(@NotNull String path, @Nullable String def) {
        return get(path, String.class, def);
    }

    /**
     * Gets an integer value from the configuration.
     * @param path The configuration path
     * @param def The default value
     * @return The integer value, or the default if not found or invalid
     */
    public int getInt(@NotNull String path, int def) {
        return get(path, Integer.class, def);
    }

    /**
     * Gets a boolean value from the configuration.
     * @param path The configuration path
     * @param def The default value
     * @return The boolean value, or the default if not found or invalid
     */
    public boolean getBoolean(@NotNull String path, boolean def) {
        return get(path, Boolean.class, def);
    }

    /**
     * Gets a double value from the configuration.
     * @param path The configuration path
     * @param def The default value
     * @return The double value, or the default if not found or invalid
     */
    public double getDouble(@NotNull String path, double def) {
        return get(path, Double.class, def);
    }

    /**
     * Gets a string list from the configuration.
     * @param path The configuration path
     * @return The string list, or an empty list if not found or invalid
     */
    @NotNull
    public List<String> getStringList(@NotNull String path) {
        return getList(path, String.class);
    }
}
