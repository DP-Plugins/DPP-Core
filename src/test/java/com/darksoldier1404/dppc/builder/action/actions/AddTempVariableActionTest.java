package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link AddTempVariableAction#execute} only touches the context variable store,
 * so no player is required.
 */
class AddTempVariableActionTest {

    @Test
    void addsToExistingNumericVariableAsInteger() {
        ActionContext ctx = new ActionContext(null);
        ctx.setVariable("n", "5");
        new AddTempVariableAction("n", 3).execute(ctx);
        // 8.0 collapses to integer string "8".
        assertEquals("8", ctx.getVariable("n"));
    }

    @Test
    void keepsFractionalResult() {
        ActionContext ctx = new ActionContext(null);
        ctx.setVariable("n", "1");
        new AddTempVariableAction("n", 0.5).execute(ctx);
        assertEquals("1.5", ctx.getVariable("n"));
    }

    @Test
    void nonNumericVariableResetsToAmount() {
        ActionContext ctx = new ActionContext(null);
        // default variable value is "" -> NumberFormatException -> set to amount
        new AddTempVariableAction("fresh", 4).execute(ctx);
        assertEquals("4.0", ctx.getVariable("fresh"));
    }

    @Test
    void getActionTypeAndSerialize() {
        AddTempVariableAction a = new AddTempVariableAction("score", 2);
        assertEquals(ActionType.ADD_TEMP_VARIABLE, a.getActionType());
        assertEquals("add_temp_variable score 2.0", a.serialize());
    }

    @Test
    void parseValidAndInvalid() {
        assertEquals(ActionType.ADD_TEMP_VARIABLE, AddTempVariableAction.parse("add_temp_variable x 5").getActionType());
        assertNull(AddTempVariableAction.parse("add_temp_variable x notanumber"));
        assertNull(AddTempVariableAction.parse("add_temp_variable x"));
        assertNull(AddTempVariableAction.parse("add_variable x 5"));
    }
}
