package com.darksoldier1404.dppc.builder.action.obj;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the player-independent logic of {@link ActionContext}: the condition
 * stack and variable store. {@code applyVariables} requires a live Player and
 * is covered by the MockBukkit-based tests.
 */
class ActionContextTest {

    private ActionContext ctx() {
        return new ActionContext(null);
    }

    @Test
    void freshContextShouldExecute() {
        assertTrue(ctx().shouldExecute());
    }

    @Test
    void cancelStopsExecution() {
        ActionContext c = ctx();
        c.cancel();
        assertTrue(c.isCancelled());
        assertFalse(c.shouldExecute());
    }

    @Test
    void falseConditionBlocksExecution() {
        ActionContext c = ctx();
        c.pushCondition(false);
        assertFalse(c.shouldExecute());
        assertEquals(1, c.getConditionDepth());
    }

    @Test
    void trueConditionAllowsExecution() {
        ActionContext c = ctx();
        c.pushCondition(true);
        assertTrue(c.shouldExecute());
    }

    @Test
    void nestedConditionsRequireAllTrue() {
        ActionContext c = ctx();
        c.pushCondition(true);
        c.pushCondition(false);
        assertFalse(c.shouldExecute());
        c.popCondition();
        assertTrue(c.shouldExecute());
    }

    @Test
    void flipTopConditionInvertsTop() {
        ActionContext c = ctx();
        c.pushCondition(false);
        assertFalse(c.shouldExecute());
        c.flipTopCondition();
        assertTrue(c.shouldExecute());
        assertEquals(1, c.getConditionDepth());
    }

    @Test
    void popOnEmptyStackIsSafe() {
        ActionContext c = ctx();
        c.popCondition();
        assertEquals(0, c.getConditionDepth());
    }

    @Test
    void variableStore() {
        ActionContext c = ctx();
        assertFalse(c.hasVariable("x"));
        assertEquals("", c.getVariable("x"));
        c.setVariable("x", "5");
        assertTrue(c.hasVariable("x"));
        assertEquals("5", c.getVariable("x"));
    }
}
