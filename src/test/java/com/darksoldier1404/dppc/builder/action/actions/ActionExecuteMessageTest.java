package com.darksoldier1404.dppc.builder.action.actions;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Execute-behaviour tests for messaging / command actions. Where MockBukkit
 * exposes the delivered message it is asserted; otherwise the test verifies the
 * code path runs without error.
 */
class ActionExecuteMessageTest extends MockServerTest {

    private ActionContext ctx(PlayerMock p) {
        return new ActionContext(p);
    }

    @Test
    void sendMessageDeliversToPlayer() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new SendMessageAction("hello").execute(ctx(p));
        p.assertSaid("hello");
    }

    @Test
    void sendMessageResolvesPlaceholders() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new SendMessageAction("hi {player}").execute(ctx(p));
        p.assertSaid("hi Steve");
    }

    @Test
    void broadcastReachesOnlinePlayer() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new BroadcastAction("announcement").execute(ctx(p));
        p.assertSaid("announcement");
    }

    @Test
    void broadcastWorldReachesPlayersInWorld() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new BroadcastWorldAction("world-msg").execute(ctx(p));
        p.assertSaid("world-msg");
    }

    @Test
    void kickMarksPlayerOffline() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        new KickAction("bye").execute(ctx(p));
        assertFalse(p.isOnline());
    }

    @Test
    void executeAsPlayerRunsWithoutError() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        assertDoesNotThrow(() -> new ExecuteCommandAsPlayerAction("say hi").execute(ctx(p)));
    }

    @Test
    void executeAsAdminRestoresOpState() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        assertDoesNotThrow(() -> new ExecuteCommandAsAdminAction("say hi").execute(ctx(p)));
        // Player was not op before, so op must be restored to false afterwards.
        assertFalse(p.isOp());
    }

    @Test
    void sendTitleRunsWithoutError() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        assertDoesNotThrow(() -> new SendTitleAction("T", "S", 10, 70, 20).execute(ctx(p)));
    }

    @Test
    void sendActionBarRunsWithoutError() {
        PlayerMock p = spawnPlayerInWorld("Steve");
        assertDoesNotThrow(() -> new SendActionBarAction("bar").execute(ctx(p)));
    }
}
