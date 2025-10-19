package com.darksoldier1404.dppc.events.dinventory;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import com.darksoldier1404.dppc.events.dinventory.obj.DInventoryEvent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

public class DInventoryCloseEvent extends DInventoryEvent {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private boolean isCancelled;

    public DInventoryCloseEvent(@NotNull InventoryView transaction, DInventory inventory) {
        super(transaction, inventory);
    }

    public HumanEntity getPlayer() {
        return transaction.getPlayer();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
