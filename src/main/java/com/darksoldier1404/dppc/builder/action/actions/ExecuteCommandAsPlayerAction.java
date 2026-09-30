package com.darksoldier1404.dppc.builder.action.actions;

import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.builder.action.obj.ActionContext;
import com.darksoldier1404.dppc.builder.action.obj.ActionType;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class ExecuteCommandAsPlayerAction implements Action {
    private final String command;

    public ExecuteCommandAsPlayerAction(String command) {
        this.command = command;
    }

    @Override
    public void execute(ActionContext context) {
        String precmd = context.applyVariables(command);
        PlayerCommandPreprocessEvent event = new PlayerCommandPreprocessEvent(context.getPlayer(), "/" + precmd);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            context.getPlayer().performCommand(precmd);
        }
    }

    @Override
    public ActionType getActionType() {
        return ActionType.EXECUTE_AS_PLAYER;
    }

    @Override
    public String serialize() {
        return "execute_as_player " + command;
    }

    public static ExecuteCommandAsPlayerAction parse(String line) {
        String[] parts = line.split("\\s+", 2);
        if (parts.length < 2 || !parts[0].equalsIgnoreCase("execute_as_player")) return null;
        return new ExecuteCommandAsPlayerAction(parts[1]);
    }
}
