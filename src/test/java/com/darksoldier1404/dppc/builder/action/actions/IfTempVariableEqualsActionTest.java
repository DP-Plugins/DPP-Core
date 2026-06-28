package com.darksoldier1404.dppc.builder.action.actions;

import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IfTempVariableEqualsActionTest extends MockServerTest {

    private ActionContext contextWithPlayer() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer("Steve");
        player.teleport(new Location(world, 0, 0, 0));
        return new ActionContext(player);
    }

    @Test
    void pushesTrueWhenVariableMatches() {
        ActionContext ctx = contextWithPlayer();
        ctx.setVariable("a", "5");
        new IfTempVariableEqualsAction("a", "5").execute(ctx);
        assertTrue(ctx.shouldExecute());
    }

    @Test
    void pushesFalseWhenVariableDiffers() {
        ActionContext ctx = contextWithPlayer();
        ctx.setVariable("a", "5");
        new IfTempVariableEqualsAction("a", "6").execute(ctx);
        assertFalse(ctx.shouldExecute());
    }

    @Test
    void isFlowControl() {
        assertTrue(new IfTempVariableEqualsAction("a", "b").isFlowControl());
    }

    @Test
    void getActionTypeAndSerializeAndParse() {
        IfTempVariableEqualsAction a = new IfTempVariableEqualsAction("a", "b");
        org.junit.jupiter.api.Assertions.assertEquals(ActionType.IF_TEMP_VARIABLE_EQUALS, a.getActionType());
        org.junit.jupiter.api.Assertions.assertEquals("if_temp_variable_equals a b", a.serialize());
        assertNull(IfTempVariableEqualsAction.parse("if_temp_variable_equals onlyname"));
        assertNull(IfTempVariableEqualsAction.parse("if_variable_equals a b"));
    }
}
