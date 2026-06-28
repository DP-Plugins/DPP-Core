package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;

public class IfGlobalVariableEqualsAction implements Action {
    private final String name;
    private final String value;

    public IfGlobalVariableEqualsAction(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void execute(ActionContext context) {
        boolean result = context.shouldExecute()
                && context.applyVariables(context.getGlobalVariable(name)).equals(context.applyVariables(value));
        context.pushCondition(result);
    }

    @Override
    public boolean isFlowControl() {
        return true;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.IF_GLOBAL_VARIABLE_EQUALS;
    }

    @Override
    public String serialize() {
        return "if_global_variable_equals " + name + " " + value;
    }

    public static IfGlobalVariableEqualsAction parse(String line) {
        String[] parts = line.split("\\s+", 3);
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("if_global_variable_equals")) return null;
        return new IfGlobalVariableEqualsAction(parts[1], parts[2]);
    }
}
