package com.darksoldier1404.dppc.data.sync;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.jetbrains.annotations.NotNull;

/**
 * Callback a container registers with {@link SyncManager} to be notified of
 * changes made on <i>other</i> servers. Invoked on the transport's subscriber
 * thread; implementations are responsible for moving any Bukkit-state mutation
 * onto the main thread.
 */
@DPPCoreVersion(since = "5.4.4")
@FunctionalInterface
public interface SyncReceiver {

    /**
     * @param key the sanitized container key that changed remotely
     * @param op  whether the key was upserted or deleted
     */
    void onRemoteChange(@NotNull String key, @NotNull SyncOp op);

    /**
     * Invoked after the transport reconnects following a dropped subscription, so
     * the container can reload everything it may have missed while disconnected.
     * Default no-op.
     */
    default void onResync() {
    }
}
