package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;

public class SetTempVariableAction implements Action {
    private final String name;
    private final String value;

    public SetTempVariableAction(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void execute(ActionContext context) {
        context.setVariable(name, context.applyVariables(value));
    }

    @Override
    public ActionType getActionType() {
        return ActionType.SET_TEMP_VARIABLE;
    }

    @Override
    public String serialize() {
        return "set_temp_variable " + name + " " + value;
    }

    public static SetTempVariableAction parse(String line) {
        String[] parts = line.split("\\s+", 3);
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("set_temp_variable")) return null;
        return new SetTempVariableAction(parts[1], parts[2]);
    }
}
