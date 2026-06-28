package com.darksoldier1404.dppc.builder.action.obj;

import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link ActionContext#applyVariables(String)}, which needs a live player.
 */
class ActionContextPlayerTest extends MockServerTest {

    private PlayerMock playerInWorld() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer("Steve");
        player.teleport(new Location(world, 1.0, 2.0, 3.0));
        return player;
    }

    @Test
    void nullTextReturnsNull() {
        ActionContext ctx = new ActionContext(playerInWorld());
        org.junit.jupiter.api.Assertions.assertNull(ctx.applyVariables(null));
    }

    @Test
    void replacesPlayerName() {
        ActionContext ctx = new ActionContext(playerInWorld());
        String result = ctx.applyVariables("Hi {player}!");
        assertEquals("Hi Steve!", result);
    }

    @Test
    void replacesAllBuiltInPlaceholders() {
        ActionContext ctx = new ActionContext(playerInWorld());
        String result = ctx.applyVariables(
                "{player}/{player_world}/{player_x}/{player_y}/{player_z}/{player_health}/{player_level}/{player_food}");
        // No placeholder tokens should remain.
        assertFalse(result.contains("{"), result);
        assertTrue(result.startsWith("Steve/world/"), result);
    }

    @Test
    void replacesCustomVariables() {
        ActionContext ctx = new ActionContext(playerInWorld());
        ctx.setVariable("score", "42");
        assertEquals("score=42", ctx.applyVariables("score={score}"));
    }
}
