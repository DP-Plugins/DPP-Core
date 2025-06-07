package com.darksoldier1404.dppc.datacontainer.obj;

import com.darksoldier1404.dppc.datacontainer.DataType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base interface for all data containers.
 * Defines the common operations for data management.
 */
public interface DData {
    /**
     * Gets the data type of this container.
     * @return The data type
     */
    @NotNull
    DataType getDataType();

    /**
     * Exports the data to its storage location.
     * This should save any pending changes to disk or other persistent storage.
     */
    void exportData();

    /**
     * Imports data from a YAML configuration.
     * @param data The YAML configuration containing the data to import
     */
    void importData(@Nullable YamlConfiguration data);
}