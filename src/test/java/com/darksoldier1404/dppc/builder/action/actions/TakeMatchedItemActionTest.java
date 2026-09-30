package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.support.MockServerTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TakeMatchedItemActionTest extends MockServerTest {

    private ActionContext contextWithPlayer() {
        return new ActionContext(spawnPlayerInWorld("Steve"));
    }

    @Test
    void executeRemovesMatchingAmount() {
        ActionContext ctx = contextWithPlayer();
        ctx.getPlayer().getInventory().addItem(new ItemStack(Material.DIAMOND_SWORD, 1));
        new TakeMatchedItemAction(new ItemStack(Material.DIAMOND_SWORD, 1)).execute(ctx);
        assertEquals(0, ctx.getPlayer().getInventory().all(Material.DIAMOND_SWORD).values()
                .stream().mapToInt(ItemStack::getAmount).sum());
    }

    @Test
    void executeLeavesUnmatchedItemsUntouched() {
        ActionContext ctx = contextWithPlayer();
        ctx.getPlayer().getInventory().addItem(new ItemStack(Material.STONE, 5));
        new TakeMatchedItemAction(new ItemStack(Material.DIRT, 1)).execute(ctx);
        assertEquals(5, ctx.getPlayer().getInventory().all(Material.STONE).values()
                .stream().mapToInt(ItemStack::getAmount).sum());
    }

    @Test
    void executeRemovesOnlyAvailableAmountWhenShortOnStock() {
        ActionContext ctx = contextWithPlayer();
        ctx.getPlayer().getInventory().addItem(new ItemStack(Material.ARROW, 2));
        new TakeMatchedItemAction(new ItemStack(Material.ARROW, 5)).execute(ctx);
        assertEquals(0, ctx.getPlayer().getInventory().all(Material.ARROW).values()
                .stream().mapToInt(ItemStack::getAmount).sum());
    }

    @Test
    void getActionTypeAndDisplayText() {
        TakeMatchedItemAction a = new TakeMatchedItemAction(new ItemStack(Material.GOLD_INGOT, 4));
        assertEquals(ActionType.TAKE_MATCHED_ITEM, a.getActionType());
        assertEquals("take_matched_item GOLD_INGOT x4", a.getDisplayText());
    }

    @Test
    void serializeStartsWithCommandName() {
        TakeMatchedItemAction a = new TakeMatchedItemAction(new ItemStack(Material.STONE));
        assertTrue(a.serialize().startsWith("take_matched_item "));
    }

    @Test
    void parseRejectsBadInput() {
        assertNull(TakeMatchedItemAction.parse("wrong_command aGVsbG8="));
        assertNull(TakeMatchedItemAction.parse("take_matched_item"));
    }

    // Round-trip (serialize -> parse) cannot be verified under MockBukkit: MockBukkit's
    // ItemMetaMock serializes to invalid YAML, so ItemStackSerializer.deserialize() always
    // returns null here even though it works correctly on a real Bukkit/Spigot server.
}
