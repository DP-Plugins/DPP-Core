package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IfHasItemActionTest extends MockServerTest {

    private ActionContext contextWithPlayer() {
        return new ActionContext(spawnPlayerInWorld("Steve"));
    }

    @Test
    void pushesTrueWhenPlayerHasEnoughMatchingItem() {
        ActionContext ctx = contextWithPlayer();
        ctx.getPlayer().getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD, 1));
        new IfHasItemAction(new ItemStack(Material.DIAMOND_SWORD, 1)).execute(ctx);
        assertTrue(ctx.shouldExecute());
    }

    @Test
    void pushesFalseWhenAmountIsInsufficient() {
        ActionContext ctx = contextWithPlayer();
        ctx.getPlayer().getInventory().addItem(new ItemStack(Material.ARROW, 2));
        new IfHasItemAction(new ItemStack(Material.ARROW, 5)).execute(ctx);
        assertFalse(ctx.shouldExecute());
    }

    @Test
    void pushesFalseWhenItemTypeDiffers() {
        ActionContext ctx = contextWithPlayer();
        ctx.getPlayer().getInventory().addItem(new ItemStack(Material.STONE, 5));
        new IfHasItemAction(new ItemStack(Material.DIRT, 1)).execute(ctx);
        assertFalse(ctx.shouldExecute());
    }

    @Test
    void isFlowControl() {
        assertTrue(new IfHasItemAction(new ItemStack(Material.STONE)).isFlowControl());
    }

    @Test
    void getActionTypeAndDisplayText() {
        IfHasItemAction a = new IfHasItemAction(new ItemStack(Material.GOLD_INGOT, 4));
        assertEquals(ActionType.IF_HAS_ITEM, a.getActionType());
        assertEquals("if_has_item GOLD_INGOT x4", a.getDisplayText());
    }

    @Test
    void serializeStartsWithCommandName() {
        IfHasItemAction a = new IfHasItemAction(new ItemStack(Material.STONE));
        assertTrue(a.serialize().startsWith("if_has_item "));
    }

    @Test
    void parseRejectsBadInput() {
        assertNull(IfHasItemAction.parse("wrong_command aGVsbG8="));
        assertNull(IfHasItemAction.parse("if_has_item"));
    }

    // Round-trip (serialize -> parse) cannot be verified under MockBukkit: MockBukkit's
    // ItemMetaMock serializes to invalid YAML, so ItemStackSerializer.deserialize() always
    // returns null here even though it works correctly on a real Bukkit/Spigot server.
}
