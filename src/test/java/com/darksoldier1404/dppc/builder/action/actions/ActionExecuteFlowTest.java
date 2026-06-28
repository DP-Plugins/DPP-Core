package com.darksoldier1404.dppc.builder.action.actions;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Execute-behaviour tests for flow-control / condition actions, asserting on the
 * resulting condition state via {@link ActionContext#shouldExecute()}.
 */
class ActionExecuteFlowTest extends MockServerTest {

    private ActionContext ctx() {
        return new ActionContext(spawnPlayerInWorld("Steve"));
    }

    @Test
    void ifVariableGreater() {
        ActionContext c = ctx();
        c.setVariable("a", "10");
        new IfTempVariableGreaterAction("a", 5).execute(c);
        assertTrue(c.shouldExecute());

        ActionContext c2 = ctx();
        c2.setVariable("a", "3");
        new IfTempVariableGreaterAction("a", 5).execute(c2);
        assertFalse(c2.shouldExecute());
    }

    @Test
    void ifVariableLess() {
        ActionContext c = ctx();
        c.setVariable("a", "3");
        new IfTempVariableLessAction("a", 5).execute(c);
        assertTrue(c.shouldExecute());
    }

    @Test
    void ifVariableGreaterWithNonNumericIsFalse() {
        ActionContext c = ctx();
        c.setVariable("a", "abc");
        new IfTempVariableGreaterAction("a", 5).execute(c);
        assertFalse(c.shouldExecute());
    }

    @Test
    void ifVariableNotEquals() {
        ActionContext c = ctx();
        c.setVariable("a", "x");
        new IfTempVariableNotEqualsAction("a", "y").execute(c);
        assertTrue(c.shouldExecute());

        ActionContext c2 = ctx();
        c2.setVariable("a", "same");
        new IfTempVariableNotEqualsAction("a", "same").execute(c2);
        assertFalse(c2.shouldExecute());
    }

    @Test
    void elseFlipsCondition() {
        ActionContext c = ctx();
        c.pushCondition(false);
        assertFalse(c.shouldExecute());
        new ElseAction().execute(c);
        assertTrue(c.shouldExecute());
    }

    @Test
    void endIfPopsCondition() {
        ActionContext c = ctx();
        c.pushCondition(false);
        assertFalse(c.shouldExecute());
        new EndIfAction().execute(c);
        assertTrue(c.shouldExecute());
        assertTrue(c.getConditionDepth() == 0);
    }

    @Test
    void ifHasPermissionFalseForPlainPlayer() {
        ActionContext c = ctx();
        new IfHasPermissionAction("dppc.test").execute(c);
        assertFalse(c.shouldExecute());
    }

    @Test
    void ifHasPermissionTrueWhenGranted() {
        PlayerMock player = spawnPlayerInWorld("Steve");
        player.addAttachment(MockBukkit.createMockPlugin(), "dppc.test", true);
        ActionContext c = new ActionContext(player);
        new IfHasPermissionAction("dppc.test").execute(c);
        assertTrue(c.shouldExecute());
    }

    @Test
    void ifNotPermissionTrueForPlainPlayer() {
        ActionContext c = ctx();
        new IfNotPermissionAction("dppc.test").execute(c);
        assertTrue(c.shouldExecute());
    }
}
