package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.api.logger.DLogNode;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A type-safe container for managing data in a Bukkit plugin, extending HashMap.
 * Stores key-value pairs with specific type constraints based on DataType:
 * - USER: Keys must be UUID, values must be YamlConfiguration.
 * - YAML: Keys must be UUID or String, values must be YamlConfiguration.
 * - CUSTOM: Keys must be UUID or String, values must implement DataCargo.
 *
 * @param <K> The key type (UUID for USER; UUID or String for YAML and CUSTOM)
 * @param <V> The value type (YamlConfiguration for USER and YAML, DataCargo for CUSTOM)
 */
@DPPCoreVersion(since = "5.3.0")
public class DataContainer<K, V> extends HashMap<K, V> implements IDataHandler<K, V> {
    private final DPlugin plugin;
    private final DataType dataType;
    private final DLogNode logger;
    private String path;

    /**
     * Constructs a DataContainer with the specified plugin and data type.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     */
    public DataContainer(DPlugin plugin, DataType dataType) {
        super();
        this.plugin = plugin;
        this.dataType = dataType;
        this.logger = plugin.getLog();
        this.path = dataType == DataType.USER ? "udata" : "data";
    }

    /**
     * Constructs a DataContainer with a custom path.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     * @param path     The custom directory path for data storage.
     */
    public DataContainer(DPlugin plugin, DataType dataType, String path) {
        this(plugin, dataType);
        this.path = path != null ? path : this.path;
    }

    public DPlugin getPlugin() {
        return plugin;
    }

    public DataType getDataType() {
        return dataType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : (dataType == DataType.USER ? "udata" : "data");
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
     * Saves the data associated with the specified key to a file.
     *
     * @param key  The key to save.
     * @param path The custom directory path (optional).
     * @throws IllegalArgumentException If the key or value type is invalid.
     */
    public void save(K key, String path) {
        setPath(path);
        save(key);
    }

    /**
     * Saves the data associated with the specified key to a file.
     *
     * @param key The key to save.
     * @throws IllegalArgumentException If the key or value type is invalid.
     */
    public void save(K key) {
        String fileName;
        try {
            fileName = getFileName(key);
        } catch (IllegalArgumentException e) {
            logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
            return;
        }
        V value = get(key);
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
     * Saves all data entries to files.
     */
    public void saveAll() {
        String savePath = path;
        for (Map.Entry<K, V> entry : entrySet()) {
            K key = entry.getKey();
            String fileName;
            try {
                fileName = getFileName(key);
            } catch (IllegalArgumentException e) {
                logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
                continue;
            }
            V value = entry.getValue();
            try {
                validateValue(value, key);
            } catch (IllegalArgumentException e) {
                logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
                continue;
            }
            if (dataType == DataType.CUSTOM) {
                Object serialized = ((DataCargo) value).serialize();
                if (!(serialized instanceof YamlConfiguration)) {
                    logger.warning("Serialized data is not a YamlConfiguration for key: " + key, DLogManager.printDataContainerLogs);
                    continue;
                }
                ConfigUtils.saveCustomData(plugin, (YamlConfiguration) serialized, fileName, savePath);
            } else {
                ConfigUtils.saveCustomData(plugin, (YamlConfiguration) value, fileName, savePath);
            }
        }
    }

    /**
     * Loads data for the specified key from a file.
     *
     * @param key   The key to load.
     * @param clazz The expected class of the value (must implement DataCargo for CUSTOM).
     * @param path  The custom directory path (optional).
     * @return This DataContainer for method chaining.
     * @throws IllegalArgumentException If the key type or clazz is invalid.
     */
    public DataContainer<K, V> load(K key, Class<?> clazz, String path) {
        setPath(path);
        return load(key, clazz);
    }

    /**
     * Loads data for the specified key from a file.
     *
     * @param key   The key to load.
     * @param clazz The expected class of the value (must implement DataCargo for CUSTOM).
     * @return This DataContainer for method chaining.
     * @throws IllegalArgumentException If the key type or clazz is invalid.
     */
    public DataContainer<K, V> load(K key, Class<?> clazz) {
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
            return this;
        }
        if (dataType == DataType.CUSTOM) {
            if (!DataCargo.class.isAssignableFrom(clazz)) {
                logger.warning("Class " + clazz.getSimpleName() + " does not implement DataCargo for key " + key, DLogManager.printDataContainerLogs);
                return this;
            }
            try {
                DataCargo dataCargo = (DataCargo) clazz.getDeclaredConstructor().newInstance();
                Object value = dataCargo.deserialize(data);
                if (clazz.isInstance(value)) {
                    put(key, (V) value);
                } else {
                    logger.warning("Type mismatch on load for key " + key + ": Value not compatible with " + clazz.getSimpleName(), DLogManager.printDataContainerLogs);
                }
            } catch (Exception e) {
                logger.warning("Failed to load CUSTOM data for key " + key + " in " + clazz.getSimpleName() + ": " + e.getMessage(), DLogManager.printDataContainerLogs);
            }
        } else {
            put(key, (V) data);
        }
        return this;
    }

    /**
     * Loads all data from the specified directory.
     *
     * @param clazz The expected class of the values (must implement DataCargo for CUSTOM).
     * @return This DataContainer for method chaining.
     */
    public DataContainer<K, V> loadAll(@Nullable Class<?> clazz) {
        String loadPath = path;
        HashMap<String, YamlConfiguration> dataMap = ConfigUtils.loadCustomDataMap(plugin, loadPath);
        if (dataType == DataType.CUSTOM) {
            if (clazz == null) {
                logger.warning("Class parameter is null for CUSTOM data type.", DLogManager.printDataContainerLogs);
                return this;
            }
            if (!DataCargo.class.isAssignableFrom(clazz)) {
                logger.warning("Class " + clazz.getSimpleName() + " does not implement DataCargo.", DLogManager.printDataContainerLogs);
                return this;
            }
        }
        for (Map.Entry<String, YamlConfiguration> entry : dataMap.entrySet()) {
            String strKey = entry.getKey();
            YamlConfiguration data = entry.getValue();
            if (data == null) {
                continue;
            }
            try {
                K key;
                if (dataType == DataType.USER) {
                    key = (K) UUID.fromString(strKey);
                } else {
                    try {
                        key = (K) UUID.fromString(strKey);
                    } catch (IllegalArgumentException e) {
                        key = (K) strKey;
                    }
                }
                if (dataType == DataType.CUSTOM) {
                    DataCargo dataCargo = (DataCargo) clazz.getDeclaredConstructor().newInstance();
                    Object value = dataCargo.deserialize(data);
                    if (clazz.isInstance(value)) {
                        put(key, (V) value);
                    } else {
                        logger.warning("Type mismatch on loadAll for key " + strKey + ": Value not compatible with " + clazz.getSimpleName(), DLogManager.printDataContainerLogs);
                    }
                } else {
                    put(key, (V) data);
                }
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid UUID format for USER key: " + strKey, DLogManager.printDataContainerLogs);
            } catch (Exception e) {
                logger.warning("Failed to load data for key " + strKey + (clazz != null ? " in " + clazz.getSimpleName() : "") + ": " + e.getMessage(), DLogManager.printDataContainerLogs);
            }
        }
        return this;
    }
}