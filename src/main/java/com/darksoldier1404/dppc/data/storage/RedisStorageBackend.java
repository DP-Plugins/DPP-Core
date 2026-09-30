package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.data.DPlugin;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * Redis backend. Each container maps to a single Redis Hash ({@code {plugin}:{path}})
 * whose fields are the container keys and whose values are the serialized YAML.
 * All access goes through a shared {@link JedisPool}.
 */
@DPPCoreVersion(since = "5.4.4")
public class RedisStorageBackend implements StorageBackend {

    private static final int MAX_CAS_RETRIES = 16;

    private final DPlugin plugin;
    private final StorageSettings settings;
    private final String hashKey;
    private final String signature;
    private final int database;
    private final JedisPool pool;

    public RedisStorageBackend(@NotNull DPlugin plugin, @NotNull StorageSettings settings, @NotNull String path) {
        this.plugin = plugin;
        this.settings = settings;
        this.hashKey = plugin.getName() + ":" + path;
        int db = 0;
        try {
            db = Integer.parseInt(settings.getDatabase());
        } catch (NumberFormatException ignored) {
        }
        this.database = db;
        this.signature = "redis:" + settings.getHost() + ":" + settings.getPort() + ":" + database;
        this.pool = ConnectionManager.acquireRedis(signature, this::buildPool);
    }

    private JedisPool buildPool() {
        GenericObjectPoolConfig<Jedis> cfg = new GenericObjectPoolConfig<>();
        cfg.setMaxTotal(10);
        String pw = settings.getPassword();
        if (pw != null && pw.isEmpty()) {
            pw = null;
        }
        return new JedisPool(cfg, settings.getHost(), settings.getPort(), 2000, pw, database);
    }

    @Override
    public void save(@NotNull String key, @NotNull String yaml) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(hashKey, key, yaml);
        } catch (Exception e) {
            warn("save", key, e);
        }
    }

    @Override
    @Nullable
    public String load(@NotNull String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hget(hashKey, key);
        } catch (Exception e) {
            warn("load", key, e);
            return null;
        }
    }

    @Override
    @NotNull
    public Map<String, String> loadAll() {
        try (Jedis jedis = pool.getResource()) {
            Map<String, String> all = jedis.hgetAll(hashKey);
            return all == null ? new HashMap<>() : all;
        } catch (Exception e) {
            warn("loadAll", null, e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean delete(@NotNull String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hdel(hashKey, key) > 0;
        } catch (Exception e) {
            warn("delete", key, e);
            return false;
        }
    }

    /**
     * Optimistic CAS via {@code WATCH}/{@code MULTI}/{@code EXEC}. Note that
     * {@code WATCH} guards the whole container Hash, so concurrent writes to
     * <i>other</i> keys in the same container can trigger a (harmless) retry. For
     * high-contention exchanges prefer MySQL as the authoritative store (row-level
     * CAS) with Redis used only for sync.
     */
    @Override
    @Nullable
    public String compute(@NotNull String key, @NotNull UnaryOperator<String> remapper) {
        for (int attempt = 0; attempt < MAX_CAS_RETRIES; attempt++) {
            try (Jedis jedis = pool.getResource()) {
                jedis.watch(hashKey);
                String current = jedis.hget(hashKey, key);
                String next = remapper.apply(current);
                if (next == null) {
                    jedis.unwatch();
                    return current; // remapper aborted
                }
                Transaction tx = jedis.multi();
                tx.hset(hashKey, key, next);
                List<Object> result = tx.exec();
                if (result != null) {
                    return next; // EXEC succeeded: no concurrent change
                }
                // watched hash changed: retry
            } catch (Exception e) {
                warn("compute", key, e);
                return null;
            }
        }
        warn("compute", key, new IllegalStateException("CAS retries exhausted (" + MAX_CAS_RETRIES + ")"));
        return null;
    }

    @Override
    public void close() {
        ConnectionManager.releaseRedis(signature);
    }

    private void warn(String op, String key, Exception e) {
        plugin.getLog().warning("[" + hashKey + "] Redis " + op + (key != null ? " (key=" + key + ")" : "")
                + " failed: " + e.getMessage(), DLogManager.printStorageLogs);
    }
}
