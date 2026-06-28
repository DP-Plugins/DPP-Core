package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;

public class IfPlayerVariableNotEqualsAction implements Action {
    private final String name;
    private final String value;

    public IfPlayerVariableNotEqualsAction(String name, String value) {
        this.name = name;
        this.value = value;
    }

    @Override
    public void execute(ActionContext context) {
        boolean result = context.shouldExecute()
                && !context.applyVariables(context.getPlayerVariable(name)).equals(context.applyVariables(value));
        context.pushCondition(result);
    }

    @Override
    public boolean isFlowControl() {
        return true;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.IF_PLAYER_VARIABLE_NOT_EQUALS;
    }

    @Override
    public String serialize() {
        return "if_player_variable_not_equals " + name + " " + value;
    }

    public static IfPlayerVariableNotEqualsAction parse(String line) {
        String[] parts = line.split("\\s+", 3);
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("if_player_variable_not_equals")) return null;
        return new IfPlayerVariableNotEqualsAction(parts[1], parts[2]);
    }
}
