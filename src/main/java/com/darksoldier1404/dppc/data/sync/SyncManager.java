package com.darksoldier1404.dppc.data.sync;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Coordinates real-time change propagation between servers. A manager owns one
 * {@link SyncTransport} subscription and a registry of container receivers; it
 * stamps every outgoing message with this JVM's {@code serverId} and drops
 * incoming messages that carry the same id (its own echoes).
 * <p>
 * Production code uses {@link #shared(String, Supplier)} so all containers that
 * point at the same sync Redis reuse a single manager (and one subscriber thread).
 * The public instance constructor exists so the propagation logic can be unit
 * tested with an in-memory transport and arbitrary server ids.
 */
@DPPCoreVersion(since = "5.4.4")
public class SyncManager {

    /** Stable per-JVM (per-server) identity used to filter out our own echoes. */
    private static final String SERVER_ID = UUID.randomUUID().toString();
    private static final Map<String, SyncManager> SHARED = new ConcurrentHashMap<>();

    private final String serverId;
    private final SyncTransport transport;
    private final Map<String, SyncReceiver> receivers = new ConcurrentHashMap<>();

    public SyncManager(@NotNull String serverId, @NotNull SyncTransport transport) {
        this.serverId = serverId;
        this.transport = transport;
        transport.setReconnectCallback(this::resyncAll);
        transport.subscribe(this::onMessage);
    }

    /**
     * Returns the shared manager for the given transport signature, creating it
     * (and its subscription) on first use.
     */
    public static SyncManager shared(@NotNull String transportSignature, @NotNull Supplier<SyncTransport> transportFactory) {
        return SHARED.computeIfAbsent(transportSignature, sig -> new SyncManager(SERVER_ID, transportFactory.get()));
    }

    /**
     * Unregisters a container from the shared manager identified by
     * {@code transportSignature}; once the last container leaves, the manager's
     * transport is closed and the manager discarded.
     */
    public static void release(@NotNull String transportSignature, @NotNull String containerId) {
        synchronized (SHARED) {
            SyncManager mgr = SHARED.get(transportSignature);
            if (mgr == null) {
                return;
            }
            mgr.unregister(containerId);
            if (mgr.receivers.isEmpty()) {
                SHARED.remove(transportSignature);
                mgr.close();
            }
        }
    }

    public String getServerId() {
        return serverId;
    }

    public void register(@NotNull String containerId, @NotNull SyncReceiver receiver) {
        receivers.put(containerId, receiver);
    }

    public void unregister(@NotNull String containerId) {
        receivers.remove(containerId);
    }

    /** Broadcasts a change for the given container/key. */
    public void publish(@NotNull String containerId, @NotNull String key, @NotNull SyncOp op) {
        transport.publish(new SyncMessage(serverId, containerId, op, key).encode());
    }

    /** Handles an incoming raw message from the transport (subscriber thread). */
    void onMessage(String raw) {
        SyncMessage msg = SyncMessage.decode(raw);
        if (msg == null) {
            return;
        }
        if (serverId.equals(msg.getServerId())) {
            return; // our own echo
        }
        SyncReceiver receiver = receivers.get(msg.getContainerId());
        if (receiver != null) {
            try {
                receiver.onRemoteChange(msg.getKey(), msg.getOp());
            } catch (Exception ignored) {
                // a faulty receiver must not kill the subscriber thread
            }
        }
    }

    /** Tells every registered container to reload after a transport reconnect. */
    void resyncAll() {
        for (SyncReceiver receiver : receivers.values()) {
            try {
                receiver.onResync();
            } catch (Exception ignored) {
                // keep going for the other receivers
            }
        }
    }

    public boolean isEmpty() {
        return receivers.isEmpty();
    }

    public void close() {
        transport.close();
    }
}
