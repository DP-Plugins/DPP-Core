package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Abstraction over a place where serialized container data is persisted.
 * <p>
 * Every backend deals only with {@code String key -> serialized YAML String}
 * pairs. The {@code DataContainer} / {@code SingleDataContainer} keep all of the
 * type handling and (de)serialization logic; a backend is purely responsible for
 * the final I/O against a file, a relational database or Redis.
 * <p>
 * Implementations must never throw on I/O failure: they log a warning and degrade
 * gracefully (returning {@code null} / an empty map / {@code false}) so a database
 * outage can never crash the server.
 */
@DPPCoreVersion(since = "5.4.4")
public interface StorageBackend {

    /**
     * Persists the serialized YAML value under the given key, overwriting any
     * existing value.
     *
     * @param key  the sanitized container key (file name / row key / hash field)
     * @param yaml the serialized YAML document
     */
    void save(@NotNull String key, @NotNull String yaml);

    /**
     * Loads the serialized YAML value for the given key.
     *
     * @param key the sanitized container key
     * @return the stored YAML string, or {@code null} if absent / unavailable
     */
    @Nullable
    String load(@NotNull String key);

    /**
     * Loads every key/value pair held by this backend.
     *
     * @return a map of key to serialized YAML (never {@code null}; empty if none)
     */
    @NotNull
    Map<String, String> loadAll();

    /**
     * Deletes the value stored under the given key.
     *
     * @param key the sanitized container key
     * @return {@code true} if a value was removed
     */
    boolean delete(@NotNull String key);

    /**
     * Atomically reads the current value, applies {@code remapper} to compute the
     * next value, and stores it — retrying on concurrent modification so the
     * read-modify-write is safe even when multiple servers share the backend.
     * <p>
     * {@code remapper} receives the current serialized YAML (or {@code null} if the
     * key is absent) and returns the new YAML to store, or {@code null} to abort
     * without writing. Because it may be invoked several times (once per retry), it
     * <b>must be a pure function with no external side effects</b>.
     *
     * @return the value left in storage: the newly written YAML, the unchanged
     * current value if {@code remapper} aborted, or {@code null} if the operation
     * could not be completed (e.g. retries exhausted or backend unavailable)
     */
    @DPPCoreVersion(since = "5.4.4")
    @Nullable
    default String compute(@NotNull String key, @NotNull UnaryOperator<String> remapper) {
        // Default: non-atomic read-modify-write, sufficient for single-process
        // (file) storage. Database backends override this with a real CAS loop.
        String current = load(key);
        String next = remapper.apply(current);
        if (next == null) {
            return current;
        }
        save(key, next);
        return next;
    }

    /**
     * Releases any resources (connection pools, sockets) held by this backend.
     * Safe to call multiple times.
     */
    void close();
}
