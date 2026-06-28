package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;

public class AddGlobalVariableAction implements Action {
    private final String name;
    private final double amount;

    public AddGlobalVariableAction(String name, double amount) {
        this.name = name;
        this.amount = amount;
    }

    @Override
    public void execute(ActionContext context) {
        try {
            double current = Double.parseDouble(context.getGlobalVariable(name));
            double result = current + amount;
            String formatted = result == (long) result
                    ? String.valueOf((long) result)
                    : String.valueOf(result);
            context.setGlobalVariable(name, formatted);
        } catch (NumberFormatException e) {
            context.setGlobalVariable(name, String.valueOf(amount));
        }
    }

    @Override
    public ActionType getActionType() {
        return ActionType.ADD_GLOBAL_VARIABLE;
    }

    @Override
    public String serialize() {
        return "add_global_variable " + name + " " + amount;
    }

    public static AddGlobalVariableAction parse(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("add_global_variable")) return null;
        try {
            return new AddGlobalVariableAction(parts[1], Double.parseDouble(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
