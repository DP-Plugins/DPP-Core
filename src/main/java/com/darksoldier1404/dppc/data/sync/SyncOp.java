package com.darksoldier1404.dppc.data.sync;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;

/**
 * The kind of change announced by a {@link SyncMessage}.
 */
@DPPCoreVersion(since = "5.4.4")
public enum SyncOp {
    /** A key was created or updated; receivers should reload it. */
    UPSERT,
    /** A key was removed; receivers should evict it. */
    DELETE
}
