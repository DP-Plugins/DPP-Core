package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class IfHasMaterialAction implements Action {
    private final String material;
    private final int amount;

    public IfHasMaterialAction(String material, int amount) {
        this.material = material.toUpperCase();
        this.amount = amount;
    }

    @Override
    public void execute(ActionContext context) {
        boolean result = false;
        try {
            Material mat = Material.valueOf(material);
            int count = 0;
            for (ItemStack is : context.getPlayer().getInventory().getContents()) {
                if (is != null && is.getType() == mat) {
                    count += is.getAmount();
                }
            }
            result = context.shouldExecute() && count >= amount;
        } catch (IllegalArgumentException ignored) {
        }
        context.pushCondition(result);
    }

    @Override
    public boolean isFlowControl() {
        return true;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.IF_HAS_MATERIAL;
    }

    @Override
    public String serialize() {
        return "if_has_material " + material + " " + amount;
    }

    public static IfHasMaterialAction parse(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("if_has_material")) return null;
        try {
            return new IfHasMaterialAction(parts[1], Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
