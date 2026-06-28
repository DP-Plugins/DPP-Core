package com.darksoldier1404.dppc.builder.action.obj;

import com.darksoldier1404.dppc.DPPCore;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class ActionContext {
    private final Player player;
    private final Map<String, String> variables = new HashMap<>();
    private final Deque<Boolean> conditionStack = new ArrayDeque<>();
    private boolean cancelled = false;
    private VariableStore store;

    public ActionContext(Player player) {
        this.player = player;
    }

    /** Lets tests inject an isolated, memory-only store. */
    public ActionContext(Player player, VariableStore store) {
        this.player = player;
        this.store = store;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }

    public boolean shouldExecute() {
        if (cancelled) return false;
        for (Boolean b : conditionStack) {
            if (!b) return false;
        }
        return true;
    }

    public void pushCondition(boolean condition) {
        conditionStack.push(condition);
    }

    public void flipTopCondition() {
        if (!conditionStack.isEmpty()) {
            conditionStack.push(!conditionStack.pop());
        }
    }

    public void popCondition() {
        if (!conditionStack.isEmpty()) {
            conditionStack.pop();
        }
    }

    public int getConditionDepth() {
        return conditionStack.size();
    }

    /**
     * Resolves the persisted variable store lazily so that contexts which only
     * touch temporary variables never need a running plugin.
     */
    private VariableStore store() {
        if (store == null) {
            store = DPPCore.variables != null ? DPPCore.variables : new VariableStore();
        }
        return store;
    }

    // --- Temporary variables (per-execution) ---

    public void setVariable(String name, String value) {
        variables.put(name, value);
    }

    public String getVariable(String name) {
        return variables.getOrDefault(name, "");
    }

    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    // --- Player variables (persisted per player) ---

    public void setPlayerVariable(String name, String value) {
        store().setPlayer(player.getUniqueId(), name, value);
    }

    public String getPlayerVariable(String name) {
        return store().getPlayer(player.getUniqueId(), name);
    }

    public boolean hasPlayerVariable(String name) {
        return store().hasPlayer(player.getUniqueId(), name);
    }

    // --- Global variables (persisted server-wide) ---

    public void setGlobalVariable(String name, String value) {
        store().setGlobal(name, value);
    }

    public String getGlobalVariable(String name) {
        return store().getGlobal(name);
    }

    public boolean hasGlobalVariable(String name) {
        return store().hasGlobal(name);
    }

    public String applyVariables(String text) {
        if (text == null) return null;
        String result = text
                .replace("{player}", player.getName())
                .replace("{player_world}", player.getWorld().getName())
                .replace("{player_x}", String.format("%.1f", player.getLocation().getX()))
                .replace("{player_y}", String.format("%.1f", player.getLocation().getY()))
                .replace("{player_z}", String.format("%.1f", player.getLocation().getZ()))
                .replace("{player_health}", String.format("%.1f", player.getHealth()))
                .replace("{player_level}", String.valueOf(player.getLevel()))
                .replace("{player_food}", String.valueOf(player.getFoodLevel()));
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        for (Map.Entry<String, String> entry : store().getPlayerMap(player.getUniqueId()).entrySet()) {
            result = result.replace("{pvar_" + entry.getKey() + "}", entry.getValue());
        }
        for (Map.Entry<String, String> entry : store().getGlobalMap().entrySet()) {
            result = result.replace("{gvar_" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
