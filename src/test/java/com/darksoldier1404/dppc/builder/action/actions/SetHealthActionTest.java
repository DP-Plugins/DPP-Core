package com.darksoldier1404.dppc.builder.action.actions;

import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SetHealthActionTest extends MockServerTest {

    private ActionContext contextWithPlayer() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer("Steve");
        player.teleport(new Location(world, 0, 0, 0));
        return new ActionContext(player);
    }

    @Test
    void executeSetsHealthWithinBounds() {
        ActionContext ctx = contextWithPlayer();
        new SetHealthAction(10).execute(ctx);
        assertEquals(10.0, ctx.getPlayer().getHealth());
    }

    @Test
    void executeClampsToMaxHealth() {
        ActionContext ctx = contextWithPlayer();
        double max = ctx.getPlayer().getMaxHealth();
        new SetHealthAction(9999).execute(ctx);
        assertEquals(max, ctx.getPlayer().getHealth());
    }

    @Test
    void getActionTypeAndSerialize() {
        SetHealthAction a = new SetHealthAction(15);
        assertEquals(ActionType.SET_HEALTH, a.getActionType());
        assertEquals("set_health 15.0", a.serialize());
    }

    @Test
    void parseValidLine() {
        SetHealthAction a = SetHealthAction.parse("set_health 12.5");
        assertEquals("set_health 12.5", a.serialize());
    }

    @Test
    void parseRejectsBadInput() {
        assertNull(SetHealthAction.parse("set_health notanumber"));
        assertNull(SetHealthAction.parse("wrong 5"));
        assertNull(SetHealthAction.parse("set_health"));
    }
}
