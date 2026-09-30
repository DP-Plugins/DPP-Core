package com.darksoldier1404.dppc.api.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedTeamParameters;
import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nametag color via raw {@code SCOREBOARD_TEAM} packets, bypassing Bukkit's
 * {@link org.bukkit.scoreboard.Scoreboard}/{@link org.bukkit.scoreboard.Team} entirely so
 * this never collides with another plugin's own scoreboard/team usage on the same player.
 * <p>
 * Scope is intentionally limited to color: prefix/suffix are left for later (see
 * {@link #teamNameFor(Player)} and {@link #ACTIVE_TEAMS}, which already carry what a future
 * prefix/suffix extension would need).
 * <p>
 * Field layout notes (learned the hard way against a live 1.21.4 server, not just the wiki):
 * "Method" is an {@code int}, and display name/prefix/suffix/visibility/collision/color/friendly
 * flags are bundled into a single {@code Optional<WrappedTeamParameters>} via
 * {@code getOptionalTeamParameters()} rather than flat fields — see {@code references/packets.md}
 * in the {@code protocollib} skill for the full writeup, including the ProtocolLib GitHub issue
 * confirming this packet can throw asynchronously at encode time rather than at the
 * {@code sendServerPacket} call site.
 * <p>
 * Live-verified: color changes correctly for both the per-viewer and broadcast variants. A
 * "no error, no visible change" symptom during testing turned out to be an unrelated plugin
 * also managing scoreboard/teams on the same server, not a bug here — see the
 * {@code protocollib-packet-debugging} memory before re-suspecting this class.
 */
@DPPCoreVersion(since = "5.5.0")
public class TeamAPI {
    private static final Map<UUID, String> ACTIVE_TEAMS = new ConcurrentHashMap<>();

    private static final int MODE_CREATE = 0;
    private static final int MODE_REMOVE = 1;
    private static final int MODE_UPDATE_INFO = 2;

    /** Changes {@code target}'s nametag color, but only as seen by {@code viewer}. */
    public static void setNameColor(Player viewer, Player target, ChatColor color) {
        ProtocolManager mgr = PacketAPI.manager();
        if (mgr == null) return;

        PacketContainer packet = buildColorPacket(mgr, target, color);
        mgr.sendServerPacket(viewer, packet);
    }

    /**
     * Changes {@code target}'s nametag color for every currently online player.
     * <p>
     * Note: a player who joins <i>after</i> this call won't have received the underlying
     * {@code CREATE} packet for this team, so they won't see the color until this (or
     * {@link #clearNameColor(Player)}) is called again while they're online. Re-sending this
     * for players who already have the team is safe (sends an {@code UPDATE_INFO}, not another
     * {@code CREATE}).
     */
    public static void setNameColor(Player target, ChatColor color) {
        ProtocolManager mgr = PacketAPI.manager();
        if (mgr == null) return;

        PacketContainer packet = buildColorPacket(mgr, target, color);
        mgr.broadcastServerPacket(packet, Bukkit.getOnlinePlayers());
    }

    /** Reverts {@code target} to their default nametag color, again only for {@code viewer}. */
    public static void clearNameColor(Player viewer, Player target) {
        ProtocolManager mgr = PacketAPI.manager();
        if (mgr == null) return;

        PacketContainer packet = buildClearPacket(mgr, target);
        if (packet == null) return;
        mgr.sendServerPacket(viewer, packet);
    }

    /** Reverts {@code target} to their default nametag color for every currently online player. */
    public static void clearNameColor(Player target) {
        ProtocolManager mgr = PacketAPI.manager();
        if (mgr == null) return;

        PacketContainer packet = buildClearPacket(mgr, target);
        if (packet == null) return;
        mgr.broadcastServerPacket(packet, Bukkit.getOnlinePlayers());
    }

    private static PacketContainer buildColorPacket(ProtocolManager mgr, Player target, ChatColor color) {
        String teamName = teamNameFor(target);
        boolean firstTime = ACTIVE_TEAMS.put(target.getUniqueId(), teamName) == null;

        WrappedTeamParameters params = WrappedTeamParameters.newBuilder()
                .displayName(WrappedChatComponent.fromText(""))
                .prefix(WrappedChatComponent.fromText(""))
                .suffix(WrappedChatComponent.fromText(""))
                .nametagVisibility("always")
                .collisionRule("always")
                .color(EnumWrappers.ChatFormatting.fromBukkit(color))
                .options(0)
                .build();

        PacketContainer packet = mgr.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(0, firstTime ? MODE_CREATE : MODE_UPDATE_INFO);
        packet.getOptionalTeamParameters().write(0, Optional.of(params));
        if (firstTime) {
            // Entities is only read by the client for method 0 (create) / 3 / 4 - omitted on update.
            packet.getSpecificModifier(Collection.class)
                    .write(0, Collections.singletonList(target.getName()));
        }
        return packet;
    }

    @Nullable
    private static PacketContainer buildClearPacket(ProtocolManager mgr, Player target) {
        String teamName = ACTIVE_TEAMS.remove(target.getUniqueId());
        if (teamName == null) return null;

        PacketContainer packet = mgr.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM, true);
        packet.getStrings().write(0, teamName);
        packet.getIntegers().write(0, MODE_REMOVE);
        return packet;
    }

    private static String teamNameFor(Player p) {
        return "dppc_" + p.getUniqueId().toString().replace("-", "").substring(0, 12);
    }
}
