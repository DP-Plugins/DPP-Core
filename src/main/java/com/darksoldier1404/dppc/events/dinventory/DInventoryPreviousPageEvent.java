package com.darksoldier1404.dppc.events.dinventory;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

public class DInventoryPreviousPageEvent extends InventoryClickEvent {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private final DInventory inventory;

    public DInventoryPreviousPageEvent(@NotNull InventoryView transaction, DInventory inventory, @NotNull InventoryType.SlotType type, int slot, @NotNull ClickType click, @NotNull InventoryAction action) {
        super(transaction, type, slot, click, action);
        this.inventory = inventory;
    }

    public DInventoryPreviousPageEvent(@NotNull InventoryView transaction, DInventory inventory, @NotNull InventoryType.SlotType type, int slot, @NotNull ClickType click, @NotNull InventoryAction action, int key) {
        super(transaction, type, slot, click, action, key);
        this.inventory = inventory;
    }

    public @NotNull DInventory getDInventory() {
        return inventory;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
