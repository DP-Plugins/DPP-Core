package com.darksoldier1404.dppc.plugin.commands;

import com.darksoldier1404.dppc.api.inventory.PageToolEditor;
import com.darksoldier1404.dppc.builder.command.ArgumentIndex;
import com.darksoldier1404.dppc.builder.command.ArgumentType;
import com.darksoldier1404.dppc.builder.command.CommandBuilder;

import java.util.Arrays;

import static com.darksoldier1404.dppc.DPPCore.plugin;

public class DPPCDICommand {
    public static void init() {
        CommandBuilder builder = new CommandBuilder(plugin);
        builder.beginSubCommand("setdefaultpagetoolitem", "/dppcdi setdefaultpagetoolitem <PANE/NEXT/PREV/CURRENT>")
                .withArgument(ArgumentIndex.ARG_0, ArgumentType.STRING, Arrays.asList("PANE", "NEXT", "PREV", "CURRENT"))
                .withPermission("dppc.admin")
                .executesPlayer(((sender, args) -> {
                    String type = args.getString(ArgumentIndex.ARG_0);
                    plugin.getConfig().set("Settings.DInventory.defaultPageToolItem." + type, sender.getInventory().getItemInMainHand());
                    plugin.saveConfig();
                    sender.sendMessage("§aDefault page tool item has been successfully set to " + type + "!");
                    return true;
                }));

        builder.beginSubCommand("edit", "/dppcdi edit")
                .withPermission("dppc.admin")
                .executesPlayer(((sender, args) -> {
                    new PageToolEditor().open(sender);
                    return true;
                }));

        builder.build("dppcdi");
    }
}
