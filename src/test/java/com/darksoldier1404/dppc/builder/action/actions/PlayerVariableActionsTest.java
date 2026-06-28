package com.darksoldier1404.dppc.builder.action.actions;

import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.VariableStore;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerVariableActionsTest extends MockServerTest {

    private ActionContext context() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer("Steve");
        player.teleport(new Location(world, 0, 64, 0));
        return new ActionContext(player, new VariableStore());
    }

    @Test
    void setStoresInPlayerScope() {
        ActionContext ctx = context();
        new SetPlayerVariableAction("coins", "10").execute(ctx);
        assertEquals("10", ctx.getPlayerVariable("coins"));
        // Must not leak into the temporary scope.
        assertFalse(ctx.hasVariable("coins"));
    }

    @Test
    void addAccumulatesInPlayerScope() {
        ActionContext ctx = context();
        ctx.setPlayerVariable("coins", "5");
        new AddPlayerVariableAction("coins", 3).execute(ctx);
        assertEquals("8", ctx.getPlayerVariable("coins"));
    }

    @Test
    void randomStaysWithinRange() {
        ActionContext ctx = context();
        new RandomPlayerNumberAction("roll", 1, 6).execute(ctx);
        int value = Integer.parseInt(ctx.getPlayerVariable("roll"));
        assertTrue(value >= 1 && value <= 6, "value=" + value);
    }

    @Test
    void conditionComparesPlayerScope() {
        ActionContext ctx = context();
        ctx.setPlayerVariable("coins", "10");
        new IfPlayerVariableGreaterAction("coins", 5).execute(ctx);
        assertTrue(ctx.shouldExecute());
    }
}
