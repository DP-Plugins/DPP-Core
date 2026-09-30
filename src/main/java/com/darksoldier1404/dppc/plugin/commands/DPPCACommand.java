package com.darksoldier1404.dppc.plugin.commands;

import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.builder.action.ActionBuilder;
import com.darksoldier1404.dppc.builder.action.helper.ActionGUI;
import com.darksoldier1404.dppc.builder.action.obj.Action;
import com.darksoldier1404.dppc.lang.DLang;
import com.darksoldier1404.dppc.utils.PluginUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DPPCACommand implements CommandExecutor, TabCompleter {
    private final DPPCore plugin = DPPCore.getInstance();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        DLang lang = plugin.getLang();
        // testsudo is the one subcommand every player may run; it is handled before the op gate.
        if (args.length > 0 && args[0].equalsIgnoreCase("testsudo")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(lang.get("g.cmd.player_only"));
                return false;
            }
            return runAction((Player) sender, lang, args, "ab.cmd.usage.testsudo");
        }
        if (!sender.isOp()) {
            sender.sendMessage(lang.get("g.cmd.permission.denied"));
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.get("g.cmd.player_only"));
            return false;
        }
        Player p = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(lang.get("ab.cmd.help.create"));
            sender.sendMessage(lang.get("ab.cmd.help.edit"));
            sender.sendMessage(lang.get("ab.cmd.help.delete"));
            sender.sendMessage(lang.get("ab.cmd.help.list"));
            sender.sendMessage(lang.get("ab.cmd.help.view"));
            sender.sendMessage(lang.get("ab.cmd.help.test"));
            sender.sendMessage(lang.get("ab.cmd.help.testsudo"));
            sender.sendMessage(lang.get("ab.cmd.help.reload"));
            return false;
        }
        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) {
                sender.sendMessage(lang.get("ab.cmd.usage.create"));
                return false;
            }
            String name = args[1];
            if (DPPCore.actions.containsKey(name)) {
                sender.sendMessage(lang.get("ab.cmd.already_exists"));
                return false;
            }
            ActionGUI gui = new ActionGUI(plugin, name);
            gui.openActionBuilderGUI(p);
            return false;
        }
        if (args[0].equalsIgnoreCase("edit")) {
            if (args.length < 2) {
                sender.sendMessage(lang.get("ab.cmd.usage.edit"));
                return false;
            }
            String name = args[1];
            if (!DPPCore.actions.containsKey(name)) {
                sender.sendMessage(lang.get("ab.cmd.not_found"));
                return false;
            }
            ActionGUI gui = new ActionGUI(DPPCore.actions.get(name));
            gui.openActionBuilderGUI(p);
            return false;
        }
        if (args[0].equalsIgnoreCase("delete")) {
            if (args.length < 2) {
                sender.sendMessage(lang.get("ab.cmd.usage.delete"));
                return false;
            }
            String name = args[1];
            if (!DPPCore.actions.containsKey(name)) {
                sender.sendMessage(lang.get("ab.cmd.not_found"));
                return false;
            }
            try {
                DPPCore.actions.remove(name);
                sender.sendMessage(lang.get("ab.cmd.deleted"));
                Files.deleteIfExists(Path.of(plugin.getDataFolder() + "/actions/" + name + ".yml"));
            } catch (IOException e) {
                sender.sendMessage(lang.getWithArgs("ab.cmd.delete_failed", name, String.valueOf(e.getMessage())));
                return false;
            }
            return false;
        }
        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage(lang.get("ab.cmd.list_header"));
            for (String action : DPPCore.actions.keySet()) {
                sender.sendMessage(lang.getWithArgs("ab.cmd.list_entry", action));
            }
            return false;
        }
        if (args[0].equalsIgnoreCase("view")) {
            if (args.length < 2) {
                sender.sendMessage(lang.get("ab.cmd.usage.view"));
                return false;
            }
            String name = args[1];
            if (!DPPCore.actions.containsKey(name)) {
                sender.sendMessage(lang.get("ab.cmd.not_found"));
                return false;
            }
            ActionBuilder ab = DPPCore.actions.get(name);
            for(Action a : ab.getActions()) {
                sender.sendMessage(a.serialize());
            }
            return false;
        }
        if (args[0].equalsIgnoreCase("test")) {
            return runAction(p, lang, args, "ab.cmd.usage.test");
        }
        if (args[0].equalsIgnoreCase("reload")) {
            PluginUtil.loadAllAction();
            sender.sendMessage(lang.get("ab.cmd.reloaded"));
            return false;
        }
        return false;
    }

    /** Runs a saved action for the player. Shared by {@code test} (op) and {@code testsudo} (everyone). */
    private boolean runAction(Player p, DLang lang, String[] args, String usageKey) {
        if (args.length < 2) {
            p.sendMessage(lang.get(usageKey));
            return false;
        }
        String name = args[1];
        if (!DPPCore.actions.containsKey(name)) {
            p.sendMessage(lang.get("ab.cmd.not_found"));
            return false;
        }
        DPPCore.actions.get(name).execute(p);
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            if (!sender.isOp()) {
                return Collections.singletonList("testsudo");
            }
            return Arrays.asList("create", "edit", "delete", "list", "view", "test", "testsudo", "reload");
        }
        if (args.length == 2) {
            return new ArrayList<>(DPPCore.actions.keySet());
        }
        return null;
    }
}