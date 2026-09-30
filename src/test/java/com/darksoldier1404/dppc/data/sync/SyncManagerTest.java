package com.darksoldier1404.dppc.data.sync;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives two {@link SyncManager}s (two "servers") over a shared in-memory bus to
 * verify cross-delivery, self-echo filtering, and resync-on-reconnect — without Redis.
 */
class SyncManagerTest {

    private String freshBus() {
        return "bus-" + UUID.randomUUID();
    }

    @Test
    void deliversToOtherServerAndFiltersOwnEcho() {
        String bus = freshBus();
        SyncManager a = new SyncManager("server-A", new InMemorySyncTransport(bus));
        SyncManager b = new SyncManager("server-B", new InMemorySyncTransport(bus));

        List<String> aReceived = new ArrayList<>();
        List<String> bReceived = new ArrayList<>();
        a.register("c1", (key, op) -> aReceived.add(key + ":" + op));
        b.register("c1", (key, op) -> bReceived.add(key + ":" + op));

        a.publish("c1", "k1", SyncOp.UPSERT);

        assertEquals(List.of("k1:UPSERT"), bReceived, "other server should receive the change");
        assertTrue(aReceived.isEmpty(), "publisher must not receive its own echo");

        a.close();
        b.close();
    }

    @Test
    void onlyMatchingContainerReceives() {
        String bus = freshBus();
        SyncManager a = new SyncManager("A", new InMemorySyncTransport(bus));
        SyncManager b = new SyncManager("B", new InMemorySyncTransport(bus));

        List<String> got = new ArrayList<>();
        b.register("containerX", (key, op) -> got.add(key));

        a.publish("containerY", "k", SyncOp.UPSERT); // different container id
        assertTrue(got.isEmpty());

        a.publish("containerX", "k", SyncOp.UPSERT);
        assertEquals(List.of("k"), got);

        a.close();
        b.close();
    }

    @Test
    void reconnectTriggersResync() {
        String bus = freshBus();
        InMemorySyncTransport transport = new InMemorySyncTransport(bus);
        SyncManager mgr = new SyncManager("S", transport);

        int[] resyncCount = {0};
        mgr.register("c", new SyncReceiver() {
            @Override
            public void onRemoteChange(String key, SyncOp op) {
            }

            @Override
            public void onResync() {
                resyncCount[0]++;
            }
        });

        transport.fireReconnect();
        assertEquals(1, resyncCount[0]);

        mgr.close();
    }
}
