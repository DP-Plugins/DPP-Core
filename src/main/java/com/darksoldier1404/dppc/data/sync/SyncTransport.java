package com.darksoldier1404.dppc.data.sync;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Pub/Sub transport used by {@link SyncManager} to broadcast and receive change
 * announcements. Abstracted so the manager can be tested with an in-memory bus,
 * with {@link RedisSyncTransport} as the production implementation.
 */
@DPPCoreVersion(since = "5.4.4")
public interface SyncTransport {

    /**
     * Broadcasts a raw message to all subscribers (including the sender's own
     * subscription, mirroring Redis Pub/Sub semantics).
     */
    void publish(@NotNull String message);

    /**
     * Begins delivering received messages to {@code onMessage}. Called once.
     */
    void subscribe(@NotNull Consumer<String> onMessage);

    /**
     * Registers a callback invoked whenever the transport re-establishes a dropped
     * subscription (not on the initial connect). Default no-op for transports that
     * cannot disconnect.
     */
    default void setReconnectCallback(@NotNull Runnable callback) {
    }

    /**
     * Stops the subscription and releases resources. Safe to call multiple times.
     */
    void close();
}
