package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;

public class AddPlayerVariableAction implements Action {
    private final String name;
    private final double amount;

    public AddPlayerVariableAction(String name, double amount) {
        this.name = name;
        this.amount = amount;
    }

    @Override
    public void execute(ActionContext context) {
        try {
            double current = Double.parseDouble(context.getPlayerVariable(name));
            double result = current + amount;
            String formatted = result == (long) result
                    ? String.valueOf((long) result)
                    : String.valueOf(result);
            context.setPlayerVariable(name, formatted);
        } catch (NumberFormatException e) {
            context.setPlayerVariable(name, String.valueOf(amount));
        }
    }

    @Override
    public ActionType getActionType() {
        return ActionType.ADD_PLAYER_VARIABLE;
    }

    @Override
    public String serialize() {
        return "add_player_variable " + name + " " + amount;
    }

    public static AddPlayerVariableAction parse(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("add_player_variable")) return null;
        try {
            return new AddPlayerVariableAction(parts[1], Double.parseDouble(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
