package com.darksoldier1404.dppc.data.storage;

import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.JedisPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Shares and reference-counts the heavyweight connection resources
 * ({@link HikariDataSource} for JDBC, {@link JedisPool} for Redis) so that
 * multiple containers pointing at the same database reuse a single pool instead
 * of each opening their own.
 * <p>
 * Each backend acquires a pool with a stable signature string and releases it on
 * {@link StorageBackend#close()}; the underlying pool is closed only once the last
 * user releases it.
 */
@DPPCoreVersion(since = "5.4.4")
public final class ConnectionManager {

    private ConnectionManager() {
    }

    private static final Map<String, RefCounted<HikariDataSource>> JDBC_POOLS = new ConcurrentHashMap<>();
    private static final Map<String, RefCounted<JedisPool>> REDIS_POOLS = new ConcurrentHashMap<>();

    private static final class RefCounted<T> {
        final T resource;
        int count;

        RefCounted(T resource) {
            this.resource = resource;
            this.count = 1;
        }
    }

    @NotNull
    public static HikariDataSource acquireJdbc(@NotNull String signature, @NotNull Supplier<HikariConfig> configSupplier) {
        synchronized (JDBC_POOLS) {
            RefCounted<HikariDataSource> rc = JDBC_POOLS.get(signature);
            if (rc != null) {
                rc.count++;
                return rc.resource;
            }
            HikariDataSource ds = new HikariDataSource(configSupplier.get());
            JDBC_POOLS.put(signature, new RefCounted<>(ds));
            return ds;
        }
    }

    public static void releaseJdbc(@NotNull String signature) {
        synchronized (JDBC_POOLS) {
            RefCounted<HikariDataSource> rc = JDBC_POOLS.get(signature);
            if (rc == null) {
                return;
            }
            if (--rc.count <= 0) {
                JDBC_POOLS.remove(signature);
                try {
                    rc.resource.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @NotNull
    public static JedisPool acquireRedis(@NotNull String signature, @NotNull Supplier<JedisPool> poolSupplier) {
        synchronized (REDIS_POOLS) {
            RefCounted<JedisPool> rc = REDIS_POOLS.get(signature);
            if (rc != null) {
                rc.count++;
                return rc.resource;
            }
            JedisPool pool = poolSupplier.get();
            REDIS_POOLS.put(signature, new RefCounted<>(pool));
            return pool;
        }
    }

    public static void releaseRedis(@NotNull String signature) {
        synchronized (REDIS_POOLS) {
            RefCounted<JedisPool> rc = REDIS_POOLS.get(signature);
            if (rc == null) {
                return;
            }
            if (--rc.count <= 0) {
                REDIS_POOLS.remove(signature);
                try {
                    rc.resource.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
