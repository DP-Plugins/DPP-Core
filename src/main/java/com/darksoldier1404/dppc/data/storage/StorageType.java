package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;

/**
 * The kind of database backend a {@link StorageSettings} targets.
 * {@link #NONE} means no database is used (file storage only).
 */
@DPPCoreVersion(since = "5.4.4")
public enum StorageType {
    NONE,
    MYSQL,
    SQLITE,
    REDIS
}
