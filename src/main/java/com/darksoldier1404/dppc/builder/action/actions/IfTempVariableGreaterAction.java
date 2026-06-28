package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;

public class IfTempVariableGreaterAction implements Action {
    private final String name;
    private final double threshold;

    public IfTempVariableGreaterAction(String name, double threshold) {
        this.name = name;
        this.threshold = threshold;
    }

    @Override
    public void execute(ActionContext context) {
        boolean result = false;
        if (context.shouldExecute()) {
            try {
                double val = Double.parseDouble(context.getVariable(name));
                result = val > threshold;
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
        return ActionType.IF_TEMP_VARIABLE_GREATER;
    }

    @Override
    public String serialize() {
        return "if_temp_variable_greater " + name + " " + threshold;
    }

    public static IfTempVariableGreaterAction parse(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 3 || !parts[0].equalsIgnoreCase("if_temp_variable_greater")) return null;
        try {
            return new IfTempVariableGreaterAction(parts[1], Double.parseDouble(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
