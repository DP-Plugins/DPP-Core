package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.utils.ItemStackSerializer;
import org.bukkit.inventory.ItemStack;

public class TakeMatchedItemAction implements Action {
    private final ItemStack item;

    public TakeMatchedItemAction(ItemStack item) {
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    @Override
    public void execute(ActionContext context) {
        context.getPlayer().getInventory().removeItem(item);
    }

    @Override
    public ActionType getActionType() {
        return ActionType.TAKE_MATCHED_ITEM;
    }

    @Override
    public String serialize() {
        return "take_matched_item " + ItemStackSerializer.serialize(item);
    }

    @Override
    public String getDisplayText() {
        return "take_matched_item " + item.getType().name() + " x" + item.getAmount();
    }

    public static TakeMatchedItemAction parse(String line) {
        String[] parts = line.split("\\s+", 2);
        if (parts.length != 2 || !parts[0].equalsIgnoreCase("take_matched_item")) return null;
        try {
            ItemStack item = ItemStackSerializer.deserialize(parts[1]);
            if (item == null) return null;
            return new TakeMatchedItemAction(item);
        } catch (Exception e) {
            return null;
        }
    }
}
