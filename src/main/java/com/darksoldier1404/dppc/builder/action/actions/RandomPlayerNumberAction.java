package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;

import java.util.concurrent.ThreadLocalRandom;

public class RandomPlayerNumberAction implements Action {
    private final String name;
    private final int min;
    private final int max;

    public RandomPlayerNumberAction(String name, int min, int max) {
        this.name = name;
        this.min = min;
        this.max = max;
    }

    @Override
    public void execute(ActionContext context) {
        int value = ThreadLocalRandom.current().nextInt(min, max + 1);
        context.setPlayerVariable(name, String.valueOf(value));
    }

    @Override
    public ActionType getActionType() {
        return ActionType.RANDOM_PLAYER_NUMBER;
    }

    @Override
    public String serialize() {
        return "random_player_number " + name + " " + min + " " + max;
    }

    public static RandomPlayerNumberAction parse(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 4 || !parts[0].equalsIgnoreCase("random_player_number")) return null;
        try {
            return new RandomPlayerNumberAction(parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
