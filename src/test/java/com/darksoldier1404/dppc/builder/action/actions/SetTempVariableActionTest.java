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

/**
 * {@link SetTempVariableAction#execute} resolves the value through
 * {@code applyVariables}, which needs a live player.
 */
class SetTempVariableActionTest extends MockServerTest {

    private ActionContext contextWithPlayer() {
        WorldMock world = server.addSimpleWorld("world");
        PlayerMock player = server.addPlayer("Steve");
        player.teleport(new Location(world, 0, 0, 0));
        return new ActionContext(player);
    }

    @Test
    void executeStoresLiteralValue() {
        ActionContext ctx = contextWithPlayer();
        new SetTempVariableAction("greeting", "hello").execute(ctx);
        assertEquals("hello", ctx.getVariable("greeting"));
    }

    @Test
    void executeResolvesPlayerPlaceholder() {
        ActionContext ctx = contextWithPlayer();
        new SetTempVariableAction("who", "{player}").execute(ctx);
        assertEquals("Steve", ctx.getVariable("who"));
    }

    @Test
    void getActionTypeAndSerialize() {
        SetTempVariableAction a = new SetTempVariableAction("k", "v");
        assertEquals(ActionType.SET_TEMP_VARIABLE, a.getActionType());
        assertEquals("set_temp_variable k v", a.serialize());
    }

    @Test
    void parseSplitsNameAndValueWithSpaces() {
        SetTempVariableAction a = SetTempVariableAction.parse("set_temp_variable msg hello world");
        assertEquals("set_temp_variable msg hello world", a.serialize());
        assertNull(SetTempVariableAction.parse("set_temp_variable onlyname"));
    }

    @Test
    void doesNotParseLegacyKeyword() {
        // Hard cutover: the old "set_variable" keyword is no longer recognized.
        assertNull(SetTempVariableAction.parse("set_variable k v"));
    }
}
