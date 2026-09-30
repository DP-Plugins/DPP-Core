package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * {@link StorageBackend} backed by per-key YAML files, reproducing the historical
 * {@code DataContainer} behaviour by delegating to {@link ConfigUtils}.
 * <p>
 * The path is read through a {@link Supplier} so that {@code setPath} on the owning
 * container keeps working exactly as before.
 */
@DPPCoreVersion(since = "5.4.4")
public class YamlStorageBackend implements StorageBackend {

    private final DPlugin plugin;
    private final Supplier<String> pathSupplier;

    public YamlStorageBackend(@NotNull DPlugin plugin, @NotNull Supplier<String> pathSupplier) {
        this.plugin = plugin;
        this.pathSupplier = pathSupplier;
    }

    private String path() {
        return pathSupplier.get();
    }

    @Override
    public void save(@NotNull String key, @NotNull String yaml) {
        YamlConfiguration data = new YamlConfiguration();
        try {
            data.loadFromString(yaml);
        } catch (Exception e) {
            plugin.getLog().warning("Failed to parse YAML for key " + key + ": " + e.getMessage(), DLogManager.printStorageLogs);
            return;
        }
        ConfigUtils.saveCustomData(plugin, data, key, path());
    }

    @Override
    @Nullable
    public String load(@NotNull String key) {
        YamlConfiguration data = ConfigUtils.loadCustomData(plugin, key, path());
        return data == null ? null : data.saveToString();
    }

    @Override
    @NotNull
    public Map<String, String> loadAll() {
        Map<String, String> result = new HashMap<>();
        HashMap<String, YamlConfiguration> dataMap = ConfigUtils.loadCustomDataMap(plugin, path());
        for (Map.Entry<String, YamlConfiguration> entry : dataMap.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue().saveToString());
            }
        }
        return result;
    }

    @Override
    public boolean delete(@NotNull String key) {
        try {
            return Files.deleteIfExists(Path.of(plugin.getDataFolder().getPath(), path() + "/" + key + ".yml"));
        } catch (IOException e) {
            plugin.getLog().warning("Failed to delete file for key " + key + ": " + e.getMessage(), DLogManager.printStorageLogs);
            return false;
        }
    }

    @Override
    public void close() {
        // Nothing to release for file storage.
    }
}
