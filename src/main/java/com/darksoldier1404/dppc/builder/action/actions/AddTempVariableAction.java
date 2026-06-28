package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;

public class AddTempVariableAction implements Action {
    private final String name;
    private final double amount;

    public AddTempVariableAction(String name, double amount) {
        this.name = name;
        this.amount = amount;
    }

    @Override
    public void execute(ActionContext context) {
        try {
            double current = Double.parseDouble(context.getVariable(name));
            double result = current + amount;
            String formatted = result == (long) result
                    ? String.valueOf((long) result)
                    : String.valueOf(result);
            context.setVariable(name, formatted);
        } catch (NumberFormatException e) {
            context.setVariable(name, String.valueOf(amount));
        }
    }

    @Override
    public ActionType getActionType() {
        return ActionType.ADD_TEMP_VARIABLE;
    }

    @Override
    public String serialize() {
        return "add_temp_variable " + name + " " + amount;
    }

    public static AddTempVariableAction parse(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("add_temp_variable")) return null;
        try {
            return new AddTempVariableAction(parts[1], Double.parseDouble(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
