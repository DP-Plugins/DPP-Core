package com.darksoldier1404.dppc.data.sync;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * In-process {@link SyncTransport} for tests. Messages published on a named bus are
 * delivered synchronously to every subscriber on that bus (including the sender,
 * mirroring Redis Pub/Sub), so {@link SyncManager}'s self-filtering and routing can
 * be exercised with no Redis instance.
 */
public class InMemorySyncTransport implements SyncTransport {

    private static final Map<String, List<Consumer<String>>> BUSES = new ConcurrentHashMap<>();

    private final String bus;
    private Consumer<String> handler;
    private Runnable reconnectCallback = () -> {
    };

    public InMemorySyncTransport(String bus) {
        this.bus = bus;
    }

    @Override
    public void publish(@NotNull String message) {
        for (Consumer<String> h : BUSES.getOrDefault(bus, List.of())) {
            h.accept(message);
        }
    }

    @Override
    public void subscribe(@NotNull Consumer<String> onMessage) {
        this.handler = onMessage;
        BUSES.computeIfAbsent(bus, b -> new CopyOnWriteArrayList<>()).add(onMessage);
    }

    @Override
    public void setReconnectCallback(@NotNull Runnable callback) {
        this.reconnectCallback = callback;
    }

    /** Test hook: simulate a reconnect, triggering the manager's resync. */
    public void fireReconnect() {
        reconnectCallback.run();
    }

    @Override
    public void close() {
        List<Consumer<String>> list = BUSES.get(bus);
        if (list != null) {
            list.remove(handler);
        }
    }
}
