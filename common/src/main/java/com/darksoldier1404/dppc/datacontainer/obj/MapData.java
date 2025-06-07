package com.darksoldier1404.dppc.datacontainer.obj;

import com.darksoldier1404.dppc.datacontainer.DataType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A data container that manages a collection of YamlData objects.
 * This is useful for managing multiple related configurations or data files.
 */
public class MapData implements DData {
    private final JavaPlugin plugin;
    private final DataType dataType;
    private final String directoryName;
    private final Map<String, YamlData> dataMap;
    private final Class<? extends YamlData> dataClass;

    /**
     * Creates a new MapData instance.
     * @param plugin The plugin instance
     * @param dataType The type of data being stored
     * @param directoryName The name of the directory to store data files
     * @param dataClass The class of the YamlData implementation to use
     */
    public MapData(@NotNull JavaPlugin plugin, @NotNull DataType dataType, 
                   @NotNull String directoryName, @NotNull Class<? extends YamlData> dataClass) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.dataType = Objects.requireNonNull(dataType, "Data type cannot be null");
        this.directoryName = Objects.requireNonNull(directoryName, "Directory name cannot be null");
        this.dataClass = Objects.requireNonNull(dataClass, "Data class cannot be null");
        this.dataMap = new ConcurrentHashMap<>();
        
        // Ensure the directory exists
        File dataFolder = new File(plugin.getDataFolder(), directoryName);
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create directory: " + dataFolder.getAbsolutePath());
        }
    }

    /**
     * Gets a data object by key, loading it if necessary.
     * @param key The key of the data object
     * @return The data object, or null if not found
     */
    @Nullable
    public YamlData get(@NotNull String key) {
        return dataMap.computeIfAbsent(key, k -> {
            try {
                return dataClass.getConstructor(JavaPlugin.class, String.class, DataType.class)
                        .newInstance(plugin, directoryName + "/" + key + ".yml", dataType);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to create data instance for key: " + key);
                e.printStackTrace();
                return null;
            }
        });
    }

    /**
     * Puts a data object into the map.
     * @param key The key to associate with the data
     * @param data The data object to store
     */
    public void put(@NotNull String key, @NotNull YamlData data) {
        dataMap.put(key, data);
    }

    /**
     * Removes a data object from the map.
     * @param key The key of the data to remove
     * @return The removed data object, or null if not found
     */
    @Nullable
    public YamlData remove(@NotNull String key) {
        return dataMap.remove(key);
    }

    /**
     * Checks if a data object exists for the given key.
     * @param key The key to check
     * @return true if the data object exists
     */
    public boolean containsKey(@NotNull String key) {
        return dataMap.containsKey(key);
    }

    /**
     * Gets all keys in the map.
     * @return A set of all keys
     */
    @NotNull
    public Set<String> keySet() {
        return Collections.unmodifiableSet(dataMap.keySet());
    }

    /**
     * Gets all data objects in the map.
     * @return A collection of all data objects
     */
    @NotNull
    public Collection<YamlData> values() {
        return Collections.unmodifiableCollection(dataMap.values());
    }

    /**
     * Clears all data from the map.
     */
    public void clear() {
        dataMap.clear();
    }

    /**
     * Gets the number of data objects in the map.
     * @return The number of data objects
     */
    public int size() {
        return dataMap.size();
    }

    /**
     * Checks if the map is empty.
     * @return true if the map is empty
     */
    public boolean isEmpty() {
        return dataMap.isEmpty();
    }

    @Override
    @NotNull
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public void exportData() {
        for (YamlData data : dataMap.values()) {
            if (data != null) {
                data.exportData();
            }
        }
    }

    @Override
    public void importData(YamlConfiguration data) {
        // Not supported for MapData
        throw new UnsupportedOperationException("importData is not supported for MapData");
    }

    /**
     * Loads all data files from the directory.
     */
    public void loadAll() {
        File dir = new File(plugin.getDataFolder(), directoryName);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            String key = file.getName().replace(".yml", "");
            if (!dataMap.containsKey(key)) {
                get(key); // This will load the data if it's not already loaded
            }
        }
    }

    /**
     * Gets the directory where the data files are stored.
     * @return The data directory
     */
    @NotNull
    public File getDataDirectory() {
        return new File(plugin.getDataFolder(), directoryName);
    }
}
