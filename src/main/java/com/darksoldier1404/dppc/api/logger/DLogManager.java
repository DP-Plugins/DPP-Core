package com.darksoldier1404.dppc.api.logger;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.darksoldier1404.dppc.DPPCore.plugin;

public class DLogManager {
    private static final HashMap<DPlugin, DLogNode> logNodes = new HashMap<>();
    private static BukkitTask logTask;
    public static boolean printConfigUtilsLogs = true;
    public static boolean printCommandLogs = false;
    public static boolean printDInventoryLogs = false;
    public static boolean printDataContainerLogs = true;
    public static boolean printPluginUtilsLogs = true;

    public static DLogNode init(DPlugin plugin) {
        DLogNode logNode = new DLogNode(plugin);
        logNodes.put(plugin, logNode);
        return logNode;
    }

    public static void initTask() {
        if (logTask != null) {
            logTask.cancel();
            logTask = null;
        }
        long time = DPPCore.getInstance().getConfig().getInt("Settings.Log.save_period") * 20L;
        logTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveIntegratedLog();
            if (DPPCore.getInstance().getConfig().getBoolean("Settings.Log.save_separated")) {
                for (DPlugin plugin : logNodes.keySet()) {
                    saveLogNode(plugin, true);
                }
            }
        }, time, time);
    }

    public static void saveIntegratedLog() {
        if (DPPCore.getInstance().getConfig().getBoolean("Settings.Log.save_integrated")) {
            List<DLogContext> logContexts = new ArrayList<>();
            for (DLogNode logNode : logNodes.values()) {
                logContexts.addAll(logNode.getLogs());
            }
            YamlConfiguration data = new YamlConfiguration();
            logContexts.sort(Comparator.comparingLong(DLogContext::getNanoTime));
            logContexts.forEach(l -> data.set(l.getFormatedTimestamp(), l.getFormatedContext()));
            ConfigUtils.saveCustomData(plugin, data, new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()), "logs_integrated");
        }
    }

    public static DLogNode getLogNode(DPlugin plugin) {
        return logNodes.get(plugin);
    }

    public static void saveLogNode(DPlugin plugin, boolean clear) {
        DLogNode logNode = logNodes.get(plugin);
        if (logNode != null) {
            ConfigUtils.saveCustomData(logNode.getPlugin(), logNode.serialize(), new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date()), "logs");
            if (clear) {
                logNode.clear();
            }
        }
    }
}