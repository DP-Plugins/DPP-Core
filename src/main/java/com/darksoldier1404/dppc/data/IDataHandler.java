package com.darksoldier1404.dppc.data;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

@DPPCoreVersion(since = "5.3.0")
public interface IDataHandler<K, V> {
    void saveAll();

    IDataHandler<K, V> loadAll(@Nullable Class<?> clazz);

    String getPath();

    void setPath(String path);

    JavaPlugin getPlugin();

    DataType getDataType();
}