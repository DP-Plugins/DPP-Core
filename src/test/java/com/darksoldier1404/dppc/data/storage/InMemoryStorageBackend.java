package com.darksoldier1404.dppc.data.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple in-memory {@link StorageBackend} used to exercise routing logic
 * (e.g. {@link CompositeStorage}) without touching a real database or the file system.
 */
public class InMemoryStorageBackend implements StorageBackend {

    public final Map<String, String> map = new HashMap<>();
    public boolean closed = false;

    @Override
    public void save(@NotNull String key, @NotNull String yaml) {
        map.put(key, yaml);
    }

    @Override
    @Nullable
    public String load(@NotNull String key) {
        return map.get(key);
    }

    @Override
    @NotNull
    public Map<String, String> loadAll() {
        return new HashMap<>(map);
    }

    @Override
    public boolean delete(@NotNull String key) {
        return map.remove(key) != null;
    }

    @Override
    public void close() {
        closed = true;
    }
}
