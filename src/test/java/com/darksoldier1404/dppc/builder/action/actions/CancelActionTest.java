package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CancelActionTest {

    @Test
    void executeCancelsContext() {
        ActionContext ctx = new ActionContext(null);
        new CancelAction().execute(ctx);
        assertTrue(ctx.isCancelled());
    }

    @Test
    void getActionTypeAndSerialize() {
        CancelAction a = new CancelAction();
        assertEquals(ActionType.CANCEL, a.getActionType());
        assertEquals("cancel", a.serialize());
    }

    @Test
    void parseAcceptsOnlyCancel() {
        assertNotNull(CancelAction.parse("cancel"));
        assertNotNull(CancelAction.parse("  cancel  "));
        assertNull(CancelAction.parse("cancel now"));
        assertNull(CancelAction.parse("nope"));
    }
}
