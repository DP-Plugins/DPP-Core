package com.darksoldier1404.dppc.api.packet;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.darksoldier1404.dppc.annotation.DPPCoreVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Map;

@DPPCoreVersion(since = "5.5.0")
public class FakeBlockAPI {

    /**
     * Sends a client-only block change to {@code viewer}. The real world is never touched,
     * so the server and this player's client immediately disagree about that block until
     * {@link #resetBlock(Player, Location)} (or a real block update) reconciles them.
     */
    public static void sendFakeBlock(Player viewer, Location loc, Material material) {
        ProtocolManager mgr = PacketAPI.manager();
        if (mgr == null) return;

        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0,
                new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        packet.getBlockData().write(0, WrappedBlockData.createData(material));

        mgr.sendServerPacket(viewer, packet);
    }

    /** Sends the real block state so the client matches the server again. */
    public static void resetBlock(Player viewer, Location loc) {
        sendFakeBlock(viewer, loc, loc.getBlock().getType());
    }

    /** Convenience for sending several fake blocks to the same viewer in one call. */
    public static void sendFakeBlocks(Player viewer, Map<Location, Material> blocks) {
        blocks.forEach((loc, mat) -> sendFakeBlock(viewer, loc, mat));
    }
}
