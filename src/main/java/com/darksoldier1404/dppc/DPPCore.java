package com.darksoldier1404.dppc;

import com.darksoldier1404.dppc.api.logger.DLogManager;
import com.darksoldier1404.dppc.api.placeholder.PlaceholderBuilder;
import com.darksoldier1404.dppc.builder.action.ActionBuilder;
import com.darksoldier1404.dppc.builder.action.helper.ActionGUIHandler;
import com.darksoldier1404.dppc.builder.action.obj.VariableStore;
import com.darksoldier1404.dppc.data.DPlugin;
import com.darksoldier1404.dppc.events.DAnvilInventoryListener;
import com.darksoldier1404.dppc.events.AdminUpdateNotifyListener;
import com.darksoldier1404.dppc.events.DPPCPPanelListener;
import com.darksoldier1404.dppc.events.InventoryEventListener;
import com.darksoldier1404.dppc.events.PageToolEditorListener;
import com.darksoldier1404.dppc.events.VariableStoreListener;
import com.darksoldier1404.dppc.plugin.commands.DPPCACommand;
import com.darksoldier1404.dppc.plugin.commands.DPPCCommand;
import com.darksoldier1404.dppc.plugin.commands.DPPCDICommand;
import com.darksoldier1404.dppc.plugin.commands.DPPCPCommand;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("all")
public class DPPCore extends DPlugin {
    public static DPPCore plugin;
    public static Plugin ess;
    public static Plugin lp;
    public static Set<PlaceholderBuilder.InternalExpansion> placeholders = new HashSet<>();
    public static Map<String, ActionBuilder> actions = new HashMap<>();
    public static VariableStore variables;

    public DPPCore() {
        super(true);
        plugin = this;
        init();
        DLogManager.printConfigUtilsLogs = config.getBoolean("Settings.Log.print_ConfigUtils_Debug");
        DLogManager.printCommandLogs = config.getBoolean("Settings.Log.print_Command_Debug");
        DLogManager.printDInventoryLogs = config.getBoolean("Settings.Log.print_DInventory_Debug");
        DLogManager.printDataContainerLogs = config.getBoolean("Settings.Log.print_DataContainer_Debug");
        DLogManager.printPluginUtilsLogs = config.getBoolean("Settings.Log.print_PluginUtils_Debug");
    }

    public static DPPCore getInstance() {
        return plugin;
    }

    public static Map<String, ActionBuilder> getActions() {
        return actions;
    }

    @Override
    public void onLoad() {
        PluginUtil.addPlugin(plugin, 24432);
    }

    @Override
    public void onEnable() {
        DLogManager.initTask();
        PluginUtil.loadAllPlugins();
        PluginUtil.initializeSoftDependPlugins();
        PluginUtil.loadAllAction();
        PluginUtil.initPlaceholders();
        variables = new VariableStore(this);
        variables.loadGlobal();
        for (org.bukkit.entity.Player pl : getServer().getOnlinePlayers()) {
            variables.loadPlayer(pl.getUniqueId());
        }
        getServer().getPluginManager().registerEvents(new ActionGUIHandler(), this);
        getServer().getPluginManager().registerEvents(new VariableStoreListener(), this);
        getServer().getPluginManager().registerEvents(new InventoryEventListener(), this);
        getServer().getPluginManager().registerEvents(new DAnvilInventoryListener(), this);
        getServer().getPluginManager().registerEvents(new PageToolEditorListener(), this);
        getServer().getPluginManager().registerEvents(new DPPCPPanelListener(), this);
        getServer().getPluginManager().registerEvents(new AdminUpdateNotifyListener(), this);
        getCommand("dppc").setExecutor(new DPPCCommand());
        getCommand("dppca").setExecutor(new DPPCACommand());
        getCommand("dppcp").setExecutor(new DPPCPCommand().getExecutor());
        DPPCDICommand.init();
        PluginUtil.showBanner();
    }

    @Override
    public void onDisable() {
        DLogManager.saveIntegratedLog();
        if (variables != null) {
            variables.saveAll();
        }
        saveAllData();
    }
}
