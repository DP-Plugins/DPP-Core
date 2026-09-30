package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Routes I/O across a database backend and a file backend when a container is
 * configured to use both.
 * <ul>
 *   <li><b>Writes</b> go to <i>both</i> backends; a failure in one never blocks the other.</li>
 *   <li><b>Reads</b> prefer the database; when it returns nothing (or fails) they
 *       fall back to the file. This makes the file a natural local backup and gives
 *       a seamless file&rarr;database migration path: the first load reads files,
 *       every subsequent load reads the database.</li>
 * </ul>
 */
@DPPCoreVersion(since = "5.4.4")
public class CompositeStorage implements StorageBackend {

    private final StorageBackend db;
    private final StorageBackend file;

    public CompositeStorage(@NotNull StorageBackend db, @NotNull StorageBackend file) {
        this.db = db;
        this.file = file;
    }

    public StorageBackend getDatabaseBackend() {
        return db;
    }

    public StorageBackend getFileBackend() {
        return file;
    }

    @Override
    public void save(@NotNull String key, @NotNull String yaml) {
        file.save(key, yaml);
        db.save(key, yaml);
    }

    @Override
    @Nullable
    public String load(@NotNull String key) {
        String value = db.load(key);
        if (value != null) {
            return value;
        }
        return file.load(key);
    }

    @Override
    @NotNull
    public Map<String, String> loadAll() {
        Map<String, String> dbData = db.loadAll();
        if (!dbData.isEmpty()) {
            return dbData;
        }
        return file.loadAll();
    }

    @Override
    public boolean delete(@NotNull String key) {
        boolean fileRemoved = file.delete(key);
        boolean dbRemoved = db.delete(key);
        return fileRemoved || dbRemoved;
    }

    /**
     * Runs the atomic compute against the authoritative database, then mirrors the
     * resulting value to the file backup. The CAS guarantee comes entirely from the
     * database; the file is a best-effort copy.
     */
    @Override
    @Nullable
    public String compute(@NotNull String key, @NotNull UnaryOperator<String> remapper) {
        String result = db.compute(key, remapper);
        if (result != null) {
            file.save(key, result);
        }
        return result;
    }

    @Override
    public void close() {
        try {
            db.close();
        } finally {
            file.close();
        }
    }
}
