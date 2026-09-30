package com.darksoldier1404.dppc.plugin.commands;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.darksoldier1404.dppc.DPPCore;
import com.darksoldier1404.dppc.api.packet.FakeBlockAPI;
import com.darksoldier1404.dppc.api.packet.PacketAPI;
import com.darksoldier1404.dppc.api.packet.PacketSoundAPI;
import com.darksoldier1404.dppc.api.packet.TeamAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Developer command for manually verifying every {@code api.packet} class against a live
 * server. MockBukkit can't simulate real packet delivery, so this is the only way to confirm
 * the field indices in {@link TeamAPI} actually hold on a given Minecraft version.
 * <p>
 * Usage: {@code /dppcpacket <fakeblock|team|sound> [args...]}
 */
public class DPPCPacketTestCommand implements CommandExecutor, TabCompleter {
    private final DPPCore plugin = DPPCore.getInstance();
    private static final List<String> FUNCTIONS = Arrays.asList("fakeblock", "team", "sound");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cYou must be op to use this command.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by a player.");
            return true;
        }
        Player p = (Player) sender;
        if (!PacketAPI.isEnabled()) {
            sender.sendMessage("§cProtocolLib is not installed. This command requires it.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "fakeblock":
                return handleFakeBlock(sender, p, args);
            case "team":
                return handleTeam(sender, p, args);
            case "sound":
                return handleSound(sender, p, args);
            default:
                sender.sendMessage("§cUnknown function: §f" + args[0]);
                sender.sendMessage("§7Type §f/" + label + "§7 to see the list.");
                return true;
        }
    }

    private boolean handleFakeBlock(CommandSender sender, Player p, String[] args) {
        Material mat = args.length > 1 ? Material.matchMaterial(args[1]) : Material.GOLD_BLOCK;
        if (mat == null) {
            sender.sendMessage("§cUnknown material: §f" + args[1]);
            return true;
        }
        Block target = p.getTargetBlockExact(10);
        Location loc = target != null ? target.getLocation() : p.getLocation();
        FakeBlockAPI.sendFakeBlock(p, loc, mat);
        sender.sendMessage("§aSent fake §e" + mat + "§a at §f" + fmt(loc) + "§a. Resetting in 5s.");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            FakeBlockAPI.resetBlock(p, loc);
            sender.sendMessage("§7Reset block at §f" + fmt(loc) + "§7 to its real state.");
        }, 100L);
        return true;
    }

    private boolean handleTeam(CommandSender sender, Player p, String[] args) {
        boolean broadcast = args.length > 2 && args[2].equalsIgnoreCase("broadcast");

        if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
            if (broadcast) {
                TeamAPI.clearNameColor(p);
                sender.sendMessage("§aCleared your name color for all online players.");
            } else {
                TeamAPI.clearNameColor(p, p);
                sender.sendMessage("§aCleared your name color (self-view only).");
            }
            return true;
        }
        ChatColor color;
        try {
            color = args.length > 1 ? ChatColor.valueOf(args[1].toUpperCase()) : ChatColor.RED;
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cUnknown color: §f" + args[1]);
            return true;
        }
        if (broadcast) {
            TeamAPI.setNameColor(p, color);
            sender.sendMessage("§aSet your name color to §e" + color + "§a for all online players. §7/dppcpacket team clear broadcast to revert.");
        } else {
            TeamAPI.setNameColor(p, p, color);
            sender.sendMessage("§aSet your name color to §e" + color + "§a (self-view only). §7/dppcpacket team clear to revert.");
        }
        return true;
    }

    private boolean handleSound(CommandSender sender, Player p, String[] args) {
        Sound sound;
        try {
            sound = args.length > 1 ? Sound.valueOf(args[1].toUpperCase()) : Sound.ENTITY_PLAYER_LEVELUP;
        } catch (IllegalArgumentException e) {
            sender.sendMessage("§cUnknown sound: §f" + args[1]);
            return true;
        }
        PacketSoundAPI.playFromEntity(p, p.getEntityId(), sound, EnumWrappers.SoundCategory.MASTER, 1.0f, 1.0f);
        sender.sendMessage("§aPlayed §e" + sound + "§a from your own entity ID.");
        return true;
    }

    private static String fmt(Location loc) {
        return loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§6=== DPP-Core Packet API Test ===");
        sender.sendMessage("§e/" + label + " fakeblock [MATERIAL] §7- shows a fake block where you're looking, resets after 5s");
        sender.sendMessage("§e/" + label + " team [COLOR|clear] [broadcast] §7- sets/clears your own nametag color (self-view only, or for everyone online with 'broadcast')");
        sender.sendMessage("§e/" + label + " sound [SOUND] §7- plays a sound tied to your own entity ID");
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            for (String f : FUNCTIONS) {
                if (f.startsWith(prefix)) out.add(f);
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("team")) {
            List<String> out = new ArrayList<>();
            for (ChatColor c : ChatColor.values()) {
                if (c.name().startsWith(args[1].toUpperCase())) out.add(c.name());
            }
            out.add("clear");
            return out;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("team") && "broadcast".startsWith(args[2].toLowerCase())) {
            return Collections.singletonList("broadcast");
        }
        return null;
    }
}
