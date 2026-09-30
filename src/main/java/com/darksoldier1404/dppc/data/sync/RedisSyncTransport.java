package com.darksoldier1404.dppc.data.sync;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.data.storage.ConnectionManager;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.function.Consumer;

/**
 * {@link SyncTransport} backed by Redis Pub/Sub on a single shared channel.
 * Publishing borrows a connection from a shared {@link JedisPool}; subscribing
 * runs on a dedicated thread with its own blocking connection, reconnecting with
 * backoff if the link drops.
 */
@DPPCoreVersion(since = "5.4.4")
public class RedisSyncTransport implements SyncTransport {

    public static final String CHANNEL = "dppc:sync";
    private static final long RECONNECT_BACKOFF_MS = 2000L;

    private final DPlugin plugin;
    private final String host;
    private final int port;
    private final int database;
    private final String password;
    private final String poolSignature;
    private final JedisPool publishPool;

    private volatile boolean running = true;
    private volatile JedisPubSub pubSub;
    private volatile Runnable reconnectCallback = () -> {
    };
    private Thread subscriberThread;

    public RedisSyncTransport(@NotNull DPlugin plugin, @NotNull String host, int port, int database, String password) {
        this.plugin = plugin;
        this.host = host;
        this.port = port;
        this.database = database;
        this.password = (password != null && password.isEmpty()) ? null : password;
        this.poolSignature = "redis:" + host + ":" + port + ":" + database;
        this.publishPool = ConnectionManager.acquireRedis(poolSignature, this::buildPool);
    }

    private JedisPool buildPool() {
        GenericObjectPoolConfig<Jedis> cfg = new GenericObjectPoolConfig<>();
        cfg.setMaxTotal(8);
        return new JedisPool(cfg, host, port, 2000, password, database);
    }

    @Override
    public void publish(@NotNull String message) {
        try (Jedis jedis = publishPool.getResource()) {
            jedis.publish(CHANNEL, message);
        } catch (Exception e) {
            plugin.getLog().warning("Sync publish failed: " + e.getMessage(), DLogManager.printStorageLogs);
        }
    }

    @Override
    public void subscribe(@NotNull Consumer<String> onMessage) {
        this.pubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                onMessage.accept(message);
            }
        };
        subscriberThread = new Thread(() -> runSubscriberLoop(onMessage), "DPP-Sync-Subscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    private void runSubscriberLoop(Consumer<String> onMessage) {
        boolean reconnecting = false;
        while (running) {
            if (reconnecting) {
                // Reload from the authoritative store to catch anything missed while down.
                try {
                    reconnectCallback.run();
                } catch (Exception ignored) {
                }
            }
            try (Jedis jedis = new Jedis(host, port, 0)) {
                if (password != null) {
                    jedis.auth(password);
                }
                if (database != 0) {
                    jedis.select(database);
                }
                jedis.subscribe(pubSub, CHANNEL); // blocks until unsubscribed
            } catch (Exception e) {
                reconnecting = true;
                if (!running) {
                    break;
                }
                plugin.getLog().warning("Sync subscriber dropped, reconnecting: " + e.getMessage(), DLogManager.printStorageLogs);
                try {
                    Thread.sleep(RECONNECT_BACKOFF_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    @Override
    public void setReconnectCallback(@NotNull Runnable callback) {
        this.reconnectCallback = callback;
    }

    @Override
    public void close() {
        running = false;
        try {
            if (pubSub != null && pubSub.isSubscribed()) {
                pubSub.unsubscribe();
            }
        } catch (Exception ignored) {
        }
        if (subscriberThread != null) {
            subscriberThread.interrupt();
        }
        ConnectionManager.releaseRedis(poolSignature);
    }
}
