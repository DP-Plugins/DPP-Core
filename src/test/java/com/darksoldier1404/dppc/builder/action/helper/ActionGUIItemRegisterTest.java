package com.darksoldier1404.dppc.builder.action.helper;

import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.darksoldier1404.dppc.builder.action.actions.IfHasItemAction;
import com.darksoldier1404.dppc.builder.action.actions.TakeMatchedItemAction;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.support.PluginTest;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Clicks against the item register GUI (channel 2) used by IF_HAS_ITEM and TAKE_MATCHED_ITEM.
 * The player must stay able to handle their own inventory, otherwise no item can ever reach
 * the register slot.
 */
class ActionGUIItemRegisterTest extends PluginTest {

    /** First raw slot belonging to the player's own inventory (top inventory is 27 slots). */
    private static final int PLAYER_SLOT = 27;

    private final ActionGUIHandler handler = new ActionGUIHandler();
    private PlayerMock player;
    private ActionGUI gui;
    private InventoryView view;

    @BeforeEach
    void openRegisterGui() {
        player = server.addPlayer("Steve");
        player.teleport(new Location(server.addSimpleWorld("world"), 0, 64, 0));
        gui = new ActionGUI(plugin, "test_action");
        gui.openItemRegisterGUI(player, ActionType.IF_HAS_ITEM);
        view = player.getOpenInventory();
    }

    private InventoryClickEvent click(int rawSlot, ClickType type, InventoryAction action) {
        InventoryType.SlotType slotType = rawSlot < view.getTopInventory().getSize()
                ? InventoryType.SlotType.CONTAINER
                : InventoryType.SlotType.QUICKBAR;
        InventoryClickEvent e = new InventoryClickEvent(view, slotType, rawSlot, type, action);
        handler.onInventoryClick(e);
        return e;
    }

    @Test
    void playerInventoryClickIsNotCancelled() {
        view.setItem(PLAYER_SLOT, new ItemStack(Material.DIAMOND));
        InventoryClickEvent e = click(PLAYER_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertFalse(e.isCancelled(), "player must be able to pick up their own item");
    }

    @Test
    void registerSlotClickIsNotCancelled() {
        InventoryClickEvent e = click(ActionGUI.ITEM_REGISTER_SLOT, ClickType.LEFT, InventoryAction.PLACE_ALL);
        assertFalse(e.isCancelled(), "the register slot accepts items");
    }

    @Test
    void decorationSlotClickIsCancelled() {
        InventoryClickEvent e = click(0, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertTrue(e.isCancelled(), "filler slots stay locked");
    }

    @Test
    void shiftClickMovesItemIntoRegisterSlot() {
        view.setItem(PLAYER_SLOT, new ItemStack(Material.DIAMOND, 3));
        InventoryClickEvent e = click(PLAYER_SLOT, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);

        assertTrue(e.isCancelled(), "the vanilla move would merge into the filler panes");
        ItemStack registered = view.getTopInventory().getItem(ActionGUI.ITEM_REGISTER_SLOT);
        assertNotNull(registered);
        assertEquals(Material.DIAMOND, registered.getType());
        assertEquals(3, registered.getAmount());
        assertNull(view.getItem(PLAYER_SLOT), "the source stack is consumed");
    }

    @Test
    void shiftClickKeepsAlreadyRegisteredItem() {
        view.getTopInventory().setItem(ActionGUI.ITEM_REGISTER_SLOT, new ItemStack(Material.EMERALD));
        view.setItem(PLAYER_SLOT, new ItemStack(Material.DIAMOND));

        click(PLAYER_SLOT, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);

        assertEquals(Material.EMERALD, view.getTopInventory().getItem(ActionGUI.ITEM_REGISTER_SLOT).getType());
        assertNotNull(view.getItem(PLAYER_SLOT), "nothing moved, so nothing is consumed");
    }

    @Test
    void collectToCursorIsCancelled() {
        view.setItem(PLAYER_SLOT, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        InventoryClickEvent e = click(PLAYER_SLOT, ClickType.DOUBLE_CLICK, InventoryAction.COLLECT_TO_CURSOR);
        assertTrue(e.isCancelled(), "double click would vacuum the filler panes out of the GUI");
    }

    @Test
    void confirmRegistersIfHasItemAction() {
        view.getTopInventory().setItem(ActionGUI.ITEM_REGISTER_SLOT, new ItemStack(Material.DIAMOND, 2));
        InventoryClickEvent e = click(ActionGUI.ITEM_REGISTER_CONFIRM_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        assertTrue(e.isCancelled());
        assertEquals(1, gui.getActionBuilder().getActions().size());
        IfHasItemAction action = (IfHasItemAction) gui.getActionBuilder().getActions().get(0);
        assertEquals(Material.DIAMOND, action.getItem().getType());
        assertEquals(2, action.getItem().getAmount());
    }

    @Test
    void takeMatchedItemUsesTheSameUnlockedGui() {
        gui.openItemRegisterGUI(player, ActionType.TAKE_MATCHED_ITEM);
        view = player.getOpenInventory();

        view.setItem(PLAYER_SLOT, new ItemStack(Material.GOLD_INGOT, 5));
        InventoryClickEvent pickup = click(PLAYER_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        assertFalse(pickup.isCancelled());

        click(PLAYER_SLOT, ClickType.SHIFT_LEFT, InventoryAction.MOVE_TO_OTHER_INVENTORY);
        click(ActionGUI.ITEM_REGISTER_CONFIRM_SLOT, ClickType.LEFT, InventoryAction.PICKUP_ALL);

        assertEquals(1, gui.getActionBuilder().getActions().size());
        TakeMatchedItemAction action = (TakeMatchedItemAction) gui.getActionBuilder().getActions().get(0);
        assertEquals(Material.GOLD_INGOT, action.getItem().getType());
        assertEquals(5, action.getItem().getAmount());
    }

    @Test
    void closingTheRegisterGuiClearsTheEditFlag() {
        gui.getActionBuilder().sendMessage("first");
        gui.getActionBuilder().setCurrentEditIndex(0);
        gui.getActionBuilder().setEditing(true);

        player.closeInventory();

        assertFalse(gui.getActionBuilder().isEditing(), "abandoning the GUI must not leave an edit pending");
    }
}
