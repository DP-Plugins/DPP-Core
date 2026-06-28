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

class GlobalVariableActionsTest extends MockServerTest {

    private ActionContext context(VariableStore store) {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer("Steve");
        player.teleport(new Location(world, 0, 64, 0));
        return new ActionContext(player, store);
    }

    @Test
    void setStoresInGlobalScope() {
        ActionContext ctx = context(new VariableStore());
        new SetGlobalVariableAction("jackpot", "9000").execute(ctx);
        assertEquals("9000", ctx.getGlobalVariable("jackpot"));
        assertFalse(ctx.hasVariable("jackpot"));
    }

    @Test
    void addAccumulatesInGlobalScope() {
        ActionContext ctx = context(new VariableStore());
        ctx.setGlobalVariable("pot", "100");
        new AddGlobalVariableAction("pot", 50).execute(ctx);
        assertEquals("150", ctx.getGlobalVariable("pot"));
    }

    @Test
    void globalScopeSharedAcrossPlayers() {
        // Two players sharing the same store see the same global value.
        VariableStore store = new VariableStore();
        ActionContext a = context(store);
        new SetGlobalVariableAction("shared", "yes").execute(a);

        PlayerMock other = server.addPlayer("Alex");
        other.teleport(new Location(server.getWorld("world"), 0, 64, 0));
        ActionContext b = new ActionContext(other, store);
        assertEquals("yes", b.getGlobalVariable("shared"));
    }

    @Test
    void conditionComparesGlobalScope() {
        ActionContext ctx = context(new VariableStore());
        ctx.setGlobalVariable("pot", "3");
        new IfGlobalVariableLessAction("pot", 5).execute(ctx);
        assertTrue(ctx.shouldExecute());
    }
}
