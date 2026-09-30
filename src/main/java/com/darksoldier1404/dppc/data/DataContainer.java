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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

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
public class DataContainer<K, V> extends HashMap<K, V> implements IDataHandler<K, V>, Creatable<K, V> {
    private final DPlugin plugin;
    private final DataType dataType;
    private final DLogNode logger;
    private final StorageBackend backend;
    private String path;

    // Real-time sync (optional). Null when sync is disabled.
    private final SyncManager syncManager;
    private final String syncSignature;
    private final String containerId;
    // Remembered for remote reloads of CUSTOM values (set on load/loadAll/compute).
    private volatile Class<?> lastClazz;

    /**
     * Constructs a file-backed DataContainer with the specified plugin and data type.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     */
    public DataContainer(DPlugin plugin, DataType dataType) {
        this(plugin, dataType, (String) null, StorageSettings.fileOnly());
    }

    /**
     * Constructs a file-backed DataContainer with a custom path.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     * @param path     The custom directory path for data storage.
     */
    public DataContainer(DPlugin plugin, DataType dataType, String path) {
        this(plugin, dataType, path, StorageSettings.fileOnly());
    }

    /**
     * Constructs a DataContainer with explicit storage settings (file and/or database),
     * using the default path for the data type.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     * @param settings The storage settings (file only, database only, or both).
     */
    @DPPCoreVersion(since = "5.4.4")
    public DataContainer(DPlugin plugin, DataType dataType, StorageSettings settings) {
        this(plugin, dataType, null, settings);
    }

    /**
     * Constructs a DataContainer with a custom path and explicit storage settings.
     *
     * @param plugin   The JavaPlugin instance.
     * @param dataType The type of data to manage (USER, YAML, or CUSTOM).
     * @param path     The custom directory path for data storage (also the DB table / Redis key suffix).
     * @param settings The storage settings (file only, database only, or both).
     */
    @DPPCoreVersion(since = "5.4.4")
    public DataContainer(DPlugin plugin, DataType dataType, String path, StorageSettings settings) {
        super();
        this.plugin = plugin;
        this.dataType = dataType;
        this.logger = plugin.getLog();
        this.path = path != null ? path : (dataType == DataType.USER ? "udata" : "data");
        this.backend = settings.createBackend(plugin, this::getPath, this.path);
        this.containerId = plugin.getName() + ":" + this.path;
        if (settings.isSyncEnabled()) {
            this.syncSignature = settings.syncSignature();
            SyncManager mgr = null;
            try {
                mgr = SyncManager.shared(syncSignature, () -> settings.createSyncTransport(plugin));
                mgr.register(containerId, new SyncReceiver() {
                    @Override
                    public void onRemoteChange(@NotNull String key, @NotNull SyncOp op) {
                        applyRemoteChange(key, op);
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

    public boolean delete(K key) {
        String fileName;
        try {
            fileName = getFileName(key);
        } catch (IllegalArgumentException e) {
            logger.warning(e.getMessage(), DLogManager.printDataContainerLogs);
            return false;
        }
        boolean success = backend.delete(fileName);
        if (success) {
            logger.info("Deleted data for key " + key, DLogManager.printDataContainerLogs);
            remove(key);
            publishSync(fileName, SyncOp.DELETE);
            return true;
        } else {
            logger.warning("No data found to delete for key " + key, DLogManager.printDataContainerLogs);
            return false;
        }
    }

    /**
     * Serializes a container value to a YAML string, or returns {@code null} (and
     * logs) if the value cannot be represented as a YamlConfiguration.
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
        String yaml = serializeToString(value, key);
        if (yaml == null) {
            return;
        }
        backend.save(fileName, yaml);
        publishSync(fileName, SyncOp.UPSERT);
    }

    /**
     * Saves all data entries to the configured storage backend(s).
     */
    public void saveAll() {
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
            String yaml = serializeToString(value, key);
            if (yaml == null) {
                continue;
            }
            backend.save(fileName, yaml);
            publishSync(fileName, SyncOp.UPSERT);
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
            return this;
        }
        YamlConfiguration data = new YamlConfiguration();
        try {
            data.loadFromString(yaml);
        } catch (Exception e) {
            logger.warning("Failed to parse stored data for key " + key + ": " + e.getMessage(), DLogManager.printDataContainerLogs);
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
        this.lastClazz = clazz;
        Map<String, String> dataMap = backend.loadAll();
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
        for (Map.Entry<String, String> entry : dataMap.entrySet()) {
            String strKey = entry.getKey();
            if (entry.getValue() == null) {
                continue;
            }
            YamlConfiguration data = new YamlConfiguration();
            try {
                data.loadFromString(entry.getValue());
            } catch (Exception e) {
                logger.warning("Failed to parse stored data for key " + strKey + ": " + e.getMessage(), DLogManager.printDataContainerLogs);
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

    @DPPCoreVersion(since = "5.4.0")
    @Nullable
    @Override
    public V create(@NotNull K key, @NotNull Class<V> clazz) {
        try {
            if (!DataCargo.class.isAssignableFrom(clazz) && dataType == DataType.CUSTOM) {
                logger.warning("Class " + clazz.getSimpleName() + " does not implement DataCargo for CUSTOM data type.", DLogManager.printDataContainerLogs);
                return null;
            }
            if (containsKey(key)) {
                logger.warning("Key " + key + " already exists. Creation skipped.", DLogManager.printDataContainerLogs);
                return get(key);
            }
            DataCargo dataCargo = (DataCargo) clazz.getDeclaredConstructor().newInstance();
            put(key, (V) dataCargo);
            return (V) dataCargo;
        } catch (Exception e) {
            logger.warning("Failed to create instance of " + clazz.getSimpleName() + ": " + e.getMessage(), DLogManager.printDataContainerLogs);
            return null;
        }
    }

    @DPPCoreVersion(since = "5.4.0")
    @Nullable
    @Override
    public V createAndSave(@NotNull K key, @NotNull Class<V> clazz) {
        V dataCargo = create(key, clazz);
        if (dataCargo != null) {
            save(key);
            return dataCargo;
        }
        return null;
    }

    /**
     * Atomically read-modify-writes the value for {@code key}, safe against
     * concurrent writers on other servers (optimistic CAS in the backend, with
     * retries). The {@code mutator} receives the current value (or a fresh instance
     * if the key is absent) and mutates it in place.
     * <p>
     * Because the mutator may run several times (once per CAS retry) it
     * <b>must be free of external side effects</b> — perform money/item/message
     * effects only after this returns a non-null result. To cancel without writing,
     * throw {@link AbortTransactionException} from the mutator.
     *
     * @return the committed value, or {@code null} if the mutator aborted or the
     * operation could not complete
     */
    @DPPCoreVersion(since = "5.4.4")
    @Nullable
    public V compute(@NotNull K key, @NotNull Class<V> clazz, @NotNull Consumer<V> mutator) {
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
            V value = materialize(currentYaml, clazz);
            if (value == null) {
                return null;
            }
            try {
                mutator.accept(value);
            } catch (AbortTransactionException abort) {
                return null;
            }
            committed[0] = value;
            return serializeToString(value, key);
        });
        if (resultYaml == null || committed[0] == null) {
            return null; // aborted or failed: nothing committed
        }
        @SuppressWarnings("unchecked")
        V finalValue = (V) committed[0];
        put(key, finalValue);
        publishSync(fileName, SyncOp.UPSERT);
        return finalValue;
    }

    /** Builds a working value from stored YAML (or a fresh instance when absent). */
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

    /**
     * Applies a change made on another server. Invoked on the sync subscriber
     * thread; database reads happen here, but the in-memory mutation is marshalled
     * onto the main thread for thread safety.
     */
    void applyRemoteChange(@NotNull String keyStr, @NotNull SyncOp op) {
        if (op == SyncOp.DELETE) {
            runOnMain(() -> removeByRawKey(keyStr));
            return;
        }
        String yaml = backend.load(keyStr);
        if (yaml == null) {
            runOnMain(() -> removeByRawKey(keyStr));
            return;
        }
        Object value = buildValue(yaml);
        if (value == null) {
            return;
        }
        runOnMain(() -> putRawKey(keyStr, value));
    }

    /** Reloads everything after a sync reconnect (best-effort). */
    void applyResync() {
        final Class<?> clazz = lastClazz;
        runOnMain(() -> loadAll(clazz));
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
                logger.warning("Cannot apply remote CUSTOM change for " + containerId + ": value class unknown (call loadAll(clazz) first)", DLogManager.printDataContainerLogs);
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

    @SuppressWarnings("unchecked")
    private void putRawKey(String strKey, Object value) {
        try {
            put(keyFromString(strKey), (V) value);
        } catch (IllegalArgumentException e) {
            logger.warning("Invalid key on remote apply: " + strKey, DLogManager.printDataContainerLogs);
        }
    }

    private void removeByRawKey(String strKey) {
        try {
            remove(keyFromString(strKey));
        } catch (IllegalArgumentException ignored) {
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
            // Scheduler unavailable (e.g. during shutdown): drop the update.
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

    /**
     * Thrown from a {@link #compute} mutator to cancel the transaction without
     * writing. Carries no stack trace (control-flow signal, not an error).
     */
    @DPPCoreVersion(since = "5.4.4")
    public static class AbortTransactionException extends RuntimeException {
        public AbortTransactionException() {
            super(null, null, false, false);
        }

        public AbortTransactionException(String message) {
            super(message, null, false, false);
        }
    }
}