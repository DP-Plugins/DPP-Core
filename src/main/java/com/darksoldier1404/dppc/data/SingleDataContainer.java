package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.api.logger.DLogNode;
import com.darksoldier1404.dppc.data.storage.StorageBackend;
import com.darksoldier1404.dppc.data.storage.StorageSettings;
import com.darksoldier1404.dppc.data.sync.SyncManager;
import com.darksoldier1404.dppc.data.sync.SyncOp;
import com.darksoldier1404.dppc.data.sync.SyncReceiver;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

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
    private final StorageBackend backend;
    private String path;
    private K key;
    private V value;

    // Real-time sync (optional). Null when sync is disabled.
    private final SyncManager syncManager;
    private final String syncSignature;
    private final String containerId;
    private volatile Class<?> lastClazz;

    /**
     * Constructs a file-backed SingleDataContainer with the specified plugin and data type.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     */
    public SingleDataContainer(DPlugin plugin, DataType dataType) {
        this(plugin, dataType, (String) null, StorageSettings.fileOnly());
    }

    /**
     * Constructs a file-backed SingleDataContainer with a custom path.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     * @param path     The custom directory path for data storage.
     */
    public SingleDataContainer(DPlugin plugin, DataType dataType, String path) {
        this(plugin, dataType, path, StorageSettings.fileOnly());
    }

    /**
     * Constructs a SingleDataContainer with explicit storage settings, using the
     * default path for the data type.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     * @param settings The storage settings (file only, database only, or both).
     */
    @DPPCoreVersion(since = "5.4.4")
    public SingleDataContainer(DPlugin plugin, DataType dataType, StorageSettings settings) {
        this(plugin, dataType, null, settings);
    }

    /**
     * Constructs a SingleDataContainer with a custom path and explicit storage settings.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     * @param path     The custom directory path (also the DB table / Redis key suffix).
     * @param settings The storage settings (file only, database only, or both).
     */
    @DPPCoreVersion(since = "5.4.4")
    public SingleDataContainer(DPlugin plugin, DataType dataType, String path, StorageSettings settings) {
        this.plugin = plugin;
        this.dataType = dataType;
        this.logger = plugin.getLog();
        this.path = path != null ? path : (dataType == DataType.USER ? "udata" : "data");
        this.backend = settings.createBackend(plugin, this::getPath, this.path);
        this.key = null;
        this.value = null;
        this.containerId = plugin.getName() + ":single:" + this.path;
        if (settings.isSyncEnabled()) {
            this.syncSignature = settings.syncSignature();
            SyncManager mgr = null;
            try {
                mgr = SyncManager.shared(syncSignature, () -> settings.createSyncTransport(plugin));
                mgr.register(containerId, new SyncReceiver() {
                    @Override
                    public void onRemoteChange(@NotNull String k, @NotNull SyncOp op) {
                        applyRemoteChange(k, op);
                    }

                    @Override
                    public void onResync() {
                        applyResync();
                    }
                });
            } catch (Exception e) {
                logger.warning("Failed to enable sync for " + containerId + ": " + e.getMessage(), DLogManager.printStorageLogs);
                mgr = null;
            }
            this.syncManager = mgr;
        } else {
            this.syncSignature = null;
            this.syncManager = null;
        }
    }

    /**
     * @return the storage backend this container persists through.
     */
    @DPPCoreVersion(since = "5.4.4")
    public StorageBackend getBackend() {
        return backend;
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
        String yaml = serializeToString(value, key);
        if (yaml == null) {
            return;
        }
        backend.save(fileName, yaml);
        publishSync(fileName, SyncOp.UPSERT);
    }

    /**
     * Serializes the value to a YAML string, or returns {@code null} (and logs) if
     * the value cannot be represented as a YamlConfiguration.
     */
    private String serializeToString(V value, K key) {
        if (dataType == DataType.CUSTOM) {
            Object serialized = ((DataCargo) value).serialize();
            if (!(serialized instanceof YamlConfiguration)) {
                logger.warning("Serialized data is not a YamlConfiguration for key: " + key, DLogManager.printDataContainerLogs);
                return null;
            }
            return ((YamlConfiguration) serialized).saveToString();
        }
        return ((YamlConfiguration) value).saveToString();
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
        this.lastClazz = clazz;
        String fileName;
        try {
            fileName = getFileName(key);
        } catch (IllegalArgumentException e) {
            logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
            return this;
        }
        String yaml = backend.load(fileName);
        if (yaml == null) {
            this.value = null;
            return this;
        }
        YamlConfiguration data = new YamlConfiguration();
        try {
            data.loadFromString(yaml);
        } catch (Exception e) {
            logger.warning("Failed to parse stored data for key " + key + ": " + e.getMessage(), DLogManager.printDataContainerLogs);
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

    /**
     * Atomically read-modify-writes the value for {@code key} (optimistic CAS in
     * the backend). The {@code mutator} mutates the current value (or a fresh
     * instance if absent) in place; it must be side-effect free as it may run
     * multiple times. Throw {@link DataContainer.AbortTransactionException} to cancel.
     *
     * @return the committed value, or {@code null} if aborted/failed
     */
    @DPPCoreVersion(since = "5.4.4")
    @Nullable
    public V compute(K key, Class<V> clazz, Consumer<V> mutator) {
        this.key = key;
        this.lastClazz = clazz;
        String fileName;
        try {
            fileName = getFileName(key);
        } catch (IllegalArgumentException e) {
            logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
            return null;
        }
        final Object[] committed = new Object[1];
        String resultYaml = backend.compute(fileName, currentYaml -> {
            committed[0] = null;
            V v = materialize(currentYaml, clazz);
            if (v == null) {
                return null;
            }
            try {
                mutator.accept(v);
            } catch (DataContainer.AbortTransactionException abort) {
                return null;
            }
            committed[0] = v;
            return serializeToString(v, key);
        });
        if (resultYaml == null || committed[0] == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        V finalValue = (V) committed[0];
        this.value = finalValue;
        publishSync(fileName, SyncOp.UPSERT);
        return finalValue;
    }

    @SuppressWarnings("unchecked")
    private V materialize(String yaml, Class<V> clazz) {
        if (dataType == DataType.CUSTOM) {
            try {
                DataCargo cargo = (DataCargo) clazz.getDeclaredConstructor().newInstance();
                if (yaml == null) {
                    return (V) cargo;
                }
                YamlConfiguration data = new YamlConfiguration();
                data.loadFromString(yaml);
                Object v = cargo.deserialize(data);
                return clazz.isInstance(v) ? (V) v : null;
            } catch (Exception e) {
                logger.warning("compute: failed to materialize CUSTOM value: " + e.getMessage(), DLogManager.printDataContainerLogs);
                return null;
            }
        }
        YamlConfiguration data = new YamlConfiguration();
        if (yaml != null) {
            try {
                data.loadFromString(yaml);
            } catch (Exception e) {
                logger.warning("compute: failed to parse stored value: " + e.getMessage(), DLogManager.printDataContainerLogs);
                return null;
            }
        }
        return (V) data;
    }

    private void publishSync(String keyStr, SyncOp op) {
        if (syncManager != null) {
            syncManager.publish(containerId, keyStr, op);
        }
    }

    /** Applies a change made on another server (subscriber thread -> main thread apply). */
    void applyRemoteChange(@NotNull String keyStr, @NotNull SyncOp op) {
        String mine = (key == null) ? null : keyToFileName(key);
        if (mine != null && !mine.equals(keyStr)) {
            return; // not the key this container tracks
        }
        if (op == SyncOp.DELETE) {
            runOnMain(() -> this.value = null);
            return;
        }
        String yaml = backend.load(keyStr);
        if (yaml == null) {
            runOnMain(() -> this.value = null);
            return;
        }
        Object built = buildValue(yaml);
        if (built == null) {
            return;
        }
        final K k = keyFromString(keyStr);
        @SuppressWarnings("unchecked")
        final V v = (V) built;
        runOnMain(() -> {
            this.key = k;
            this.value = v;
        });
    }

    void applyResync() {
        if (key == null) {
            return;
        }
        final K k = key;
        final Class<?> clazz = lastClazz;
        runOnMain(() -> load(k, clazz));
    }

    private String keyToFileName(K key) {
        try {
            return getFileName(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Object buildValue(String yaml) {
        YamlConfiguration data = new YamlConfiguration();
        try {
            data.loadFromString(yaml);
        } catch (Exception e) {
            return null;
        }
        if (dataType == DataType.CUSTOM) {
            Class<?> clazz = lastClazz;
            if (clazz == null || !DataCargo.class.isAssignableFrom(clazz)) {
                logger.warning("Cannot apply remote CUSTOM change for " + containerId + ": value class unknown", DLogManager.printDataContainerLogs);
                return null;
            }
            try {
                DataCargo cargo = (DataCargo) clazz.getDeclaredConstructor().newInstance();
                Object v = cargo.deserialize(data);
                return clazz.isInstance(v) ? v : null;
            } catch (Exception e) {
                return null;
            }
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private K keyFromString(String strKey) {
        if (dataType == DataType.USER) {
            return (K) UUID.fromString(strKey);
        }
        try {
            return (K) UUID.fromString(strKey);
        } catch (IllegalArgumentException e) {
            return (K) strKey;
        }
    }

    private void runOnMain(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
            return;
        }
        try {
            Bukkit.getScheduler().runTask(plugin, task);
        } catch (Exception ignored) {
        }
    }

    @DPPCoreVersion(since = "5.4.4")
    @Override
    public void close() {
        if (syncManager != null && syncSignature != null) {
            SyncManager.release(syncSignature, containerId);
        }
        backend.close();
    }
}