package com.darksoldier1404.dppc;

import com.darksoldier1404.dppc.api.placeholder.PlaceholderBuilder;
import com.darksoldier1404.dppc.builder.action.ActionBuilder;
import com.darksoldier1404.dppc.builder.action.helper.ActionGUIHandler;
import com.darksoldier1404.dppc.commands.DPPCACommand;
import com.darksoldier1404.dppc.commands.DPPCCommand;
import com.darksoldier1404.dppc.commands.DPPCPCommand;
import com.darksoldier1404.dppc.utils.ConfigUtils;
import com.darksoldier1404.dppc.utils.PluginUtil;
import com.earth2me.essentials.Essentials;
import com.sk89q.worldguard.WorldGuard;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

@SuppressWarnings("all")
public class DPPCore extends JavaPlugin {
    private static DPPCore plugin;
    public YamlConfiguration config;
    public Logger log;
    public static Essentials ess;
    public static LuckPerms lp;
    public static WorldGuard wg;
    public static Set<PlaceholderBuilder.InternalExpansion> placeholders = new HashSet<PlaceholderBuilder.InternalExpansion>();
    public static Map<String, ActionBuilder> actions = new HashMap<>();

    public static DPPCore getInstance() {
        return plugin;
    }

    public static Map<String, ActionBuilder> getActions() {
        return actions;
    }

    @Override
    public void onLoad() {
        plugin = this;
        PluginUtil.addPlugin(plugin, 24432);
    }

    @Override
    public void onEnable() {
        plugin = this;
        log = getLogger();
        StringBuilder loadedPlugins = new StringBuilder();
        PluginUtil.getLoadedPlugins().keySet().stream()
                .skip(1) // Skip the first plugin
                .forEach(pl -> {
                    if (pl != null) {
                        loadedPlugins.append(String.format(
                                "   §aName §f: §b%s §7| §aVersion §f: §e%s%n",
                                pl.getName(),
                                pl.getDescription().getVersion()
                        ));
                    }
                });

        String result = loadedPlugins.toString();
        getServer().getConsoleSender().sendMessage("\n" +
                "─────────────────────────────────────────────────────────────────\n" +
                "  §b    ____  ____  ____        ______              \n" +
                "     / __ ⧵/ __ ⧵/ __ ⧵      / ____/___  ________ \n" +
                "    / / / / /_/ / /_/ /_____/ /   / __ ⧵/ ___/ _ ⧵\n" +
                "   / /_/ / ____/ ____/_____/ /___/ /_/ / /  /  __/\n" +
                "  /_____/_/   /_/          ⧵____/⧵____/_/   ⧵___/ \n\n" +
                "§7》》》》》》》 §aVERSION §f: §e" + getDescription().getVersion() + "\n\n" +
                "§7》》》》》》》 §cAPI-VERSION §f: §e" + getDescription().getAPIVersion() + "\n\n\n" +
                "§7》》》》》》》 §aPlugin installed with DPP-Core §7《《《《《《《\n\n" +
                loadedPlugins +
                "§f─────────────────────────────────────────────────────────────────\n");
        config = ConfigUtils.loadDefaultPluginConfig(plugin);
        PluginUtil.loadALLPlugins();
        PluginUtil.initializeSoftDependPlugins();
        PluginUtil.loadALLAction();
        PluginUtil.initPlaceholders();
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            PluginUtil.updateCheck();
        }, 100L);
        getCommand("dppc").setExecutor(new DPPCCommand());
        getCommand("dppca").setExecutor(new DPPCACommand());
        getCommand("dppcp").setExecutor(new DPPCPCommand());
        getServer().getPluginManager().registerEvents(new ActionGUIHandler(), this);
    }

    @Override
    public void onDisable() {
        log.info("plugin disabled.");
        ConfigUtils.savePluginConfig(plugin, config);
    }
}
