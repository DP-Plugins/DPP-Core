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
 * Covers the player/global variable scopes on {@link ActionContext} and their
 * {@code {pvar_*}} / {@code {gvar_*}} placeholder expansion. A memory-only
 * {@link VariableStore} is injected so no plugin is required.
 */
class ActionContextScopeTest extends MockServerTest {

    private ActionContext context() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer("Steve");
        player.teleport(new Location(world, 0, 64, 0));
        return new ActionContext(player, new VariableStore());
    }

    @Test
    void playerVariableSetAndGet() {
        ActionContext ctx = context();
        assertFalse(ctx.hasPlayerVariable("coins"));
        ctx.setPlayerVariable("coins", "50");
        assertTrue(ctx.hasPlayerVariable("coins"));
        assertEquals("50", ctx.getPlayerVariable("coins"));
    }

    @Test
    void globalVariableSetAndGet() {
        ActionContext ctx = context();
        ctx.setGlobalVariable("jackpot", "9000");
        assertEquals("9000", ctx.getGlobalVariable("jackpot"));
    }

    @Test
    void scopesAreIsolated() {
        ActionContext ctx = context();
        ctx.setVariable("x", "temp");
        ctx.setPlayerVariable("x", "player");
        ctx.setGlobalVariable("x", "global");
        assertEquals("temp", ctx.getVariable("x"));
        assertEquals("player", ctx.getPlayerVariable("x"));
        assertEquals("global", ctx.getGlobalVariable("x"));
    }

    @Test
    void applyVariablesExpandsAllScopes() {
        ActionContext ctx = context();
        ctx.setVariable("t", "TT");
        ctx.setPlayerVariable("p", "PP");
        ctx.setGlobalVariable("g", "GG");
        assertEquals("TT/PP/GG", ctx.applyVariables("{t}/{pvar_p}/{gvar_g}"));
    }

    @Test
    void savedPrefixDoesNotCollideWithBuiltinPlayerPlaceholders() {
        ActionContext ctx = context();
        // A saved variable literally named "world" must not disturb {player_world}.
        ctx.setPlayerVariable("world", "SAVED");
        String result = ctx.applyVariables("{player_world}|{pvar_world}");
        assertEquals("world|SAVED", result);
    }
}
