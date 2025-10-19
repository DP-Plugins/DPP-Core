package com.darksoldier1404.dppc.events.dinventory.obj;

import com.darksoldier1404.dppc.api.inventory.DInventory;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

public abstract class DInventoryEvent extends InventoryEvent {
    private final DInventory inventory;

    public DInventoryEvent(@NotNull InventoryView transaction, DInventory inventory) {
        super(transaction);
        this.inventory = inventory;
    }

    public @NotNull DInventory getDInventory() {
        return inventory;
    }
}
