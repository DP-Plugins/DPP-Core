package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.api.logger.DLogNode;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A type-safe container for managing a single piece of data in a Bukkit plugin.
 * Based on DataContainer, but holds only one key-value pair instead of a map.
 * Supports the same DataType constraints:
 * - USER: Key must be UUID, value must be YamlConfiguration.
 * - YAML: Key must be UUID or String, value must be YamlConfiguration.
 * - CUSTOM: Key must be UUID or String, value must implement DataCargo.
 *
 * @param <K> The key type (UUID for USER; UUID or String for YAML and CUSTOM)
 * @param <V> The value type (YamlConfiguration for USER and YAML, DataCargo for CUSTOM)
 */
@DPPCoreVersion(since = "5.3.0")
public class SingleDataContainer<K, V> implements IDataHandler<K, V> {
    private final DPlugin plugin;
    private final DataType dataType;
    private final DLogNode logger;
    private String path;
    private K key;
    private V value;

    /**
     * Constructs a SingleDataContainer with the specified plugin and data type.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     */
    public SingleDataContainer(DPlugin plugin, DataType dataType) {
        this.plugin = plugin;
        this.dataType = dataType;
        this.logger = plugin.getLog();
        this.path = dataType == DataType.USER ? "udata" : "data";
        this.key = null;
        this.value = null;
    }

    /**
     * Constructs a SingleDataContainer with a custom path.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     * @param path     The custom directory path for data storage.
     */
    public SingleDataContainer(DPlugin plugin, DataType dataType, String path) {
        this(plugin, dataType);
        this.path = path != null ? path : this.path;
    }

    @Override
    public JavaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public DataType getDataType() {
        return dataType;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public void setPath(String path) {
        this.path = path != null ? path : (dataType == DataType.USER ? "udata" : "data");
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    /**
     * Sets the key and value, validating types based on DataType.
     *
     * @param key   The key to set.
     * @param value The value to set.
     * @throws IllegalArgumentException If the key or value type is invalid.
     */
    public void set(K key, V value) {
        try {
            validateKey(key);
            validateValue(value, key);
            this.key = key;
            this.value = value;
        } catch (IllegalArgumentException e) {
            logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
            this.key = null;
            this.value = null;
        }
    }

    /**
     * Validates and returns the file name for the given key.
     *
     * @param key The key to validate.
     * @return The sanitized file name.
     * @throws IllegalArgumentException If the key type is invalid for the DataType.
     */
    private String getFileName(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }
        if (dataType == DataType.USER) {
            if (!(key instanceof UUID)) {
                throw new IllegalArgumentException("Invalid key type for USER: Expected UUID but found " + key.getClass().getSimpleName());
            }
        } else {
            if (!(key instanceof UUID || key instanceof String)) {
                throw new IllegalArgumentException("Invalid key type for " + dataType + ": Expected UUID or String but found " + key.getClass().getSimpleName());
            }
        }
        return key.toString().replace(".yml", "");
    }

    /**
     * Validates the key type for the given DataType.
     *
     * @param key The key to validate.
     * @throws IllegalArgumentException If the key type is invalid.
     */
    private void validateKey(K key) {
        getFileName(key); // Reuses validation from getFileName
    }

    /**
     * Validates the value type for the given DataType.
     *
     * @param value The value to validate.
     * @param key   The associated key for error reporting.
     * @throws IllegalArgumentException If the value type is invalid.
     */
    private void validateValue(V value, K key) {
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null for key: " + key);
        }
        if (dataType == DataType.CUSTOM) {
            if (!(value instanceof DataCargo)) {
                throw new IllegalArgumentException("Invalid value type for CUSTOM: Expected DataCargo but found " + value.getClass().getSimpleName() + " for key " + key);
            }
        } else {
            if (!(value instanceof YamlConfiguration)) {
                throw new IllegalArgumentException("Invalid value type for " + dataType + ": Expected YamlConfiguration but found " + value.getClass().getSimpleName() + " for key " + key);
            }
        }
    }

    /**
     * Saves the data to a file using the current key and path.
     *
     * @param path The custom directory path (optional).
     * @throws IllegalArgumentException If the key or value is invalid or null.
     */
    public void save(String path) {
        setPath(path);
        save();
    }

    /**
     * Saves the data to a file using the current key and path.
     *
     * @throws IllegalArgumentException If the key or value is invalid or null.
     */
    public void save() {
        if (key == null || value == null) {
            logger.warning("Cannot save: Key or value is null", DLogManager.printDataContainerLogs);
            return;
        }
        String fileName;
        try {
            fileName = getFileName(key);
        } catch (IllegalArgumentException e) {
            logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
            return;
        }
        try {
            validateValue(value, key);
        } catch (IllegalArgumentException e) {
            logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
            return;
        }
        String savePath = path;
        if (dataType == DataType.CUSTOM) {
            Object serialized = ((DataCargo) value).serialize();
            if (!(serialized instanceof YamlConfiguration)) {
                logger.warning("Serialized data is not a YamlConfiguration for key: " + key, DLogManager.printDataContainerLogs);
                return;
            }
            ConfigUtils.saveCustomData(plugin, (YamlConfiguration) serialized, fileName, savePath);
        } else {
            ConfigUtils.saveCustomData(plugin, (YamlConfiguration) value, fileName, savePath);
        }
    }

    /**
     * Loads data from a file using the specified key and populates the value.
     *
     * @param key   The key to load.
     * @param clazz The expected class of the value (must implement DataCargo for CUSTOM).
     * @param path  The custom directory path (optional).
     * @return This SingleDataContainer for method chaining.
     * @throws IllegalArgumentException If the key type or clazz is invalid.
     */
    public SingleDataContainer<K, V> load(K key, Class<?> clazz, String path) {
        setPath(path);
        return load(key, clazz);
    }

    /**
     * Loads data from a file using the specified key and populates the value.
     *
     * @param key   The key to load.
     * @param clazz The expected class of the value (must implement DataCargo for CUSTOM).
     * @return This SingleDataContainer for method chaining.
     * @throws IllegalArgumentException If the key type or clazz is invalid.
     */
    public SingleDataContainer<K, V> load(K key, Class<?> clazz) {
        this.key = key; // Set the key
        String fileName;
        try {
            fileName = getFileName(key);
        } catch (IllegalArgumentException e) {
            logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
            return this;
        }
        String loadPath = path;
        YamlConfiguration data = ConfigUtils.loadCustomData(plugin, fileName, loadPath);
        if (data == null) {
            this.value = null;
            return this;
        }
        if (dataType == DataType.CUSTOM) {
            if (!DataCargo.class.isAssignableFrom(clazz)) {
                logger.warning("Class " + clazz.getSimpleName() + " does not implement DataCargo for key " + key, DLogManager.printDataContainerLogs);
                return this;
            }
            try {
                DataCargo dataCargo = (DataCargo) clazz.getDeclaredConstructor().newInstance();
                Object loadedValue = dataCargo.deserialize(data);
                if (clazz.isInstance(loadedValue)) {
                    this.value = (V) loadedValue;
                } else {
                    logger.warning("Type mismatch on load for key " + key + ": Value not compatible with " + clazz.getSimpleName(), DLogManager.printDataContainerLogs);
                    this.value = null;
                }
            } catch (Exception e) {
                logger.warning("Failed to load CUSTOM data for key " + key + " in " + clazz.getSimpleName() + ": " + e.getMessage(), DLogManager.printDataContainerLogs);
                this.value = null;
            }
        } else {
            this.value = (V) data;
        }
        return this;
    }

    /**
     * Clears the key and value.
     */
    public void clear() {
        this.key = null;
        this.value = null;
    }

    /**
     * Checks if this container has valid data.
     *
     * @return true if key and value are both non-null.
     */
    public boolean hasData() {
        return key != null && value != null;
    }

    @Override
    public void saveAll() {
        save();
    }

    @Override
    public SingleDataContainer<K, V> loadAll(@Nullable Class<?> clazz) {
        if (hasData()) {
            load(key, clazz);
        } else {
            logger.warning("Cannot loadAll: No key set for SingleDataContainer with path '" + path + "'", DLogManager.printDataContainerLogs);
        }
        return this;
    }
}