package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;

public class IfGlobalVariableLessAction implements Action {
    private final String name;
    private final double threshold;

    public IfGlobalVariableLessAction(String name, double threshold) {
        this.name = name;
        this.threshold = threshold;
    }

    @Override
    public void execute(ActionContext context) {
        boolean result = false;
        if (context.shouldExecute()) {
            try {
                double val = Double.parseDouble(context.getGlobalVariable(name));
                result = val < threshold;
            } catch (NumberFormatException ignored) {
            }
        }
        context.pushCondition(result);
    }

    @Override
    public boolean isFlowControl() {
        return true;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.IF_GLOBAL_VARIABLE_LESS;
    }

    @Override
    public String serialize() {
        return "if_global_variable_less " + name + " " + threshold;
    }

    public static IfGlobalVariableLessAction parse(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("if_global_variable_less")) return null;
        try {
            return new IfGlobalVariableLessAction(parts[1], Double.parseDouble(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
