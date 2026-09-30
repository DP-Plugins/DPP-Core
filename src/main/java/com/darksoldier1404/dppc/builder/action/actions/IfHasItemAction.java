package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import com.darksoldier1404.dppc.utils.ItemStackSerializer;
import org.bukkit.inventory.ItemStack;

public class IfHasItemAction implements Action {
    private final ItemStack item;

    public IfHasItemAction(ItemStack item) {
        this.item = item;
    }

    public ItemStack getItem() {
        return item;
    }

    @Override
    public void execute(ActionContext context) {
        boolean result = context.shouldExecute() && context.getPlayer().getInventory().containsAtLeast(item, item.getAmount());
        context.pushCondition(result);
    }

    @Override
    public boolean isFlowControl() {
        return true;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.IF_HAS_ITEM;
    }

    @Override
    public String serialize() {
        return "if_has_item " + ItemStackSerializer.serialize(item);
    }

    @Override
    public String getDisplayText() {
        return "if_has_item " + item.getType().name() + " x" + item.getAmount();
    }

    public static IfHasItemAction parse(String line) {
        String[] parts = line.split("\\s+", 2);
        if (parts.length != 2 || !parts[0].equalsIgnoreCase("if_has_item")) return null;
        try {
            ItemStack item = ItemStackSerializer.deserialize(parts[1]);
            if (item == null) return null;
            return new IfHasItemAction(item);
        } catch (Exception e) {
            return null;
        }
    }
}
