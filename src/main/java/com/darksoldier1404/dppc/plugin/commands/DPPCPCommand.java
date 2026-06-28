package com.darksoldier1404.dppc.plugin.commands;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.builder.command.ArgumentIndex;
import com.darksoldier1404.dppc.builder.command.ArgumentType;
import com.darksoldier1404.dppc.builder.command.CommandBuilder;
import com.darksoldier1404.dppc.plugin.functions.DPPCPFunction;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Collectors;

public class DPPCPCommand {
    private final DPPCore plugin = DPPCore.getInstance();
    private final CommandBuilder builder = new CommandBuilder(plugin);

    public DPPCPCommand() {
        // Bare "/dppcp" opens the GUI information panel.
        builder.setDefaultAction((sender, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getPrefix() + "§cThis command can only be used by players.");
                return;
            }
            if (!sender.hasPermission("dppc.admin")) {
                sender.sendMessage(plugin.getPrefix() + "§cYou do not have permission to use this command.");
                return;
            }
            DPPCPFunction.openDPPListGUI((Player) sender);
        });

        builder.beginSubCommand("info", "/dppcp info <PluginName>")
                .withPermission("dppc.admin")
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING)
                .executes((p, args) -> {
                    String pluginName = args.getString(ArgumentIndex.ARG_0);
                    if (pluginName.isEmpty()) {
                        p.sendMessage(plugin.getPrefix() + "§cPlease specify a plugin name.");
                        return false; // Returning false will show the usage message
                    }
                    p.sendMessage(plugin.getPrefix() + "§aPlugin Information for: " + pluginName);
                    JavaPlugin javaPlugin = PluginUtil.getPluginByName(pluginName);
                    if (javaPlugin == null) {
                        p.sendMessage(plugin.getPrefix() + "§cPlugin not found: " + pluginName);
                        return true;
                    }
                    String version = javaPlugin.getDescription().getVersion();
                    String mainClass = javaPlugin.getDescription().getMain();
                    String apiVersion = javaPlugin.getDescription().getAPIVersion();
                    String dependencies = String.join(", ", javaPlugin.getDescription().getDepend());
                    String softDependencies = String.join(", ", javaPlugin.getDescription().getSoftDepend());
                    String commands = String.join(", ", javaPlugin.getDescription().getCommands().keySet());

                    p.sendMessage(plugin.getPrefix() + "§eVersion§f: §b" + version);
                    p.sendMessage(plugin.getPrefix() + "§eMain Class§f: §b" + mainClass);
                    p.sendMessage(plugin.getPrefix() + "§eAPI Version§f: §b" + (apiVersion != null ? apiVersion : "N/A"));
                    p.sendMessage(plugin.getPrefix() + "§eDepend§f: §b" + (dependencies.isEmpty() ? "None" : dependencies));
                    p.sendMessage(plugin.getPrefix() + "§eSoft Depend§f: §b" + (softDependencies.isEmpty() ? "None" : softDependencies));
                    p.sendMessage(plugin.getPrefix() + "§eCommands§f: §b" + (commands.isEmpty() ? "None" : commands));
                    return true;
                });

        builder.addTabCompletion("info", (sender, args) -> {
            if (args.length == 2) {
                return PluginUtil.getLoadedPlugins().keySet().stream()
                        .map(JavaPlugin::getName)
                        .collect(Collectors.toList());
            }
            return null;
        });
    }

    public CommandBuilder getExecutor() {
        return builder;
    }
}