package com.darksoldier1404.dppc.api.logger;

import com.darksoldier1404.dppc.data.DPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;

public class DLogNode {
    private final DPlugin plugin;
    private final ArrayList<DLogContext> logs;

    public DLogNode(DPlugin plugin) {
        this.plugin = plugin;
        this.logs = new ArrayList<>();
    }

    public DPlugin getPlugin() {
        return plugin;
    }

    public ArrayList<DLogContext> getLogs() {
        return logs;
    }

    public void info(String message, boolean printToConsole) {
        DLogContext dlc = DLogContext.of(message, Level.INFO);
        logs.add(dlc);
        if (printToConsole) {
            plugin.getLogger().log(Level.INFO, dlc.getFormatedFullContext());
        }
    }

    public void warning(String message, boolean printToConsole) {
        logs.add(DLogContext.of(message, Level.WARNING));
        if (printToConsole) {
            plugin.getLogger().log(Level.WARNING, message);
        }
    }

    public void severe(String message, boolean printToConsole) {
        logs.add(DLogContext.of(message, Level.SEVERE));
        if (printToConsole) {
            plugin.getLogger().log(Level.SEVERE, message);
        }
    }

    public void clear() {
        logs.clear();
    }

    public YamlConfiguration serialize() {
        YamlConfiguration data = new YamlConfiguration();
        logs.sort(Comparator.comparingLong(o -> o.getTimestamp().getTime()));
        logs.forEach(l -> data.set(l.getFormatedTimestamp(), l.getFormatedContext()));
        return data;
    }
}
