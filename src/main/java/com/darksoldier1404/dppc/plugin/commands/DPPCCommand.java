package com.darksoldier1404.dppc.plugin.commands;

import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DPPCCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return false;
        }
        if (args.length == 0) {
            sender.sendMessage("/dppca - DPP ActionBuilder Command Help");
            sender.sendMessage("/dppcp - installed DP-Plugins list GUI");
            sender.sendMessage("/dppc updatecheck (PluginName) - Check for updates");
            return false;
        }
        if (args[0].equalsIgnoreCase("updatecheck")) {
            if (args.length == 1) {
                PluginUtil.updateCheck(sender);
                return false;
            }
            if (args.length == 2) {
                PluginUtil.updateCheck(sender, args[1]);
                return false;
            }
            return false;
        }
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Collections.singletonList("updatecheck");
        }
        if (args.length == 2) {
            return PluginUtil.getLoadedPlugins().keySet().stream().map(JavaPlugin::getName).collect(Collectors.toList());
        }
        return null;
    }
}
